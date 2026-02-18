package luma.hunt.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import luma.hunt.LumaHunt;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LumaHuntConfig {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("lumahunt.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data != null) {
                    LumaHunt.OFFLINE_MODE_ALLOWED = data.offlineModeAllowed;
                }
            } catch (IOException e) {
                LumaHunt.LOGGER.error("Failed to load config", e);
            }
        } else {
            save();
        }
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.offlineModeAllowed = LumaHunt.OFFLINE_MODE_ALLOWED;

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            LumaHunt.LOGGER.error("Failed to save config", e);
        }
    }

    private static class ConfigData {
        boolean offlineModeAllowed = false;
    }
}
