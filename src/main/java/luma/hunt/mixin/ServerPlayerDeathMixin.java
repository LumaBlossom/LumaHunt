package luma.hunt.mixin;

import luma.hunt.config.GameSettings;
import luma.hunt.lobby.LobbyManager;
import luma.hunt.logic.GameManager;
import luma.hunt.logic.Role;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerDeathMixin {

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void onPlayerDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        MinecraftServer server = player.getServer();
        
        if (server == null || !GameSettings.getInstance().isGameActive()) {
            return;
        }

        Role playerRole = LobbyManager.getInstance().getPlayerRole(player.getUuid());

        if (playerRole != Role.RUNNER) {
            return;
        }
        
        String killerName = null;
        if (source.getAttacker() instanceof PlayerEntity attacker) {
            killerName = attacker.getName().getString();
        }
        
        boolean shouldCancel = GameManager.getInstance().onPlayerDeath(server, player, killerName);
        
        if (shouldCancel) {
            ci.cancel();
            player.setHealth(player.getMaxHealth());
            player.getHungerManager().setFoodLevel(20);
            player.changeGameMode(GameMode.SPECTATOR);
            player.teleport(player.getServerWorld(), 
                player.getX(), player.getY() + 2, player.getZ(),
                player.getYaw(), player.getPitch());
        }
    }
}
