package luma.hunt.network;

import luma.hunt.LumaHunt;
import luma.hunt.client.hud.TimerHud;
import luma.hunt.client.screen.LobbyScreen;
import luma.hunt.config.GameSettings;
import luma.hunt.lobby.LobbyManager;
import luma.hunt.logic.Role;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class NetworkHandler {
    public static final Identifier LOBBY_STATE_PACKET = LumaHunt.id("lobby_state");
    public static final Identifier CHANGE_ROLE_PACKET = LumaHunt.id("change_role");
    public static final Identifier OPEN_LOBBY_PACKET = LumaHunt.id("open_lobby");
    public static final Identifier GAME_START_PACKET = LumaHunt.id("game_start");
    public static final Identifier TIMER_STATE_PACKET = LumaHunt.id("timer_state");
    public static final Identifier GAME_END_PACKET = LumaHunt.id("game_end");

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(CHANGE_ROLE_PACKET, (server, player, handler, buf, responseSender) -> {
            UUID targetUuid = buf.readUuid();
            Role newRole = buf.readEnumConstant(Role.class);
            server.execute(() -> {
                LobbyManager.getInstance().setRole(targetUuid, newRole);
                LobbyManager.getInstance().broadcastLobbyState(server);
            });
        });
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(LOBBY_STATE_PACKET, (client, handler, buf, responseSender) -> {
            int count = buf.readInt();
            java.util.Map<UUID, Role> roles = new java.util.HashMap<>();
            for (int i = 0; i < count; i++) {
                roles.put(buf.readUuid(), buf.readEnumConstant(Role.class));
            }
            client.execute(() -> {
                LobbyManager.getInstance().updateClientRoles(roles);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(OPEN_LOBBY_PACKET, (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                if (!(client.currentScreen instanceof LobbyScreen)) {
                    client.setScreen(new LobbyScreen(null, false));
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(GAME_START_PACKET, (client, handler, buf, responseSender) -> {
            int headStartSeconds = buf.readInt();
            boolean dreamStart = buf.readBoolean();
            
            client.execute(() -> {
                int startModeIndex = 0;
                if (dreamStart) {
                    startModeIndex = 0;
                } else if (headStartSeconds > 0) {
                    int[] values = GameSettings.getHeadstartValues();
                    for (int i = 1; i < values.length; i++) {
                        if (values[i] >= headStartSeconds) {
                            startModeIndex = i;
                            break;
                        }
                        startModeIndex = i;
                    }
                }
                GameSettings.getInstance().setStartModeIndex(startModeIndex);
                TimerHud.initializeGameStart();
                client.setScreen(null);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(TIMER_STATE_PACKET, (client, handler, buf, responseSender) -> {
            int state = buf.readInt();
            long headstartEndTime = buf.readLong();
            long gameStartTime = buf.readLong();
            
            client.execute(() -> {
                TimerHud.setStateFromPacket(state, headstartEndTime, gameStartTime);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(GAME_END_PACKET, (client, handler, buf, responseSender) -> {
            boolean isWinner = buf.readBoolean();
            String message = buf.readString();
            
            client.execute(() -> {
                TimerHud.stopTimer();
                GameSettings.getInstance().setGameActive(false);

                String title = isWinner ? "§a§lYOU WON!" : "§c§lYOU LOST";
                String subtitle = "§7" + message;
                
                if (client.player != null) {
                    client.inGameHud.setTitle(Text.of(title));
                    client.inGameHud.setSubtitle(Text.of(subtitle));
                    client.inGameHud.setTitleTicks(10, 70, 20);

                    if (isWinner) {
                        client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    } else {
                        client.player.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 0.5f, 1.0f);
                    }
                }
            });
        });
    }
    
    public static void sendChangeRole(UUID targetUuid, Role newRole) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(targetUuid);
        buf.writeEnumConstant(newRole);
        ClientPlayNetworking.send(CHANGE_ROLE_PACKET, buf);
    }

    public static void sendOpenLobby(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, OPEN_LOBBY_PACKET, buf);
    }

    public static void sendGameStart(ServerPlayerEntity player, int headStartSeconds, boolean dreamStart) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(headStartSeconds);
        buf.writeBoolean(dreamStart);
        ServerPlayNetworking.send(player, GAME_START_PACKET, buf);
    }

    public static void sendTimerState(ServerPlayerEntity player, int state, long headstartEndTime, long gameStartTime) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(state);
        buf.writeLong(headstartEndTime);
        buf.writeLong(gameStartTime);
        ServerPlayNetworking.send(player, TIMER_STATE_PACKET, buf);
    }

    public static void sendGameEnd(ServerPlayerEntity player, boolean isWinner, String message) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(isWinner);
        buf.writeString(message);
        ServerPlayNetworking.send(player, GAME_END_PACKET, buf);
    }
}

