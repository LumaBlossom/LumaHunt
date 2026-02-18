package luma.hunt.mixin;

import luma.hunt.LumaHunt;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {

    @Inject(method = "openToLan", at = @At("HEAD"))
    private void lumahunt$setOfflineMode(CallbackInfoReturnable<Boolean> cir) {
        if (LumaHunt.OFFLINE_MODE_ALLOWED) {
            IntegratedServer server = (IntegratedServer)(Object)this;
            server.setOnlineMode(false);
            LumaHunt.LOGGER.info("Set server to offline mode for e4mc tunneling");

            Text message = Text.of("§8[§dLumaHunt§8]§r Server set to §eoffline mode§r for tunneling");
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(message, false);
            }
        }
    }
}
