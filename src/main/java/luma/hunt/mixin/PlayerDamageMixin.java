package luma.hunt.mixin;

import luma.hunt.client.hud.TimerHud;
import luma.hunt.config.GameSettings;
import luma.hunt.lobby.LobbyManager;
import luma.hunt.logic.Role;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity victim = (PlayerEntity) (Object) this;
        if (!GameSettings.getInstance().isGameActive()) {
            return;
        }
        
        if (!TimerHud.isDreamStartWaiting()) {
            return;
        }
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof PlayerEntity attackerPlayer)) {
            return;
        }
        Role attackerRole = LobbyManager.getInstance().getPlayerRole(attackerPlayer.getUuid());
        Role victimRole = LobbyManager.getInstance().getPlayerRole(victim.getUuid());
        
        if (attackerRole == Role.RUNNER && victimRole == Role.HUNTER) {
            TimerHud.onFirstHit();
        }
    }
}
