package luma.hunt.mixin;

import luma.hunt.lobby.LobbyManager;
import luma.hunt.logic.GameManager;
import luma.hunt.logic.HunterTracker;
import luma.hunt.logic.Role;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class CompassUseMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (world.isClient) return;
        
        ItemStack stack = user.getStackInHand(hand);
        
        if (stack.getItem() == Items.COMPASS && 
            stack.hasCustomName() && 
            stack.getName().getString().startsWith("§dTracker:")) {

            if (user instanceof ServerPlayerEntity serverPlayer) {
                Role role = LobbyManager.getInstance().getPlayerRole(serverPlayer.getUuid());
                
                if (role == Role.HUNTER && 
                    (GameManager.getInstance().getCurrentPhase() == GameManager.GamePhase.RUNNING ||
                     GameManager.getInstance().getCurrentPhase() == GameManager.GamePhase.HEADSTART)) {
                    HunterTracker.getInstance().cycleTarget(serverPlayer, serverPlayer.getServer());
                    
                    cir.setReturnValue(TypedActionResult.success(stack, true));
                }
            }
        }
    }
}
