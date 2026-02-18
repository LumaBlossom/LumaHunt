package luma.hunt.client.screen;

import luma.hunt.client.hud.TimerHud;
import luma.hunt.client.ui.LumaParticleSystem;
import luma.hunt.client.ui.ModernButton;
import luma.hunt.client.ui.PlayerSkinWidget;
import luma.hunt.client.ui.RoleDropdown;
import luma.hunt.lobby.LobbyManager;
import luma.hunt.logic.Role;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class LobbyScreen extends AbstractLumaHuntScreen {
    private final boolean isHost;
    private boolean codeRevealed = false;
    private String lobbyCode;
    
    private final List<PlayerSkinWidget> runnerWidgets = new ArrayList<>();
    private final List<PlayerSkinWidget> hunterWidgets = new ArrayList<>();
    private final List<PlayerSkinWidget> nobodyWidgets = new ArrayList<>();

    public LobbyScreen(Screen parent, boolean isHost) {
        super(Text.translatable("lumahunt.screen.lobby.title"), parent);
        this.isHost = isHost;
        this.lobbyCode = LobbyManager.getInstance().getCurrentLobbyCode();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void close() {}

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int bottomY = this.height - 30;
        int btnWidth = 120;
        int btnHeight = 20;
        int btnSpacing = 10;

        if (isHost) {
            this.addDrawableChild(ModernButton.create(centerX - 10, 30, 90, 20, 
                Text.translatable("lumahunt.screen.lobby.show"), button -> {
                    this.codeRevealed = !this.codeRevealed;
                    button.setMessage(Text.translatable(codeRevealed ? "lumahunt.screen.lobby.hide" : "lumahunt.screen.lobby.show"));

                }));
                
            this.addDrawableChild(ModernButton.create(centerX + 85, 30, 50, 20,
                Text.translatable("lumahunt.screen.lobby.copy"), button -> {
                    if (this.lobbyCode != null) {
                        this.client.keyboard.setClipboard(this.lobbyCode);
                    }
                }));
            
            int totalWidth = btnWidth * 3 + btnSpacing * 2;
            int startX = centerX - totalWidth / 2;
            
            this.addDrawableChild(ModernButton.create(startX, bottomY, btnWidth, btnHeight,
                Text.translatable("lumahunt.screen.lobby.settings"), button -> {
                    this.client.setScreen(new SettingsScreen(this));
                }));
            
            this.addDrawableChild(ModernButton.success(startX + btnWidth + btnSpacing, bottomY, btnWidth, btnHeight,
                Text.translatable("lumahunt.screen.lobby.start"), button -> {
                    LobbyManager.getInstance().startGame();
                    TimerHud.initializeGameStart();
                    this.client.setScreen(null);
                }));
            
            this.addDrawableChild(ModernButton.danger(startX + (btnWidth + btnSpacing) * 2, bottomY, btnWidth, btnHeight,
                Text.translatable("lumahunt.screen.lobby.close"), button -> {
                    LobbyManager.getInstance().deleteLobby();
                    this.client.setScreen(this.parent);
                }));
        } else {
            this.addDrawableChild(ModernButton.create(centerX - 60, bottomY, 120, btnHeight,
                Text.translatable("lumahunt.screen.lobby.leave"), button -> {
                    this.client.disconnect();
                    this.client.setScreen(this.parent);
                }));
        }
        
        rebuildPlayerWidgets();
    }
    
    private void rebuildPlayerWidgets() {
        runnerWidgets.clear();
        hunterWidgets.clear();
        nobodyWidgets.clear();
        
        if (this.client.getNetworkHandler() == null) return;
        
        List<PlayerListEntry> runners = new ArrayList<>();
        List<PlayerListEntry> hunters = new ArrayList<>();
        List<PlayerListEntry> nobodies = new ArrayList<>();
        
        for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
            Role role = LobbyManager.getInstance().getPlayerRole(entry.getProfile().getId());
            switch (role) {
                case RUNNER -> runners.add(entry);
                case HUNTER -> hunters.add(entry);
                default -> nobodies.add(entry);
            }
        }
        
        int bigSize = 80;
        int smallSize = 40;
        int spacing = 25;
        
        // runners
        int runnersY = Math.max(80, this.height / 6);
        buildRow(runners, Role.RUNNER, runnerWidgets, runnersY, bigSize, spacing);
        
        // hunters
        int runnersRows = (runners.size() + 4) / 5;
        int runnersHeight = runnersRows > 0 ? runnersRows * bigSize + (runnersRows - 1) * spacing : 0;
        
        int gap = 60; // space for vs
        int huntersY = runnersY + runnersHeight + gap; 
        buildRow(hunters, Role.HUNTER, hunterWidgets, huntersY, bigSize, spacing);
        
        // nobodies
        int nobodyY = this.height - 30 - smallSize - 20;
        buildRow(nobodies, Role.NOBODY, nobodyWidgets, nobodyY, smallSize, spacing);
    }
    
    private void buildRow(List<PlayerListEntry> players, Role role, List<PlayerSkinWidget> widgets, 
                          int y, int size, int spacing) {
        int count = players.size();
        if (count == 0) return;
        
        int totalWidth = count * size + (count - 1) * spacing;
        int startX = (this.width - totalWidth) / 2;
        
        for (int i = 0; i < count; i++) {
            PlayerListEntry entry = players.get(i);
            int x = startX + i * (size + spacing);
            widgets.add(new PlayerSkinWidget(
                entry.getProfile().getId(),
                entry.getProfile().getName(),
                role,
                x, y, size
            ));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF000000);
        renderPinkGradient(context);
        LumaParticleSystem.getInstance().update();
        LumaParticleSystem.getInstance().render(context);
        
        context.drawCenteredTextWithShadow(this.textRenderer, "§d§lʟᴜᴍᴀʜᴜɴᴛ", this.width / 2, 10, 0xFFFFFF);
        
        if (isHost && this.lobbyCode != null) {
            String codeText = codeRevealed ? lobbyCode : "*****";
            int codeColor = codeRevealed ? 0x00FF00 : 0xAAAAAA;
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("lumahunt.screen.lobby.code", codeText), this.width / 2 - 70, 36, codeColor);
        }
        
        rebuildPlayerWidgets();
        
        if (!runnerWidgets.isEmpty() && !hunterWidgets.isEmpty()) {
             int bigSize = 80;
             int spacing = 25;
             int runnersY = Math.max(80, this.height / 6);
             
             int runnersRows = (runnerWidgets.size() + 4) / 5;
             int runnersBlockHeight = runnersRows > 0 ? runnersRows * bigSize + (runnersRows - 1) * spacing : 0;
             
             int gap = 60;
             int vsY = runnersY + runnersBlockHeight + (gap / 2) - 4; 
             
             context.drawCenteredTextWithShadow(this.textRenderer, "§f§lVS", this.width / 2, vsY, 0xFFFFFF);
        }
        
        for (PlayerSkinWidget widget : runnerWidgets) widget.render(context, mouseX, mouseY);
        for (PlayerSkinWidget widget : hunterWidgets) widget.render(context, mouseX, mouseY);
        for (PlayerSkinWidget widget : nobodyWidgets) widget.render(context, mouseX, mouseY);
        
        super.render(context, mouseX, mouseY, delta);
        
        RoleDropdown.render(context, mouseX, mouseY);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (RoleDropdown.isActive()) {
            if (RoleDropdown.handleClick((int)mouseX, (int)mouseY, button)) {
                return true;
            }
        }
        
        // host can open dropdown for players
        if (isHost && (button == 0 || button == 1)) { 
            for (PlayerSkinWidget widget : getAllWidgets()) {
                if (widget.isMouseOver((int)mouseX, (int)mouseY)) {
                    RoleDropdown.show((int)mouseX, (int)mouseY, widget.getPlayerUuid());
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private List<PlayerSkinWidget> getAllWidgets() {
        List<PlayerSkinWidget> all = new ArrayList<>();
        all.addAll(runnerWidgets);
        all.addAll(hunterWidgets);
        all.addAll(nobodyWidgets);
        return all;
    }
}

