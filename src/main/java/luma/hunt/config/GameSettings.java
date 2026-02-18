package luma.hunt.config;

public class GameSettings {
    private static GameSettings instance;
    
    // index 0 = DreamStart, indices 1+ = headstart seconds
    private static final int[] HEADSTART_VALUES = {0, 10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 90, 100, 120, 150, 180};
    
    private int startModeIndex = 0;
    private boolean gameActive = false;
    private int maxPlayers = 12;
    
    private GameSettings() {}
    
    public static GameSettings getInstance() {
        if (instance == null) {
            instance = new GameSettings();
        }
        return instance;
    }
    
    public boolean isDreamStartEnabled() {
        return startModeIndex == 0;
    }
    
    public int getRunnerHeadStartSeconds() {
        if (startModeIndex == 0) return 0;
        int idx = Math.min(startModeIndex, HEADSTART_VALUES.length - 1);
        return HEADSTART_VALUES[idx];
    }
    
    public int getStartModeIndex() {
        return startModeIndex;
    }
    
    public void setStartModeIndex(int index) {
        this.startModeIndex = Math.max(0, Math.min(index, HEADSTART_VALUES.length - 1));
    }
    
    public static int[] getHeadstartValues() {
        return HEADSTART_VALUES;
    }
    
    public static int getMaxStartModeIndex() {
        return HEADSTART_VALUES.length - 1;
    }
    
    public static String getStartModeLabel(int index) {
        if (index == 0) return "DreamStart";
        if (index >= 1 && index < HEADSTART_VALUES.length) {
            return HEADSTART_VALUES[index] + "s";
        }
        return "Unknown";
    }
    
    public boolean isGameActive() {
        return gameActive;
    }
    
    public void setGameActive(boolean active) {
        this.gameActive = active;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = Math.max(2, Math.min(maxPlayers, 20)); 
    }
    
    public void reset() {
        this.startModeIndex = 0;
        this.gameActive = false;
    }
}
