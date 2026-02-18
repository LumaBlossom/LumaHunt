package luma.hunt.client.ui;

import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LumaParticleSystem {
    private static LumaParticleSystem instance;
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private int screenWidth;
    private int screenHeight;
    private static final int MAX_PARTICLES = 100;
    private static final int SPAWN_RATE = 2;
    
    private static final int[] PINK_COLORS = {
        0xFF69B4,
        0xFF1493,
        0xDB7093,
        0xC71585,
        0xFFC0CB,
        0xFFB6C1
    };

    private LumaParticleSystem() {}

    public static LumaParticleSystem getInstance() {
        if (instance == null) {
            instance = new LumaParticleSystem();
        }
        return instance;
    }

    public void updateScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void clearParticles() {
        this.particles.clear();
    }

    public void update() {
        if (screenWidth <= 0 || screenHeight <= 0) return;
        
        for (int i = 0; i < SPAWN_RATE && particles.size() < MAX_PARTICLES; i++) {
            particles.add(new Particle(
                random.nextFloat() * screenWidth,
                random.nextFloat() * screenHeight,
                (random.nextFloat() - 0.5f) * 0.5f,
                (random.nextFloat() - 0.5f) * 0.5f,
                random.nextInt(3) + 2,
                random.nextFloat() * 0.6f + 0.4f,
                PINK_COLORS[random.nextInt(PINK_COLORS.length)]
            ));
        }

        particles.removeIf(p -> {
            p.update();
            return p.x < 0 || p.x > screenWidth || p.y < 0 || p.y > screenHeight || p.alpha <= 0;
        });
    }

    public void render(DrawContext context) {
        for (Particle particle : particles) {
            particle.render(context);
        }
    }

    private static class Particle {
        float x, y, vx, vy;
        int size;
        float alpha;
        int color;

        Particle(float x, float y, float vx, float vy, int size, float alpha, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.alpha = alpha;
            this.color = color;
        }

        void update() {
            x += vx;
            y += vy;
            alpha -= 0.001f;
        }

        void render(DrawContext context) {
            int alphaInt = (int) (alpha * 180);
            if (alphaInt > 0) {
                int colorWithAlpha = (alphaInt << 24) | (color & 0x00FFFFFF);
                int x1 = (int) Math.floor(x);
                int y1 = (int) Math.floor(y);
                int x2 = (int) Math.ceil(x + size);
                int y2 = (int) Math.ceil(y + size);
                context.fill(x1, y1, x2, y2, colorWithAlpha);
            }
        }
    }
}
