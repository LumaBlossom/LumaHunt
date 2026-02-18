package luma.hunt.client.ui;

import luma.hunt.logic.Role;
import luma.hunt.network.NetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.util.UUID;

public class RoleDropdown {
    private static RoleDropdown activeDropdown = null;
    
    private final int x, y;
    private final UUID targetPlayer;
    private final int width = 100;
    private final int itemHeight = 18;
    private int hoveredIndex = -1;
    
    private static final Role[] OPTIONS = { Role.NOBODY, Role.RUNNER, Role.HUNTER };
    private static final String[] LABELS = { "♪ Spectator", "☄ Runner", "⚔ Hunter" };
    private static final int[] COLORS = { 0xFF888888, 0xFF00FF00, 0xFFFF0000 };
    
    private RoleDropdown(int x, int y, UUID targetPlayer) {
        this.x = x;
        this.y = y;
        this.targetPlayer = targetPlayer;
    }
    
    public static void show(int x, int y, UUID targetPlayer) {
        activeDropdown = new RoleDropdown(x, y, targetPlayer);
    }
    
    public static void hide() {
        activeDropdown = null;
    }
    
    public static boolean isActive() {
        return activeDropdown != null;
    }
    
    public static void render(DrawContext context, int mouseX, int mouseY) {
        if (activeDropdown != null) {
            activeDropdown.renderDropdown(context, mouseX, mouseY);
        }
    }
    
    public static boolean handleClick(int mouseX, int mouseY, int button) {
        if (activeDropdown != null) {
            return activeDropdown.onClick(mouseX, mouseY, button);
        }
        return false;
    }
    
    private void renderDropdown(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300.0F);
        
        int totalHeight = OPTIONS.length * itemHeight;
        
        context.fill(x, y, x + width, y + totalHeight, 0xF0222222);
        
        context.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
        context.fill(x, y + totalHeight - 1, x + width, y + totalHeight, 0xFFFFFFFF);
        context.fill(x, y, x + 1, y + totalHeight, 0xFFFFFFFF);
        context.fill(x + width - 1, y, x + width, y + totalHeight, 0xFFFFFFFF);
        
        hoveredIndex = -1;
        for (int i = 0; i < OPTIONS.length; i++) {
            int itemY = y + i * itemHeight;
            boolean hovered = mouseX >= x && mouseX < x + width && 
                             mouseY >= itemY && mouseY < itemY + itemHeight;
            
            if (hovered) {
                hoveredIndex = i;
                context.fill(x + 1, itemY, x + width - 1, itemY + itemHeight, 0x40FFFFFF);
            }
            
            context.fill(x + 8, itemY + itemHeight/2 - 2, x + 12, itemY + itemHeight/2 + 2, COLORS[i]);
            context.drawTextWithShadow(client.textRenderer, LABELS[i], x + 18, itemY + 5, 0xFFFFFFFF);
        }
        
        context.getMatrices().pop();
    }
    
    private boolean onClick(int mouseX, int mouseY, int button) {
        int totalHeight = OPTIONS.length * itemHeight;
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + totalHeight) {
            hide();
            return false;
        }
        
        for (int i = 0; i < OPTIONS.length; i++) {
            int itemY = y + i * itemHeight;
            if (mouseY >= itemY && mouseY < itemY + itemHeight) {
                setPlayerRole(OPTIONS[i]);
                MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
                hide();
                return true;
            }
        }
        
        return false;
    }
    
    private void setPlayerRole(Role newRole) {
        NetworkHandler.sendChangeRole(targetPlayer, newRole);
    }
}
