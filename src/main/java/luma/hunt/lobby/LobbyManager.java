package luma.hunt.lobby;

import luma.hunt.LumaHunt;
import luma.hunt.network.SupabaseClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import luma.hunt.logic.Role;
import luma.hunt.network.NetworkHandler;
import net.fabricmc.loader.api.FabricLoader;
import luma.hunt.tunnel.TunnelManager;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class LobbyManager {
    private static LobbyManager instance;
    private String currentLobbyCode;
    private boolean isHost;
    private String currentMode = "1v1";
    private final Map<UUID, Role> playerRoles = new HashMap<>();
    
    private LobbyManager() {}

    public static void init() {
        instance = new LobbyManager();
    }

    public void checkChatForE4mc(String text) {
        if (isHost && currentLobbyCode != null) {
            if (text.contains("Local game hosted on domain") || text.contains(".e4mc.link")) {
                 handleE4mcDomain(text);
            }
        }
    }

    public static LobbyManager getInstance() {
        if (instance == null) {
            instance = new LobbyManager();
        }
        return instance;
    }

    public boolean createLobby(String mode) {
        if (this.currentLobbyCode != null) {
            deleteLobby();
        }

        IntegratedServer server = MinecraftClient.getInstance().getServer();
        if (server == null) {
            LumaHunt.LOGGER.error("Cannot create lobby: IntegratedServer is null");
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getServer() == null || !client.getServer().isSingleplayer()) {
                client.inGameHud.setTitle(net.minecraft.text.Text.of("§d§lʟᴜᴍᴀʜᴜɴᴛ"));
                client.inGameHud.setSubtitle(net.minecraft.text.Text.translatable("lumahunt.lobby.error.singleplayer"));
                return false;
            }
        }

        if (!server.isRemote()) {
             if (server.getServerPort() == -1) {
                 int port = findFreePort();
                 if (!server.openToLan(null, true, port)) {
                     LumaHunt.LOGGER.error("Failed to open LAN");
                     MinecraftClient.getInstance().getToastManager().add(new net.minecraft.client.toast.SystemToast(
                         net.minecraft.client.toast.SystemToast.Type.PERIODIC_NOTIFICATION,
                         net.minecraft.text.Text.of("ʟᴜᴍᴀʜᴜɴᴛ"),
                         net.minecraft.text.Text.of("Err | Failed to open LAN!")
                     ));
                     return false;
                 }
             }
        }

        String code = generateCode();
        this.currentLobbyCode = code;
        this.isHost = true;
        this.currentMode = mode;
        
        if (luma.hunt.LumaHunt.DISABLE_DAYLIGHT_ON_CREATE) {
            luma.hunt.LumaHunt.DISABLE_DAYLIGHT_ON_CREATE = false;

            MinecraftClient.getInstance().execute(() -> {
                IntegratedServer srv = MinecraftClient.getInstance().getServer();
                if (srv != null) {
                    srv.getCommandManager().executeWithPrefix(
                        srv.getCommandSource().withSilent(),
                        "gamerule doDaylightCycle false"
                    );
                    srv.getCommandManager().executeWithPrefix(
                        srv.getCommandSource().withSilent(),
                        "gamerule doWeatherCycle false"
                    );
                }
            });
        }
        
        boolean e4mcLoaded = FabricLoader.getInstance().isModLoaded("e4mc_minecraft");
        LumaHunt.LOGGER.info("Lobby created key: " + code + " Mode: " + mode + ". Waiting for e4mc domain...");
        
        if (!e4mcLoaded) {
             LumaHunt.LOGGER.info("External e4mc not loaded - using built-in tunnel");
             checkBuiltinTunnelDomain();
        }
        return true;
    }

    public boolean createLobby() {
        return createLobby("1v1");
    }
    
    private void handleE4mcDomain(String chatMessage) {
        try {
            String domain = null;
            String[] words = chatMessage.split(" ");
            for (String word : words) {
                if (word.contains(".e4mc.link")) {
                    domain = word;
                    break;
                }
            }
            
            if (domain != null) {
                domain = domain.replaceAll("[\\[\\]]", "");
                domain = domain.replaceAll("§.", "");
                while (domain.length() > 0 && !Character.isLetterOrDigit(domain.charAt(domain.length() - 1))) {
                     domain = domain.substring(0, domain.length() - 1);
                }
                LumaHunt.LOGGER.info("Capturing e4mc domain: " + domain);
                String finalDomain = domain;

                SupabaseClient.getInstance().createLobby(currentLobbyCode, finalDomain, 25565).thenAccept(success -> {
                    if (success) {
                       LumaHunt.LOGGER.info("Lobby registered in Supabase with domain: " + finalDomain);
                       MinecraftClient client = MinecraftClient.getInstance();
                       client.execute(() -> {
                           client.inGameHud.setTitle(net.minecraft.text.Text.translatable("lumahunt.lobby.ready"));
                           client.inGameHud.setSubtitle(net.minecraft.text.Text.translatable("lumahunt.lobby.code.msg", currentLobbyCode));
                       });
                    }
                });
            }
        } catch (Exception e) {
            LumaHunt.LOGGER.error("Failed to parse e4mc domain", e);
        }
    }
    
    private void checkBuiltinTunnelDomain() {
        TunnelManager tunnel = TunnelManager.getInstance();
        String domain = tunnel.getCurrentDomain();
        if (domain != null) {
            registerTunnelDomain(domain);
            return;
        }

        Thread waitThread = new Thread(() -> {
            int maxWaitSeconds = 15;
            int waited = 0;
            while (waited < maxWaitSeconds) {
                try {
                    Thread.sleep(500);
                    waited++;
                    
                    String d = tunnel.getCurrentDomain();
                    if (d != null) {
                        MinecraftClient.getInstance().execute(() -> registerTunnelDomain(d));
                        return;
                    }
                    
                    if (tunnel.getState() == luma.hunt.tunnel.TunnelSession.State.UNHEALTHY) {
                        LumaHunt.LOGGER.error("Tunnel failed to start");
                        return;
                    }
                } catch (InterruptedException e) {
                    return;
                }
            }
            LumaHunt.LOGGER.warn("Timeout waiting for tunnel domain");
        }, "lumahunt-tunnel-wait");
        waitThread.setDaemon(true);
        waitThread.start();
    }
    
    private void registerTunnelDomain(String domain) {
        if (currentLobbyCode == null) return;
        
        LumaHunt.LOGGER.info("Registering built-in tunnel domain: " + domain);
        
        SupabaseClient.getInstance().createLobby(currentLobbyCode, domain, 25565).thenAccept(success -> {
            if (success) {
                LumaHunt.LOGGER.info("Lobby registered with tunnel domain: " + domain);
                MinecraftClient.getInstance().getToastManager().add(new net.minecraft.client.toast.SystemToast(
                    net.minecraft.client.toast.SystemToast.Type.PERIODIC_NOTIFICATION,
                    net.minecraft.text.Text.of("Lobby Ready"),
                    net.minecraft.text.Text.of("Code: " + currentLobbyCode)
                ));
            }
        });
    }
    
    public void deleteLobby() {
        if (this.currentLobbyCode != null && this.isHost) {
            SupabaseClient.getInstance().deleteLobby(this.currentLobbyCode);
            this.currentLobbyCode = null;
            this.isHost = false;
        }
    }

    public void joinLobby(String code) {
        SupabaseClient.getInstance().getLobby(code).thenAccept(json -> {
            if (json != null) {
                String ip = json.get("ip").getAsString();
                int port = json.get("port").getAsInt();
                LumaHunt.LOGGER.info("Found lobby: " + ip + ":" + port);
                
                MinecraftClient.getInstance().execute(() -> {
                    luma.hunt.client.screen.JoinGameScreen.connectTo(ip, port);
                });
            } else {
                LumaHunt.LOGGER.error("Lobby not found: " + code);
            }
        });
    }

    private String generateCode() {
        Random random = new Random();
        return String.format("%05d", random.nextInt(100000));
    }
    
    private CompletableFuture<String> fetchPublicIp() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                java.net.URL url = new java.net.URL("https://api.ipify.org");
                java.util.Scanner sc = new java.util.Scanner(url.openStream());
                return sc.hasNext() ? sc.next() : null;
            } catch (Exception e) {
                LumaHunt.LOGGER.error("Failed to fetch public IP", e);
                return "127.0.0.1";
            }
        });
    }

    public String getCurrentLobbyCode() {
        return currentLobbyCode;
    }
    
    public boolean isHost() {
        return isHost;
    }
    
    public void cycleRole(UUID playerUuid) {
        playerRoles.compute(playerUuid, (k, v) -> v == null ? Role.NOBODY : v.next());
    }

    public void setRole(UUID playerUuid, Role role) {
        playerRoles.put(playerUuid, role);
    }
    
    public Role getPlayerRole(UUID playerUuid) {
        return playerRoles.getOrDefault(playerUuid, Role.NOBODY);
    }
    
    private int findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            return 25565;
        }
    }
    
    public void updateClientRoles(Map<UUID, Role> roles) {
        this.playerRoles.clear();
        this.playerRoles.putAll(roles);
    }
    
    public void broadcastLobbyState(MinecraftServer server) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(playerRoles.size());
        playerRoles.forEach((uuid, role) -> {
            buf.writeUuid(uuid);
            buf.writeEnumConstant(role);
        });
        
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, NetworkHandler.LOBBY_STATE_PACKET, buf);
        }
    }

    public void startGame() {
        if (isHost) {
            int count = playerRoles.size();
            long runners = playerRoles.values().stream().filter(r -> r == Role.RUNNER).count();
            long hunters = playerRoles.values().stream().filter(r -> r == Role.HUNTER).count();
            
            boolean valid = false;
            String error = "";

            switch (currentMode) {
                case "1v1":
                    if (count == 2 && runners == 1 && hunters == 1) valid = true;
                    else error = "Need 1 Runner & 1 Hunter (Total 2)";
                    break;
                case "1v2":
                    if (count == 3 && runners == 1 && hunters == 2) valid = true;
                    else error = "Need 1 Runner & 2 Hunters (Total 3)";
                    break;
                case "1v3":
                    if (count == 4 && runners == 1 && hunters == 3) valid = true;
                    else error = "Need 1 Runner & 3 Hunters (Total 4)";
                    break;
                case "1vX":
                    if (runners == 1 && hunters >= 1) valid = true;
                    else error = "Need 1 Runner & at least 1 Hunter";
                    break;
                case "XvX":
                    if (runners >= 1 && hunters >= 1) valid = true;
                    else error = "Need at least 1 Runner & 1 Hunter";
                    break;
            }

            if (!valid) {
                MinecraftClient.getInstance().getToastManager().add(new net.minecraft.client.toast.SystemToast(
                    net.minecraft.client.toast.SystemToast.Type.PERIODIC_NOTIFICATION,
                    net.minecraft.text.Text.of("Cannot Start"),
                    net.minecraft.text.Text.of(error)
                ));
                return;
            }

            MinecraftServer server = MinecraftClient.getInstance().getServer();
            if (server != null) {
                String startMsg = String.format("§a§lGAME STARTED: §fManhunt");
                server.getPlayerManager().broadcast(net.minecraft.text.Text.of(startMsg), false);

                server.getCommandManager().executeWithPrefix(
                    server.getCommandSource().withSilent(),
                    "gamerule doDaylightCycle true"
                );
                server.getCommandManager().executeWithPrefix(
                    server.getCommandSource().withSilent(),
                    "weather clear"
                );

                int headStart = luma.hunt.config.GameSettings.getInstance().getRunnerHeadStartSeconds();
                boolean dreamStart = luma.hunt.config.GameSettings.getInstance().isDreamStartEnabled();

                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    NetworkHandler.sendGameStart(player, headStart, dreamStart);
                }
                luma.hunt.logic.GameManager.getInstance().startGame(server);
            }
        }
    }
    
    public void onPlayerJoin(ServerPlayerEntity player) {
        if (!playerRoles.containsKey(player.getUuid())) {
            playerRoles.put(player.getUuid(), Role.NOBODY);
        }

        NetworkHandler.sendOpenLobby(player);
        broadcastLobbyState(player.getServer());
    }
}
