package luma.hunt.client.screen;

import luma.hunt.client.ui.LumaParticleSystem;
import luma.hunt.client.ui.ModernButton;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;

public class ModeSelectionScreen extends AbstractLumaHuntScreen {

    public ModeSelectionScreen(Screen parent) {
        super(Text.literal(""), parent);
    }

    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 220;
        int buttonHeight = 28;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 4 + 48; // Adjusted relative positioning
        int gap = 36;

        this.addDrawableChild(ModernButton.primary(centerX, startY, buttonWidth, buttonHeight,
            Text.translatable("lumahunt.screen.mode.host"), button -> {
                // quick create world -> open lobby
                luma.hunt.LumaHunt.AUTO_CREATE_LUMA_HUNT = true;
                net.minecraft.client.gui.screen.world.CreateWorldScreen.create(this.client, this);
            }));

        this.addDrawableChild(ModernButton.secondary(centerX, startY + gap + 15, buttonWidth, buttonHeight,
            Text.translatable("lumahunt.screen.mode.join"), button -> {
                this.client.setScreen(new JoinGameScreen(this));
            }));

        this.addDrawableChild(ModernButton.danger(centerX, this.height - 45, buttonWidth, buttonHeight,
            Text.translatable("lumahunt.screen.mode.close"), button -> this.close()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF000000);
        renderPinkGradient(context);
        LumaParticleSystem.getInstance().update();
        LumaParticleSystem.getInstance().render(context);
        
        drawTitle(context);
        
        int settingsY = (this.height / 4 + 48) + 36 * 2 + 15;
        int dividerWidth = 180;
        context.fill(this.width / 2 - dividerWidth / 2, settingsY, this.width / 2 + dividerWidth / 2, settingsY + 1, 0x40FFFFFF);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void drawTitle(DrawContext context) {
        String title = "ʟᴜᴍᴀʜᴜɴᴛ";
        int titleX = this.width / 2;
        int titleY = 25;
        
        context.drawCenteredTextWithShadow(this.textRenderer, title, titleX, titleY, 0xFFFF69B4);
        
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("lumahunt.screen.mode.select"), titleX, titleY + 15, 0xFFAAAAAA);
    }
}

