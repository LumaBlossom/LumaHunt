package luma.hunt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import luma.hunt.client.screen.LobbyScreen;
import luma.hunt.client.screen.ModeSelectionScreen;
import luma.hunt.network.NetworkHandler;
import luma.hunt.lobby.LobbyManager;

public class LumaHuntClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkHandler.registerClientReceivers();
        
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            checkAutoOpen(client);
            luma.hunt.client.hud.TimerHud.onTick(client);
        });
        
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(luma.hunt.client.hud.TimerHud::render);
    }
    
    private boolean hasOpenedLobby = false;
    private String lastWorldName = "";

    private void checkAutoOpen(net.minecraft.client.MinecraftClient client) {
        if (client.world != null && client.getServer() != null && client.getServer().isSingleplayer()) {
             String worldName = client.getServer().getSaveProperties().getLevelName();
             
             // reset flag if world changed
             if (!worldName.equals(lastWorldName)) {
                 lastWorldName = worldName;
                 hasOpenedLobby = false;
             }
             
             // auto-open lobby for lumahunt worlds
             if (worldName.startsWith("LumaHunt #") && !hasOpenedLobby) {
                 if (client.currentScreen == null && client.getOverlay() == null) {
                      hasOpenedLobby = true;
                      
                      if (LobbyManager.getInstance().getCurrentLobbyCode() == null) {
                          LobbyManager.getInstance().createLobby("XvX");
                      }
                      
                      client.setScreen(new LobbyScreen(null, true));
                 }
             }
        }
    }

}

