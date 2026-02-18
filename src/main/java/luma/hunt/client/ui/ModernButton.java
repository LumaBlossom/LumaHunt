package luma.hunt.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import luma.hunt.client.screen.LumaGuiUtils;

public class ModernButton extends ButtonWidget {
    
    public enum Style {
        DEFAULT(0x00000000, 0x40FFFFFF, 0xFFFFFFFF, 0xFFFFFFFF),
        SUCCESS(0x00000000, 0x4000FF00, 0xFF00FF00, 0xFFFFFFFF),
        DANGER(0x00000000, 0x40FF0000, 0xFFFF0000, 0xFFFFFFFF),
        SECONDARY(0x00000000, 0x40888888, 0xFF888888, 0xFFAAAAAA);
        
        final int baseColor;
        final int hoverColor;
        final int outlineColor;
        final int textColor;
        
        Style(int baseColor, int hoverColor, int outlineColor, int textColor) {
            this.baseColor = baseColor;
            this.hoverColor = hoverColor;
            this.outlineColor = outlineColor;
            this.textColor = textColor;
        }
    }
    
    private final Style style;
    private float hoverProgress = 0f;
    private float borderAnimation = 0f;
    private float borderAlpha = 0f;

    public ModernButton(int x, int y, int width, int height, Text message, PressAction onPress, Style style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.style = style;
    }
    
    public static ModernButton create(int x, int y, int width, int height, Text message, PressAction onPress) {
        return new ModernButton(x, y, width, height, message, onPress, Style.DEFAULT);
    }
    
    public static ModernButton primary(int x, int y, int width, int height, Text message, PressAction onPress) {
        return new ModernButton(x, y, width, height, message, onPress, Style.DEFAULT);
    }
    
    public static ModernButton success(int x, int y, int width, int height, Text message, PressAction onPress) {
        return new ModernButton(x, y, width, height, message, onPress, Style.SUCCESS);
    }
    
    public static ModernButton danger(int x, int y, int width, int height, Text message, PressAction onPress) {
        return new ModernButton(x, y, width, height, message, onPress, Style.DANGER);
    }
    
    public static ModernButton secondary(int x, int y, int width, int height, Text message, PressAction onPress) {
        return new ModernButton(x, y, width, height, message, onPress, Style.SECONDARY);
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        
        boolean hovered = this.isHovered();
        
        float targetProgress = hovered ? 1f : 0f;
        float animSpeed = 0.12f;
        if (hoverProgress < targetProgress) {
            hoverProgress = Math.min(targetProgress, hoverProgress + animSpeed);
        } else if (hoverProgress > targetProgress) {
            hoverProgress = Math.max(targetProgress, hoverProgress - animSpeed);
        }
        
        if (hovered) {
             this.borderAnimation += delta * 0.02F;
             if (this.borderAnimation > 1.0F) {
                this.borderAnimation -= 1.0F;
             }
        }
        
        float targetAlpha = hovered ? 1.0F : 0.0F;
        this.borderAlpha += (targetAlpha - this.borderAlpha) * 0.2F * delta;
        
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();
        
        int outlineColor = style.outlineColor;
        int fillColor = style.baseColor;
        int textColor = style.textColor;
        
        if (!this.active) {
            outlineColor = 0xFF444444;
            textColor = 0xFF666666;
        }
        
        if (this.active && hoverProgress > 0) {
            int alpha = (int)(hoverProgress * 0x40);
            fillColor = (alpha << 24) | 0xFFFFFF;
        }
        
        if ((fillColor >> 24) != 0) {
            context.fill(x + 1, y + 1, x + w - 1, y + h - 1, fillColor);
        }
        
        if (this.borderAlpha < 0.95F) {
             context.fill(x, y, x + w, y + 1, outlineColor);
             context.fill(x, y + h - 1, x + w, y + h, outlineColor);
             context.fill(x, y, x + 1, y + h, outlineColor);
             context.fill(x + w - 1, y, x + w, y + h, outlineColor);
        }
        
        if (this.borderAlpha > 0.01F && this.active) {
            LumaGuiUtils.renderAnimatedBorder(context, x, y, w, h, this.borderAnimation, this.borderAlpha);
        }
        
        String text = this.getMessage().getString();
        int textWidth = textRenderer.getWidth(text);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (h - 8) / 2;
        
        context.drawTextWithShadow(textRenderer, text, textX, textY, textColor);
    }
}

