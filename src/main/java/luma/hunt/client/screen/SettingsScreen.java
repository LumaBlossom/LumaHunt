package luma.hunt.client.screen;

import luma.hunt.client.ui.LumaParticleSystem;
import luma.hunt.client.ui.ModernButton;
import luma.hunt.config.GameSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class SettingsScreen extends AbstractLumaHuntScreen {
    
    private StartModeSlider startModeSlider;
    private int pendingStartModeIndex;

    public SettingsScreen(Screen parent) {
        super(Text.literal(""), parent);
        this.pendingStartModeIndex = GameSettings.getInstance().getStartModeIndex();
    }

    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 280;
        int buttonHeight = 28;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 4 + 40;
        int gap = 35;

        this.startModeSlider = new StartModeSlider(
            centerX, startY, buttonWidth, buttonHeight, pendingStartModeIndex
        );
        this.addDrawableChild(startModeSlider);
        
        int currentMax = GameSettings.getInstance().getMaxPlayers();
        this.addDrawableChild(new luma.hunt.client.ui.ModernSlider(
            centerX, startY + gap, buttonWidth, buttonHeight, 
            Text.translatable("lumahunt.screen.settings.max_players"), 2, 12, currentMax,
            value -> GameSettings.getInstance().setMaxPlayers(value)
        ));
        
        this.addDrawableChild(ModernButton.success(centerX, this.height - 85, buttonWidth, buttonHeight,
            Text.translatable("lumahunt.screen.settings.save"), button -> {
                GameSettings.getInstance().setStartModeIndex(pendingStartModeIndex);
                this.close();
            }));
        
        this.addDrawableChild(ModernButton.secondary(centerX, this.height - 50, buttonWidth, buttonHeight,
            Text.translatable("lumahunt.screen.settings.back"), button -> this.close()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF000000);
        renderPinkGradient(context);
        LumaParticleSystem.getInstance().update();
        LumaParticleSystem.getInstance().render(context);
        drawTitle(context);
        
        int startY = this.height / 4 + 40;
        int sectionY = startY - 25;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("lumahunt.screen.settings.title_edit"), this.width / 2, sectionY, 0xFFAAAAAA);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void drawTitle(DrawContext context) {
        String title = "ʟᴜᴍᴀʜᴜɴᴛ";
        int titleX = this.width / 2;
        int titleY = 20;
        
        context.drawCenteredTextWithShadow(this.textRenderer, title, titleX, titleY, 0xFFFF69B4);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("lumahunt.screen.settings.title"), titleX, titleY + 12, 0xFFAAAAAA);
    }

    private class StartModeSlider extends SliderWidget {
        
        private float borderAnimation = 0f;
        private float borderAlpha = 0f;
        
        public StartModeSlider(int x, int y, int width, int height, int initialIndex) {
            super(x, y, width, height, 
                Text.literal("Start Mode: " + GameSettings.getStartModeLabel(initialIndex)),
                (double) initialIndex / GameSettings.getMaxStartModeIndex());
        }

        @Override
        protected void updateMessage() {
            int index = (int) Math.round(this.value * GameSettings.getMaxStartModeIndex());
            this.setMessage(Text.literal("Start Mode: " + GameSettings.getStartModeLabel(index)));
        }

        @Override
        protected void applyValue() {
            int index = (int) Math.round(this.value * GameSettings.getMaxStartModeIndex());
            pendingStartModeIndex = index;
        }

        @Override
        public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            MinecraftClient client = MinecraftClient.getInstance();
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();
            boolean hovered = this.isHovered();

            if (hovered) {
                 this.borderAnimation += delta * 0.02F;
                 if (this.borderAnimation > 1.0F) {
                    this.borderAnimation -= 1.0F;
                 }
            }
            
            float targetAlpha = hovered ? 1.0F : 0.0F;
            this.borderAlpha += (targetAlpha - this.borderAlpha) * 0.2F * delta;
            int outlineColor = 0xFFAAAAAA;

            if (this.borderAlpha < 0.95F) {
                 context.fill(x, y, x + w, y + 1, outlineColor);
                 context.fill(x, y + h - 1, x + w, y + h, outlineColor);
                 context.fill(x, y, x + 1, y + h, outlineColor);
                 context.fill(x + w - 1, y, x + w, y + h, outlineColor);
            }
            
            if (this.borderAlpha > 0.01F) {
                LumaGuiUtils.renderAnimatedBorder(context, x, y, w, h, this.borderAnimation, this.borderAlpha);
            }
            
            int handleWidth = 8;
            int handleX = x + (int)(this.value * (w - handleWidth));
            int handleColor = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
            int handleBorderThickness = (hovered && this.borderAlpha > 0.5f) ? 2 : 1;
            context.fill(handleX, y + handleBorderThickness, handleX + handleWidth, y + h - handleBorderThickness, handleColor);

            String text = this.getMessage().getString();
            int textWidth = client.textRenderer.getWidth(text);
            int textX = x + (w - textWidth) / 2;
            int textY = y + (h - 8) / 2;
            
            context.drawTextWithShadow(client.textRenderer, text, textX, textY, 0xFFFFFFFF);
        }
    }
}
