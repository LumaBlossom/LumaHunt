package luma.hunt.client.ui;

import luma.hunt.client.screen.LumaGuiUtils;
import luma.hunt.client.screen.ModeSelectionScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LumaHuntMenuButton extends ButtonWidget {
    
    private float borderAnimation = 0f;
    private float hoverAlpha = 0f; // smooth hover transition
    private float compassTint = 0f; // smooth compass tint transition
    
    // mini particles
    private final List<MiniParticle> particles = new ArrayList<>();
    private static final int MAX_PARTICLES = 8;
    private final Random random = new Random();
    
    public LumaHuntMenuButton(int x, int y, int width, int height) {
        super(x, y, width, height, Text.literal(""), 
            button -> MinecraftClient.getInstance().setScreen(new ModeSelectionScreen(MinecraftClient.getInstance().currentScreen)),
            DEFAULT_NARRATION_SUPPLIER);
        
        for (int i = 0; i < MAX_PARTICLES; i++) {
            particles.add(new MiniParticle(width, height));
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
        
        float targetHover = hovered ? 1.0f : 0.0f;
        this.hoverAlpha += (targetHover - this.hoverAlpha) * 0.15f * delta;
        
        float targetTint = hovered ? 1.0f : 0.0f;
        this.compassTint += (targetTint - this.compassTint) * 0.1f * delta;
        
        this.borderAnimation += delta * 0.015F;
        if (this.borderAnimation > 1.0F) {
            this.borderAnimation -= 1.0F;
        }
        
        // background
        int bgAlpha = (int)(0xBB + (0xE0 - 0xBB) * hoverAlpha);
        int bgColor = (bgAlpha << 24) | 0x1A1A2E;
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgColor);
        
        // hover 
        if (hoverAlpha > 0.01f) {
            int overlayAlpha = (int)(0x40 * hoverAlpha);
            context.fill(x + 1, y + 1, x + w - 1, y + h - 1, (overlayAlpha << 24) | 0xFFFFFF);
        }
        
        updateAndRenderParticles(context, x, y, w, h, delta);
        
        LumaGuiUtils.renderAnimatedBorder(context, x, y, w, h, this.borderAnimation, 1.0f);
        
        // spinning compass
        int compassX = x + 8;
        int compassY = y + (h - 16) / 2;
        
        drawSpinningCompass(context, compassX, compassY, 16);
        
        String text = "LumaHunt";
        int textWidth = client.textRenderer.getWidth(text);
        
        int compassSpace = 28;
        int availableWidth = w - compassSpace - 8;
        int textX = x + compassSpace + (availableWidth - textWidth) / 2;
        int textY = y + (h - 8) / 2;
        
        // interpolate white -> pink
        int r = (int)(255 + (0xFF - 255) * compassTint);
        int g = (int)(255 + (0x69 - 255) * compassTint);
        int b = (int)(255 + (0xB4 - 255) * compassTint);
        int textColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        
        context.drawTextWithShadow(client.textRenderer, text, textX, textY, textColor);
    }
    
    private void drawSpinningCompass(DrawContext context, int x, int y, int size) {
        long time = System.currentTimeMillis();
        // 32 frames for compass animation
        int frame = (int)((time / 50) % 32); 
        String frameStr = String.format("%02d", frame);
        Identifier texture = new Identifier("minecraft", "textures/item/compass_" + frameStr + ".png");
        
        context.drawTexture(texture, x, y, 0, 0, 0, size, size, size, size);
    }
    
    private void updateAndRenderParticles(DrawContext context, int x, int y, int w, int h, float delta) {
        for (MiniParticle p : particles) {
            p.x += p.vx * delta * 0.5f;
            p.y += p.vy * delta * 0.5f;
            p.life -= delta * 0.02f;
            
            if (p.life <= 0 || p.x < 0 || p.x > w || p.y < 0 || p.y > h) {
                p.reset(w, h);
            }
            
            int alpha = (int)(p.life * 150);
            if (alpha > 0) {
                int color = (alpha << 24) | 0xFF69B4;
                int px = x + (int)p.x;
                int py = y + (int)p.y;
                context.fill(px, py, px + 2, py + 2, color);
            }
        }
    }
    
    private class MiniParticle {
        float x, y, vx, vy, life;
        
        MiniParticle(int maxW, int maxH) {
            reset(maxW, maxH);
        }
        
        void reset(int maxW, int maxH) {
            x = random.nextFloat() * maxW;
            y = random.nextFloat() * maxH;
            vx = (random.nextFloat() - 0.5f) * 0.3f;
            vy = (random.nextFloat() - 0.5f) * 0.3f;
            life = 0.5f + random.nextFloat() * 0.5f;
        }
    }
    
    public static LumaHuntMenuButton create() {
        int padding = 10;
        int buttonWidth = 120;
        int buttonHeight = 24;
        return new LumaHuntMenuButton(padding, padding, buttonWidth, buttonHeight);
    }
}
