package luma.hunt.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class CompassAnimationMixin {

    @Shadow
    private ItemStack mainHand;
    
    @Shadow
    private ItemStack offHand;
    
    @Shadow
    private float equipProgressMainHand;
    
    @Shadow
    private float equipProgressOffHand;

    @Inject(method = "updateHeldItems", at = @At("HEAD"))
    private void suppressCompassAnimation(CallbackInfo ci) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null) {
            return;
        }
        
        ItemStack currentMainHand = player.getMainHandStack();
        ItemStack currentOffHand = player.getOffHandStack();
        
        if (isTrackerCompass(mainHand) && isTrackerCompass(currentMainHand)) {
            equipProgressMainHand = 1.0F;
        }
        
        if (isTrackerCompass(offHand) && isTrackerCompass(currentOffHand)) {
            equipProgressOffHand = 1.0F;
        }
    }
    
    private boolean isTrackerCompass(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.getItem() == Items.COMPASS && 
               stack.hasCustomName() && 
               stack.getName().getString().startsWith("§dTracker:");
    }
}
