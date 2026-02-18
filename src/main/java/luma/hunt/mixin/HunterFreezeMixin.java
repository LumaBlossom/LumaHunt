package luma.hunt.mixin;

import luma.hunt.client.hud.TimerHud;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class HunterFreezeMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void freezeHunterMovement(boolean slowDown, float f, CallbackInfo ci) {
        if (TimerHud.shouldFreezePlayer()) {
            Input input = (Input) (Object) this;
            input.movementForward = 0;
            input.movementSideways = 0;
            input.jumping = false;
            input.sneaking = false;
        }
    }
}
