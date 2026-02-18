package luma.hunt.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import luma.hunt.config.GameSettings;
import luma.hunt.lobby.LobbyManager;
import luma.hunt.logic.Role;

public class TimerHud {
    private static long startTime = 0;
    private static boolean isRunning = false;
    
    // hunters frozen
    private static long headStartEndTime = 0;
    private static boolean headStartActive = false;
    
    // waiting for runner to hit hunter
    private static boolean dreamStartWaiting = false;
    
    // game ended
    private static boolean gameStopped = false;
    private static long stoppedElapsedTime = 0;

    public static void startTimer() {
        startTime = System.currentTimeMillis();
        isRunning = true;
        dreamStartWaiting = false;
        gameStopped = false;
    }
    
    public static void stopTimer() {
        if (isRunning) {
            stoppedElapsedTime = System.currentTimeMillis() - startTime;
        }
        isRunning = false;
        gameStopped = true;
    }
    
    public static void resetTimer() {
        isRunning = false;
        startTime = 0;
        headStartActive = false;
        dreamStartWaiting = false;
        gameStopped = false;
        stoppedElapsedTime = 0;
    }
    
    public static void initializeGameStart() {
        GameSettings settings = GameSettings.getInstance();
        gameStopped = false;
        
        if (settings.isDreamStartEnabled()) {
            dreamStartWaiting = true;
            isRunning = false;
        } else if (settings.getRunnerHeadStartSeconds() > 0) {
            headStartActive = true;
            headStartEndTime = System.currentTimeMillis() + (settings.getRunnerHeadStartSeconds() * 1000L);
            isRunning = false;
        } else {
            startTimer();
        }
        
        settings.setGameActive(true);
    }
    
    // state: 0=dreamstart, 1=headstart, 2=running, 3=stopped
    public static void setStateFromPacket(int state, long serverHeadstartEndTime, long serverGameStartTime) {
        switch (state) {
            case 0:
                dreamStartWaiting = true;
                headStartActive = false;
                isRunning = false;
                gameStopped = false;
                break;
            case 1:
                dreamStartWaiting = false;
                headStartActive = true;
                headStartEndTime = serverHeadstartEndTime;
                isRunning = false;
                gameStopped = false;
                break;
            case 2:
                dreamStartWaiting = false;
                headStartActive = false;
                startTime = serverGameStartTime;
                isRunning = true;
                gameStopped = false;
                break;
            case 3:
                if (isRunning) {
                    stoppedElapsedTime = System.currentTimeMillis() - startTime;
                }
                dreamStartWaiting = false;
                headStartActive = false;
                isRunning = false;
                gameStopped = true;
                break;
        }
    }
    
    public static void onFirstHit() {
        if (dreamStartWaiting) {
            dreamStartWaiting = false;
            startTimer();
        }
    }
    
    public static boolean isDreamStartWaiting() {
        return dreamStartWaiting;
    }
    
    public static boolean isHeadStartActive() {
        return headStartActive;
    }
    
    public static boolean shouldFreezePlayer() {
        if (!headStartActive) return false;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        Role role = LobbyManager.getInstance().getPlayerRole(client.player.getUuid());
        return role == Role.HUNTER;
    }

    public static void onTick(MinecraftClient client) {
        if (headStartActive && System.currentTimeMillis() >= headStartEndTime) {
            headStartActive = false;
            startTimer();
        }
    }

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null) return;
        
        boolean gameActive = isRunning || dreamStartWaiting || headStartActive || gameStopped;
        if (!gameActive && !GameSettings.getInstance().isGameActive()) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int y = 10;
        int padding = 6;
        
        Role role = LobbyManager.getInstance().getPlayerRole(client.player.getUuid());
        
        String timerText;
        int timerColor = 0xFF69B4;
        
        if (dreamStartWaiting) {
            timerText = "⏳ Waiting for Hit...";
            timerColor = 0xFFAA00;
        } else if (headStartActive) {
            long remaining = headStartEndTime - System.currentTimeMillis();
            if (remaining < 0) remaining = 0;
            long seconds = remaining / 1000;
            long millis = remaining % 1000;
            
            if (role == Role.HUNTER) {
                timerText = String.format("❄ FROZEN: %d.%01d", seconds, millis / 100);
                timerColor = 0xFF4444;
            } else {
                timerText = String.format("⚡ Head Start: %d.%01d", seconds, millis / 100);
                timerColor = 0x00FF00;
            }
        } else if (gameStopped) {
            timerText = formatTime(stoppedElapsedTime) + " §7(ENDED)";
            timerColor = 0xAAAAAA;
        } else if (isRunning) {
            long elapsed = System.currentTimeMillis() - startTime;
            timerText = formatTime(elapsed);
        } else {
            timerText = "--:--";
        }
        
        String roleText;
        int roleColor;
        switch (role) {
            case RUNNER -> {
                roleText = "☄ RUNNER";
                roleColor = 0x00FF00;
            }
            case HUNTER -> {
                roleText = "⚔ HUNTER";
                roleColor = 0xFF0000;
            }
            default -> {
                roleText = "♪ SPECTATOR";
                roleColor = 0x888888;
            }
        }
        
        int timerWidth = client.textRenderer.getWidth(timerText);
        int roleWidth = client.textRenderer.getWidth(roleText);
        int gap = 15;
        
        int timerX = screenWidth - timerWidth - 10;
        int roleX = timerX - gap - roleWidth;
        
        context.fill(roleX - padding, y - padding, roleX + roleWidth + padding, y + 9 + padding, 0xAA000000);
        context.drawTextWithShadow(client.textRenderer, roleText, roleX, y, roleColor);
        
        context.fill(timerX - padding, y - padding, timerX + timerWidth + padding, y + 9 + padding, 0xAA000000);
        context.drawTextWithShadow(client.textRenderer, timerText, timerX, y, timerColor);
    }
    
    private static String formatTime(long elapsed) {
        // Clamp to 0 - never show negative time
        if (elapsed < 0) elapsed = 0;
        
        long totalSeconds = elapsed / 1000;
        long millis = elapsed % 1000;
        long minutes = totalSeconds / 60;
        long hours = minutes / 60;
        long seconds = totalSeconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
        } else {
            return String.format("%02d:%02d.%03d", minutes, seconds, millis);
        }
    }
}

