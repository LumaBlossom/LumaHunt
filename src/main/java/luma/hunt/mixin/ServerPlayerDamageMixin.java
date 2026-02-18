package luma.hunt.mixin;

import luma.hunt.config.GameSettings;
import luma.hunt.lobby.LobbyManager;
import luma.hunt.logic.GameManager;
import luma.hunt.logic.Role;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void onServerDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity victim = (ServerPlayerEntity) (Object) this;
        MinecraftServer server = victim.getServer();
        
        if (server == null || !GameSettings.getInstance().isGameActive()) {
            return;
        }

        if (GameManager.getInstance().getCurrentPhase() != GameManager.GamePhase.DREAMSTART_WAITING) {
            return;
        }
        
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity attackerPlayer)) {
            return;
        }
        
        Role attackerRole = LobbyManager.getInstance().getPlayerRole(attackerPlayer.getUuid());
        Role victimRole = LobbyManager.getInstance().getPlayerRole(victim.getUuid());
        
        if (attackerRole == Role.RUNNER && victimRole == Role.HUNTER) {
            GameManager.getInstance().onDreamStartTrigger(server);
        }
    }
}
