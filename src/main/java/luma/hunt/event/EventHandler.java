package luma.hunt.event;

import luma.hunt.lobby.LobbyManager;
import luma.hunt.logic.GameManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class EventHandler {
    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (LobbyManager.getInstance().getCurrentLobbyCode() != null) {
                ServerPlayerEntity joiner = handler.getPlayer();
                if (!server.getPlayerManager().getPlayerList().isEmpty()) {
                    ServerPlayerEntity host = server.getPlayerManager().getPlayerList().get(0);
                    if (host != null && !joiner.equals(host)) {
                        joiner.teleport(host.getServerWorld(), 
                            host.getX(), host.getY(), host.getZ(),
                            host.getYaw(), host.getPitch());
                    }
                }
                
                LobbyManager.getInstance().onPlayerJoin(joiner);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            GameManager.getInstance().tick(server);
        });
    }
}

