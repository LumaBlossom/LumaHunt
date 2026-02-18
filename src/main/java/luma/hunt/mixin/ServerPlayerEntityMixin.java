package luma.hunt.mixin;

import luma.hunt.config.GameSettings;
import luma.hunt.lobby.LobbyManager;
import luma.hunt.logic.GameManager;
import luma.hunt.logic.Role;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "moveToWorld", at = @At("HEAD"))
    private void onMoveToWorld(ServerWorld destination, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (!GameSettings.getInstance().isGameActive()) {
            return;
        }

        Role role = LobbyManager.getInstance().getPlayerRole(player.getUuid());
        if (role != Role.RUNNER) {
            return;
        }

        ServerWorld currentWorld = player.getServerWorld();
        if (currentWorld.getRegistryKey() == World.END && destination.getRegistryKey() == World.OVERWORLD) {
            GameManager.getInstance().onRunnerEnterEndGateway(player.getServer(), player);
        }
    }
}
