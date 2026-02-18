package luma.hunt.client.screen;

import luma.hunt.client.ui.ModernButton;
import luma.hunt.config.ClientConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;

public class WelcomeScreen extends AbstractLumaHuntScreen {
    
    private final Screen parent;
    
    public WelcomeScreen(Screen parent) {
        super(Text.literal("Welcome"), parent);
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 100;
        int buttonHeight = 20;
        int spacing = 10;
        int bottomY = this.height - 40;
        
        int totalWidth = buttonWidth * 5 + spacing * 4;
        int startX = (this.width - totalWidth) / 2;
        
        // Close
        this.addDrawableChild(ModernButton.secondary(startX, bottomY, buttonWidth, buttonHeight, 
            Text.literal("Close"), btn -> this.close()));
            
        // Discord
        this.addDrawableChild(ModernButton.create(startX + (buttonWidth + spacing), bottomY, buttonWidth, buttonHeight, 
            Text.literal("Discord"), btn -> openLink("https://discord.gg/lumahunt")));
            
        // Issues
        this.addDrawableChild(ModernButton.create(startX + (buttonWidth + spacing) * 2, bottomY, buttonWidth, buttonHeight, 
            Text.literal("Issues"), btn -> openLink("https://github.com/LumaHunt/LumaHunt/issues")));
            
        // Ko-fi
        this.addDrawableChild(ModernButton.create(startX + (buttonWidth + spacing) * 3, bottomY, buttonWidth, buttonHeight, 
            Text.literal("Ko-fi"), btn -> openLink("https://ko-fi.com/lumahunt")));
            
        // Boosty
        this.addDrawableChild(ModernButton.create(startX + (buttonWidth + spacing) * 4, bottomY, buttonWidth, buttonHeight, 
            Text.literal("Boosty"), btn -> openLink("https://boosty.to/lumahunt")));
    }
    
    private void openLink(String url) {
        try {
            Util.getOperatingSystem().open(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void close() {
        ClientConfig.getInstance().hasSeenWelcomeScreen = true;
        ClientConfig.getInstance().save();
        this.client.setScreen(this.parent);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Darken background
        this.renderBackground(context);
        context.fill(0, 0, this.width, this.height, 0xAA000000);
        
        // Banner dimensions
        int bannerWidth = Math.min(600, this.width - 40);
        int bannerHeight = Math.min(400, this.height - 80);
        int bannerX = (this.width - bannerWidth) / 2;
        int bannerY = (this.height - bannerHeight) / 2 - 20;
        
        // Banner background (gradient or image)
        context.fillGradient(bannerX, bannerY, bannerX + bannerWidth, bannerY + bannerHeight, 0xFF2A0A1A, 0xFF1A0A12);
        LumaGuiUtils.renderAnimatedBorder(context, bannerX, bannerY, bannerWidth, bannerHeight, (System.currentTimeMillis() % 2000) / 2000f, 1.0f);
        
        // Content
        int textY = bannerY + 20;
        
        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, "§d§lWelcome to LUMAHUNT", this.width / 2, textY, 0xFFFFFF);
        textY += 40;
        
        // Steps
        drawStep(context, "1. How to Host World:", textY, 0xFF69B4);
        textY += 15;
        drawStep(context, "- Click 'Host Game' in the menu.", textY, 0xAAAAAA);
        textY += 12;
        drawStep(context, "- This creates a new world and lobby instantly.", textY, 0xAAAAAA);
        textY += 12;
        drawStep(context, "- Copy the code and send it to friends.", textY, 0xAAAAAA);
        
        textY += 30;
        
        drawStep(context, "2. How to Join as a Friend:", textY, 0xFF69B4);
        textY += 15;
        drawStep(context, "- Click 'Join Game' in the menu.", textY, 0xAAAAAA);
        textY += 12;
        drawStep(context, "- Enter the 5-digit code provided by the host.", textY, 0xAAAAAA);
        textY += 12;
        drawStep(context, "- Wait for the host to start.", textY, 0xAAAAAA);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void drawStep(DrawContext context, String text, int y, int color) {
        int bannerWidth = Math.min(600, this.width - 40);
        int bannerX = (this.width - bannerWidth) / 2;
        context.drawTextWithShadow(this.textRenderer, text, bannerX + 40, y, color);
    }
}
