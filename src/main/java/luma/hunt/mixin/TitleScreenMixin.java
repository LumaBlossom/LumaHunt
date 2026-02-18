package luma.hunt.mixin;

import luma.hunt.client.ui.LumaHuntMenuButton;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "init")
    private void addLumaHuntButton(CallbackInfo ci) {
        if (!luma.hunt.config.ClientConfig.getInstance().hasSeenWelcomeScreen) {
            // Open welcome screen on first launch
            net.minecraft.client.MinecraftClient.getInstance().setScreen(new luma.hunt.client.screen.WelcomeScreen(this));
        } else {
            this.addDrawableChild(LumaHuntMenuButton.create());
        }
    }
}
