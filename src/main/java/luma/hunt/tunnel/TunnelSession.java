package luma.hunt.tunnel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.incubator.codec.quic.*;
import luma.hunt.LumaHunt;
import net.minecraft.client.MinecraftClient;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Simplified e4mc tunnel session for LumaHunt.
 * Connects to e4mc relay servers to provide public domain for LAN games.
 * Stripped version without P2P/dialtone and kaleido dependencies.
 */
public class TunnelSession {
    private static final Gson gson = new Gson();
    private static final String BROKER_URL = "https://broker.e4mc.link/getBestRelay";
    
    public enum State {
        STARTING, STARTED, UNHEALTHY, STOPPING, STOPPED
    }
    
    public State state = State.STARTING;
    public Throwable failureCause = null;
    public String assignedDomain = null;
    
    private final ChannelHandler childHandler;
    private final EventLoopGroup group;
    private DatagramChannel datagramChannel;
    private QuicChannel quicChannel;
    
    private Consumer<String> onDomainAssigned;
    private Consumer<Throwable> onError;
    
    public TunnelSession(ChannelHandler childHandler, EventLoopGroup group) {
        this.childHandler = childHandler;
        this.group = group;
    }
    
    public void setOnDomainAssigned(Consumer<String> callback) {
        this.onDomainAssigned = callback;
    }
    
    public void setOnError(Consumer<Throwable> callback) {
        this.onError = callback;
    }
    
    public void startAsync() {
        Thread thread = new Thread(this::start, "lumahunt-tunnel-init");
        thread.setDaemon(true);
        thread.start();
    }
    
    // Broker response structure
    static class BrokerResponse {
        String id;
        String host;
        int port;
    }
    
    private BrokerResponse getRelay() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest
                .newBuilder(new URI(BROKER_URL))
                .header("Accept", "application/json")
                .build();
        
