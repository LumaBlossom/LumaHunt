package luma.hunt.client.ui;

import luma.hunt.client.screen.LumaGuiUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

public class ModernTextField extends TextFieldWidget {
    
    private float borderAnimation = 0f;
    private float borderAlpha = 0f;
    
    public ModernTextField(TextRenderer textRenderer, int x, int y, int width, int height, Text placeholder) {
        super(textRenderer, x, y, width, height, placeholder);
        this.setDrawsBackground(false);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();
        
        boolean focused = this.isFocused();
        boolean hovered = this.isHovered();
        boolean active = focused || hovered;
        
        if (active) {
             this.borderAnimation += delta * 0.02F;
             if (this.borderAnimation > 1.0F) {
                this.borderAnimation -= 1.0F;
             }
        }
        
        float targetAlpha = active ? 1.0F : 0.0F;
        this.borderAlpha += (targetAlpha - this.borderAlpha) * 0.2F * delta;
        
        int bgColor = focused ? 0xCC1A1A2E : 0xAA1A1A2E;
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgColor);
        
        int outlineColor = focused ? 0xFFFF69B4 : 0xFFAAAAAA;
        if (this.borderAlpha < 0.95F) {
             context.fill(x, y, x + w, y + 1, outlineColor);
             context.fill(x, y + h - 1, x + w, y + h, outlineColor);
             context.fill(x, y, x + 1, y + h, outlineColor);
             context.fill(x + w - 1, y, x + w, y + h, outlineColor);
        }
        
        if (this.borderAlpha > 0.01F) {
            LumaGuiUtils.renderAnimatedBorder(context, x, y, w, h, this.borderAnimation, this.borderAlpha);
        }
        
        String text = this.getText();
        int textColor = 0xFFFFFFFF;
        int textY = y + (h - 8) / 2;
        
        if (text.isEmpty() && !focused) {
            String placeholderText = "Type code here...";
            int placeholderWidth = client.textRenderer.getWidth(placeholderText);
            int centeredX = x + (w - placeholderWidth) / 2;
            context.drawTextWithShadow(client.textRenderer, placeholderText, centeredX, textY, 0xFF888888);
        } else {
            String displayText = text.toUpperCase();
            int textWidth = client.textRenderer.getWidth(displayText);
            int centeredX = x + (w - textWidth) / 2;
            context.drawTextWithShadow(client.textRenderer, displayText, centeredX, textY, textColor);

            if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cursorX = centeredX + textWidth + 1;
                context.fill(cursorX, textY, cursorX + 1, textY + 8, 0xFFFF69B4);
            }
        }
        
        if (focused && text.isEmpty() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = x + w / 2;
            context.fill(cursorX, textY, cursorX + 1, textY + 8, 0xFFFF69B4);
        }
    }
    
    public static ModernTextField codeInput(TextRenderer textRenderer, int x, int y, int width, int height) {
        ModernTextField field = new ModernTextField(textRenderer, x, y, width, height, Text.literal("Code"));
        field.setMaxLength(5);
        return field;
    }
}

