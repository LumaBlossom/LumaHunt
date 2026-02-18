package luma.hunt.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class LumaGuiUtils {
    
    private static final int COL_TOP = 0xFF000000;
    private static final int COL_MID = 0xFFFF73EC;
    private static final int COL_BOT = 0xFF2A1A2E;
    
    private static final List<Particle> particles = new ArrayList<>();
    private static final Random random = new Random();
    
    public static void renderBackground(DrawContext context, int width, int height) {
        context.fillGradient(0, 0, width, height / 2, COL_Top(), COL_Mid());
        context.fillGradient(0, height / 2, width, height, COL_Mid(), COL_Bot());
        
        renderParticles(context, width, height);
    }
    
    private static int COL_Top() { return 0xFF000000; }
    private static int COL_Mid() { return 0xFFFF73EC; }
    private static int COL_Bot() { return 0xFF2A1A2E; }

    private static void renderParticles(DrawContext context, int width, int height) {
        if (particles.size() < 50 && random.nextInt(10) == 0) {
            particles.add(new Particle(
                random.nextInt(width),
                height + 10,
                (random.nextFloat() - 0.5f) * 0.5f,
                -random.nextFloat() * 2.0f - 0.5f
            ));
        }
        
        particles.removeIf(p -> {
            p.x += p.vx;
            p.y += p.vy;
            return p.y < -10;
        });
        
        for (Particle p : particles) {
            int color = 0xFFFF00FF; 
            context.fill((int)p.x, (int)p.y, (int)p.x + 2, (int)p.y + 2, color);
        }
    }
    
    private static class Particle {
        float x, y, vx, vy;
        Particle(float x, float y, float vx, float vy) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        }
    }

    public static void renderAnimatedBorder(DrawContext context, int x, int y, int width, int height, float borderAnimation, float alpha) {
        int w2 = width - 2;
        int h2 = height - 2;
        int perimeter = 2 * w2 + 2 * h2;
        
        if (perimeter <= 0) return;

        for (int i = 0; i < perimeter; ++i) {
            float progress = (borderAnimation + (float)i / (float)perimeter) % 1.0F;
            int color = lerpBorderColor(progress, alpha);
            
            int drawX, drawY;
            
            if (i < w2) {
                drawX = x + i;
                drawY = y;
            } else if (i < w2 + h2) {
                drawX = x + w2;
                drawY = y + (i - w2);
            } else if (i < 2 * w2 + h2) {
                drawX = x + w2 - (i - (w2 + h2));
                drawY = y + h2;
            } else {
                drawX = x;
                drawY = y + h2 - (i - (2 * w2 + h2));
            }

            context.fill(drawX, drawY, drawX + 2, drawY + 2, color);
        }
    }

    private static int lerpBorderColor(float progress, float alpha) {
        float sine = (float)Math.sin((double)progress * Math.PI * 2.0D) * 0.5F + 0.5F;
        sine = sine * alpha;
        
        int r = net.minecraft.util.math.MathHelper.lerp(sine, 255, 255);
        int g = net.minecraft.util.math.MathHelper.lerp(sine, 60, 255);
        int b = net.minecraft.util.math.MathHelper.lerp(sine, 200, 255);
        
        return (int)(alpha * 255.0F) << 24 | r << 16 | g << 8 | b;
    }
}
