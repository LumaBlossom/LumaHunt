package luma.hunt.client.screen;

import luma.hunt.client.ui.LumaParticleSystem;
import luma.hunt.client.ui.ModernButton;
import luma.hunt.client.ui.ModernTextField;
import luma.hunt.lobby.LobbyManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerAddress;
/*? >=1.20.2 {*/
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
/*? } else {*/
import net.minecraft.client.gui.screen.ConnectScreen;
/*? }*/
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class JoinGameScreen extends AbstractLumaHuntScreen {
    private ModernTextField codeField;

    public JoinGameScreen(Screen parent) {
        super(Text.literal(""), parent);
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 220;
        int buttonHeight = 28;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 4 + 48;
        int gap = 35;

        this.codeField = new ModernTextField(this.textRenderer, 
            centerX, startY, buttonWidth, buttonHeight, 
            Text.translatable("lumahunt.screen.join.placeholder"));
        this.codeField.setMaxLength(5);
        this.codeField.setPlaceholder(Text.translatable("lumahunt.screen.join.placeholder"));
        this.addDrawableChild(codeField);

        this.addDrawableChild(ModernButton.primary(centerX, startY + gap, buttonWidth, buttonHeight,
            Text.translatable("lumahunt.screen.join.action"), button -> {
                String code = codeField.getText();
                if (code.length() >= 3) {
                     LobbyManager.getInstance().joinLobby(code);
                }
            }));

        this.addDrawableChild(ModernButton.secondary(centerX, this.height - 40, buttonWidth, buttonHeight,
            Text.translatable("lumahunt.screen.join.back"), button -> this.close()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF000000);
        renderPinkGradient(context);
        LumaParticleSystem.getInstance().update();
        LumaParticleSystem.getInstance().render(context);
        
        drawTitle(context);
        
        context.drawCenteredTextWithShadow(this.textRenderer, "Enter 5-digit Code", this.width / 2, this.height / 4 + 30, 0xFFAAAAAA);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void drawTitle(DrawContext context) {
        String title = "ʟᴜᴍᴀʜᴜɴᴛ";
        int titleX = this.width / 2;
        int titleY = 30;
        
        context.drawCenteredTextWithShadow(this.textRenderer, title, titleX, titleY, 0xFFFF69B4);
        context.drawCenteredTextWithShadow(this.textRenderer, "Join Game", titleX, titleY + 15, 0xFFAAAAAA);
    }

    public static void connectTo(String ip, int port) {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerAddress address = new ServerAddress(ip, port);
        ServerInfo info = new ServerInfo("LH | Lobby", ip + ":" + port, false);
        /*? <1.20.2 {*/
        ConnectScreen.connect(client.currentScreen, client, address, info);
        /*? } else {*/
        ConnectScreen.connect(client.currentScreen, client, address, info, false);
        /*? }*/
    }
}
