package luma.hunt.client.screen;

import luma.hunt.client.ui.LumaParticleSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class AbstractLumaHuntScreen extends Screen {
    protected final Screen parent;

    protected AbstractLumaHuntScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        LumaParticleSystem.getInstance().updateScreenSize(this.width, this.height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // solid black base
        context.fill(0, 0, this.width, this.height, 0xFF000000);
        
        renderPinkGradient(context);
        
        LumaParticleSystem.getInstance().update();
        LumaParticleSystem.getInstance().render(context);
        
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFF69B4);
        
        super.render(context, mouseX, mouseY, delta);
    }

    protected void renderPinkGradient(DrawContext context) {
        int topColor = 0xFF000000;
        int midColor = 0xFF4A1533;
        int bottomColor = 0xFF1A0A12;
        
        context.fillGradient(0, 0, this.width, this.height / 2, topColor, midColor);
        context.fillGradient(0, this.height / 2, this.width, this.height, midColor, bottomColor);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
