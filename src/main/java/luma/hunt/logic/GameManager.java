package luma.hunt.logic;

import luma.hunt.LumaHunt;
import luma.hunt.config.GameSettings;
import luma.hunt.lobby.LobbyManager;
import luma.hunt.network.NetworkHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.UUID;

public class GameManager {
    private static GameManager instance;
    
    public enum GamePhase {
        LOBBY,
        DREAMSTART_WAITING,
        HEADSTART,
        RUNNING,
        ENDED
    }
    
    private GamePhase currentPhase = GamePhase.LOBBY;
    private long headstartEndTime = 0;
    private long gameStartTime = 0;
    private String winnerTeam = null;
    
    private GameManager() {}
    
    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }
    
    public GamePhase getCurrentPhase() {
        return currentPhase;
    }
    
    public long getHeadstartEndTime() {
        return headstartEndTime;
    }
    
    public long getGameStartTime() {
        return gameStartTime;
    }

    public void startGame(MinecraftServer server) {
        GameSettings settings = GameSettings.getInstance();

        setNobodyToSpectator(server);
        
        if (settings.isDreamStartEnabled()) {
            currentPhase = GamePhase.DREAMSTART_WAITING;
            LumaHunt.LOGGER.info("Game started in DreamStart mode - waiting for first hit");
        } else if (settings.getRunnerHeadStartSeconds() > 0) {
            currentPhase = GamePhase.HEADSTART;
            headstartEndTime = System.currentTimeMillis() + (settings.getRunnerHeadStartSeconds() * 1000L);
            LumaHunt.LOGGER.info("Game started with " + settings.getRunnerHeadStartSeconds() + "s headstart");

            setHuntersGameMode(server, GameMode.ADVENTURE);
        } else {
            currentPhase = GamePhase.RUNNING;
            gameStartTime = System.currentTimeMillis();
        }
        
        HunterTracker.getInstance().initializeTracking(server);

        broadcastTimerState(server);
    }
    
    public void onDreamStartTrigger(MinecraftServer server) {
        if (currentPhase != GamePhase.DREAMSTART_WAITING) {
            return;
        }
        
        LumaHunt.LOGGER.info("DreamStart triggered - timer starting");
        currentPhase = GamePhase.RUNNING;
        gameStartTime = System.currentTimeMillis();

        broadcastTimerState(server);
    }
    
    public void tick(MinecraftServer server) {
        if (currentPhase == GamePhase.HEADSTART && System.currentTimeMillis() >= headstartEndTime) {
            LumaHunt.LOGGER.info("Headstart ended - game running");
            currentPhase = GamePhase.RUNNING;
            gameStartTime = System.currentTimeMillis();

            setHuntersGameMode(server, GameMode.SURVIVAL);

            broadcastTimerState(server);
        }

        if (currentPhase == GamePhase.RUNNING || currentPhase == GamePhase.HEADSTART) {
            HunterTracker.getInstance().tick(server);
        }
    }

    public boolean onPlayerDeath(MinecraftServer server, ServerPlayerEntity player, String killerName) {
        if (currentPhase != GamePhase.RUNNING && currentPhase != GamePhase.HEADSTART && currentPhase != GamePhase.DREAMSTART_WAITING) {
            return false;
        }
        
        Role playerRole = LobbyManager.getInstance().getPlayerRole(player.getUuid());
        String victimName = player.getName().getString();
        
        if (playerRole == Role.RUNNER) {
            currentPhase = GamePhase.ENDED;
            winnerTeam = "HUNTER";
            
            String deathMessage = victimName + " was slain by " + (killerName != null ? killerName : "unknown");
            broadcastGameEnd(server, Role.HUNTER, deathMessage);
            
            LumaHunt.LOGGER.info("Runner died - Hunters win!");
            return true;
        } else if (playerRole == Role.HUNTER) {
            LumaHunt.LOGGER.info("Hunter " + victimName + " eliminated");
            return true;
        }
        
        return false;
    }

    public void onRunnerEnterEndGateway(MinecraftServer server, ServerPlayerEntity runner) {
        if (currentPhase != GamePhase.RUNNING) {
            return;
        }
        
        Role role = LobbyManager.getInstance().getPlayerRole(runner.getUuid());
        if (role != Role.RUNNER) {
            return;
        }
        
        currentPhase = GamePhase.ENDED;
        winnerTeam = "RUNNER";
        
        String winMessage = runner.getName().getString() + " escaped through the End Gateway!";
        broadcastGameEnd(server, Role.RUNNER, winMessage);
        
        LumaHunt.LOGGER.info("Runner escaped - Runner wins!");
    }
    
    private void setHuntersGameMode(MinecraftServer server, GameMode mode) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Role role = LobbyManager.getInstance().getPlayerRole(player.getUuid());
            if (role == Role.HUNTER) {
                player.changeGameMode(mode);
            }
        }
    }
    
    private void broadcastTimerState(MinecraftServer server) {
        int state = switch (currentPhase) {
            case DREAMSTART_WAITING -> 0;
            case HEADSTART -> 1;
            case RUNNING -> 2;
            case ENDED -> 3;
            default -> 0;
        };
        
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            NetworkHandler.sendTimerState(player, state, headstartEndTime, gameStartTime);
        }
    }
    
    private void broadcastGameEnd(MinecraftServer server, Role winnerRole, String message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Role playerRole = LobbyManager.getInstance().getPlayerRole(player.getUuid());
            boolean isWinner = (playerRole == winnerRole);

            player.changeGameMode(GameMode.SPECTATOR);
            
            NetworkHandler.sendGameEnd(player, isWinner, message);
        }
    }
    
    private void setNobodyToSpectator(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Role role = LobbyManager.getInstance().getPlayerRole(player.getUuid());
            if (role == Role.NOBODY) {
                player.changeGameMode(GameMode.SPECTATOR);
            }
        }
    }
    
    public void reset() {
        currentPhase = GamePhase.LOBBY;
        headstartEndTime = 0;
        gameStartTime = 0;
        winnerTeam = null;
    }
}
