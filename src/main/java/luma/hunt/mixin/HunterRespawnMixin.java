package luma.hunt.mixin;

import luma.hunt.config.GameSettings;
import luma.hunt.lobby.LobbyManager;
import luma.hunt.logic.HunterTracker;
import luma.hunt.logic.Role;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class HunterRespawnMixin {

    @Inject(method = "onSpawn", at = @At("TAIL"))
    private void onPlayerRespawn(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        
        if (!GameSettings.getInstance().isGameActive()) {
            return;
        }
        
        Role role = LobbyManager.getInstance().getPlayerRole(player.getUuid());
        
        if (role == Role.HUNTER && player.getServer() != null) {
            HunterTracker.getInstance().giveCompassToHunter(player, player.getServer());
        }
    }
}
