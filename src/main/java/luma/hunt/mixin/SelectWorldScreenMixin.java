package luma.hunt.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SelectWorldScreen.class)
public class SelectWorldScreenMixin extends Screen {

    protected SelectWorldScreenMixin(Text title) {
        super(title);
    }
}
