package luma.hunt.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ClientConfig {
    private static ClientConfig instance;
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("lumahunt-client.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean hasSeenWelcomeScreen = false;

    public static ClientConfig getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, ClientConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        if (instance == null) {
            instance = new ClientConfig();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
