package luma.hunt.client.ui;

import luma.hunt.client.screen.LumaGuiUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Function;

public class ModernSlider extends SliderWidget {
    
    private float borderAnimation = 0f;
    private float borderAlpha = 0f;
    
    private final Text label;
    private final int min;
    private final int max;
    private final Consumer<Integer> onChange;
    
    public ModernSlider(int x, int y, int width, int height, Text label, int min, int max, int initialValue, java.util.function.Consumer<Integer> onChange) {
        super(x, y, width, height, Text.empty(), (double)(initialValue - min) / (max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        this.updateMessage();
    }
    
    public int getValue() {
        return min + (int)Math.round(this.value * (max - min));
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Text.empty().append(label).append(": " + getValue()));
    }

    @Override
    protected void applyValue() {
        if (onChange != null) {
            onChange.accept(getValue());
        }
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

        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xAA1A1A2E);
        
        if (this.borderAlpha < 0.95F) {
             int outlineColor = 0xFFAAAAAA;
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
        int handleColor = hovered ? 0xFFFF69B4 : 0xFFCCCCCC;
        
        int handlePadding = (hovered && this.borderAlpha > 0.5f) ? 2 : 1;
        context.fill(handleX, y + handlePadding, handleX + handleWidth, y + h - handlePadding, handleColor);
        
        String text = this.getMessage().getString();
        int textWidth = client.textRenderer.getWidth(text);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (h - 8) / 2;
        
        context.drawTextWithShadow(client.textRenderer, text, textX, textY, 0xFFFFFFFF);
    }
}
