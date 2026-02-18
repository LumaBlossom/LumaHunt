package luma.hunt.client.config;

import luma.hunt.LumaHunt;
import luma.hunt.config.LumaHuntConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

public class LumaHuntConfigScreen extends Screen {
    private final Screen parent;
    private CheckboxWidget offlineModeCheckbox;

    public LumaHuntConfigScreen(Screen parent) {
        super(Text.translatable("lumahunt.screen.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 4;

        this.offlineModeCheckbox = new CheckboxWidget(x, y, 200, 20, Text.translatable("lumahunt.screen.config.offline_mode"), LumaHunt.OFFLINE_MODE_ALLOWED);
        this.addDrawableChild(this.offlineModeCheckbox);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> {
            LumaHunt.OFFLINE_MODE_ALLOWED = this.offlineModeCheckbox.isChecked();
            LumaHuntConfig.save();
            this.client.setScreen(this.parent);
        })
        .dimensions(x, this.height - 40, 200, 20)
        .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        
        context.drawText(this.textRenderer, Text.translatable("lumahunt.screen.config.offline_desc"), 
                this.width / 2 - 100, this.height / 4 + 25, 0xAAAAAA, false);
        context.drawText(this.textRenderer, Text.translatable("lumahunt.screen.config.offline_warning"), 
                this.width / 2 - 100, this.height / 4 + 37, 0xFF5555, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        LumaHuntConfig.save();
        this.client.setScreen(this.parent);
    }
}
