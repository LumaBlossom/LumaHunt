package luma.hunt.mixin;

import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import luma.hunt.LumaHunt;
import luma.hunt.tunnel.TunnelManager;
import net.minecraft.server.ServerNetworkIo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;

@Mixin(ServerNetworkIo.class)
public abstract class ServerNetworkIoMixin {
    
    @Unique
    private ChannelHandler lumahunt$childHandler;
    
    @Unique
    private EventLoopGroup lumahunt$group;
    
    @ModifyArg(
        method = "bind",
        at = @At(
            value = "INVOKE",
            target = "Lio/netty/bootstrap/ServerBootstrap;childHandler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/ServerBootstrap;",
            remap = false
        )
    )
    private ChannelHandler interceptHandler(ChannelHandler childHandler) {
        lumahunt$childHandler = childHandler;
        return childHandler;
    }
    
    @ModifyArg(
        method = "bind",
        at = @At(
            value = "INVOKE",
            target = "Lio/netty/bootstrap/ServerBootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/ServerBootstrap;",
            remap = false
        )
    )
    private EventLoopGroup interceptGroup(EventLoopGroup group) {
        lumahunt$group = group;
        return group;
    }
    
    @Inject(method = "bind", at = @At("TAIL"))
    private void startTunnel(InetAddress address, int port, CallbackInfo ci) {
        if (lumahunt$childHandler != null && lumahunt$group != null) {
            LumaHunt.LOGGER.info("Server bound, starting tunnel...");
            TunnelManager.getInstance().startTunnel(
                lumahunt$childHandler,
                lumahunt$group,
                domain -> {
                    LumaHunt.LOGGER.info("Tunnel domain assigned: {}", domain);
                }
            );
        }
        
        lumahunt$childHandler = null;
        lumahunt$group = null;
    }
    
    @Inject(method = "stop", at = @At("HEAD"))
    private void stopTunnel(CallbackInfo ci) {
        try {
            TunnelManager.getInstance().stopTunnel();
        } catch (Exception e) {
            LumaHunt.LOGGER.debug("Tunnel cleanup ignored during shutdown");
        }
    }
}
