package luma.hunt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import luma.hunt.network.NetworkHandler;
import luma.hunt.lobby.LobbyManager;

public class LumaHunt implements ModInitializer {
    public static final String MOD_ID = "lumahunt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static boolean AUTO_CREATE_LUMA_HUNT = false;
    public static boolean DISABLE_DAYLIGHT_ON_CREATE = false;
    public static boolean OFFLINE_MODE_ALLOWED = false;

    @Override
    public void onInitialize() {
        LOGGER.info("Hello from Luma! Thanks for installing LumaHunt :)");
        luma.hunt.config.LumaHuntConfig.load();
        NetworkHandler.registerServerReceivers();
        LobbyManager.init();
        luma.hunt.event.EventHandler.init();
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
