package luma.hunt.mixin;

import luma.hunt.client.hud.TimerHud;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class CameraFreezeMixin {

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void freezeHunterCamera(CallbackInfo ci) {
        if (TimerHud.shouldFreezePlayer()) {
            ci.cancel();
        }
    }
}