        LumaHunt.LOGGER.info("Requesting relay from broker: {}", BROKER_URL);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Broker returned status " + response.statusCode());
        }
        
        return gson.fromJson(response.body(), BrokerResponse.class);
    }
    
    public void start() {
        try {
            BrokerResponse relayInfo = getRelay();
            LumaHunt.LOGGER.info("Using relay: {} ({}:{})", relayInfo.id, relayInfo.host, relayInfo.port);
            
            QuicSslContext context = QuicSslContextBuilder
                    .forClient()
                    .applicationProtocols("quiclime")
                    .build();
            
            QuicClientCodecBuilder codec = new QuicClientCodecBuilder()
                    .sslContext(context)
                    .sslEngineProvider(ch -> context.newEngine(ch.alloc(), relayInfo.host, relayInfo.port))
                    .initialMaxStreamsBidirectional(512)
                    .maxIdleTimeout(10, TimeUnit.SECONDS)
                    .initialMaxData(4611686018427387903L)
                    .initialMaxStreamDataBidirectionalRemote(1250000)
                    .initialMaxStreamDataBidirectionalLocal(1250000)
                    .initialMaxStreamDataUnidirectional(1250000);
            
            Class<? extends DatagramChannel> channelClass = NioDatagramChannel.class;
            
            new Bootstrap()
                    .group(group)
                    .channel(channelClass)
                    .handler(codec.build())
                    .bind(0)
                    .addListener((ChannelFutureListener) datagramFuture -> {
                        if (!datagramFuture.isSuccess()) {
                            fail(datagramFuture.cause());
                            return;
                        }
                        
                        datagramChannel = (DatagramChannel) datagramFuture.channel();
                        
                        QuicChannel.newBootstrap(datagramChannel)
                                .streamHandler(childHandler)
                                .handler(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        fail(cause);
                                    }
                                    
                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) {
                                        state = State.STOPPED;
                                    }
                                })
                                .remoteAddress(new InetSocketAddress(InetAddress.getByName(relayInfo.host), relayInfo.port))
                                .connect()
                                .addListener(quicFuture -> {
                                    if (!quicFuture.isSuccess()) {
                                        fail(quicFuture.cause());
                                        return;
                                    }
                                    
                                    quicChannel = (QuicChannel) quicFuture.getNow();

                                    quicChannel.createStream(QuicStreamType.BIDIRECTIONAL,
                                            new ChannelInitializer<QuicStreamChannel>() {
                                                @Override
                                                protected void initChannel(QuicStreamChannel ch) {
                                                    ch.pipeline().addLast(
                                                            new ControlMessageCodec(),
                                                            new ControlChannelHandler()
                                                    );
                                                }
                                            })
                                            .addListener(streamFuture -> {
                                                if (!streamFuture.isSuccess()) {
                                                    fail(streamFuture.cause());
                                                    return;
                                                }
                                                
                                                QuicStreamChannel streamChannel = (QuicStreamChannel) streamFuture.getNow();
                                                LumaHunt.LOGGER.info("Control channel opened");
                                                streamChannel.writeAndFlush(new RequestDomainMessage());
                                                
                                                quicChannel.closeFuture().addListener(f -> {
                                                    if (datagramChannel != null) {
                                                        datagramChannel.close();
                                                    }
                                                });
                                            });
                                });
                    });
                    
        } catch (Throwable e) {
            fail(e);
        }
    }
    
    private void fail(Throwable e) {
        state = State.UNHEALTHY;
        failureCause = e;
        LumaHunt.LOGGER.error("Tunnel error", e);
        if (onError != null) {
            onError.accept(e);
        }
    }
    
    public void stop() {
        state = State.STOPPING;
        try {
            if (quicChannel != null && quicChannel.isOpen()) {
                quicChannel.close().addListener(f -> {
                    try {
                        if (datagramChannel != null && datagramChannel.isOpen()) {
                            datagramChannel.close().addListener(f2 -> state = State.STOPPED);
                        } else {
                            state = State.STOPPED;
                        }
                    } catch (Exception e) {
                        state = State.STOPPED;
                    }
                });
            } else if (datagramChannel != null && datagramChannel.isOpen()) {
                datagramChannel.close().addListener(f -> state = State.STOPPED);
            } else {
                state = State.STOPPED;
            }
        } catch (Exception e) {
            LumaHunt.LOGGER.debug("Tunnel stop ignored (event loop closed): {}", e.getMessage());
            state = State.STOPPED;
        }
    }

    static class RequestDomainMessage {
        String kind = "request_domain_assignment";
    }
    
    static class DomainAssignedMessage {
        String kind;
        String domain;
    }

    class ControlMessageCodec extends ByteToMessageCodec<Object> {
        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) {
            byte[] json = gson.toJson(msg).getBytes(StandardCharsets.UTF_8);
            writeVarInt(out, json.length);
            out.writeBytes(json);
        }
        
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            if (in.readableBytes() < 1) return;
            
            int size = in.getByte(in.readerIndex());
            if (in.readableBytes() >= size + 1) {
                in.skipBytes(1);
                byte[] buf = new byte[size];
                in.readBytes(buf);
                
                JsonObject json = gson.fromJson(new String(buf, StandardCharsets.UTF_8), JsonObject.class);
                String kind = json.get("kind").getAsString();
                
                if ("domain_assignment_complete".equals(kind)) {
                    DomainAssignedMessage msg = new DomainAssignedMessage();
                    msg.kind = kind;
                    msg.domain = json.get("domain").getAsString();
                    out.add(msg);
                }
            }
        }
        
        private void writeVarInt(ByteBuf buf, int value) {
            while ((value & 0xffffff80) != 0) {
                buf.writeByte(value & 0x7F | 0x80);
                value >>>= 7;
            }
            buf.writeByte(value);
        }
    }

    class ControlChannelHandler extends SimpleChannelInboundHandler<Object> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof DomainAssignedMessage) {
                DomainAssignedMessage dam = (DomainAssignedMessage) msg;
                state = State.STARTED;
                assignedDomain = dam.domain;
                LumaHunt.LOGGER.info("Domain assigned: {}", assignedDomain);
                
                if (onDomainAssigned != null) {
                    onDomainAssigned.accept(assignedDomain);
                }

                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(
                            net.minecraft.text.Text.of("§8[§dLumaHunt§8] §fTunnel active: §e" + assignedDomain),
                            false
                    );
                }
            }
        }
    }
}
