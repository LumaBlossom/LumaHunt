package luma.hunt.mixin;

import luma.hunt.LumaHunt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    @Shadow protected abstract void createLevel();
    @Shadow private WorldCreator worldCreator;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (LumaHunt.AUTO_CREATE_LUMA_HUNT) {
            LumaHunt.AUTO_CREATE_LUMA_HUNT = false;
            
            MinecraftClient client = MinecraftClient.getInstance();
            String worldName = "LumaHunt #1";
            int maxNum = 0;
            try {
                Path savesDir = client.getLevelStorage().getSavesDirectory();
                try (java.util.stream.Stream<Path> paths = java.nio.file.Files.list(savesDir)) {
                    for (Path path : (Iterable<Path>) paths::iterator) {
                        if (java.nio.file.Files.isDirectory(path)) {
                            String name = path.getFileName().toString();
                            if (name.startsWith("LumaHunt")) {
                                try {
                                    String numStr = name.replaceAll("[^0-9]", "");
                                    if (!numStr.isEmpty()) {
                                        int num = Integer.parseInt(numStr);
                                        if (num > maxNum) maxNum = num;
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
               worldName = "LumaHunt #" + (maxNum + 1);
               
            } catch (Exception e) {
                LumaHunt.LOGGER.error("Failed to calc world name", e);
            }

            if (this.worldCreator != null) {
                this.worldCreator.setWorldName(worldName);
            }
            
            LumaHunt.DISABLE_DAYLIGHT_ON_CREATE = true;
            
            this.createLevel();
        }
    }
}
