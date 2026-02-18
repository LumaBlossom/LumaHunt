package luma.hunt.client.ui;

import luma.hunt.logic.Role;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.DefaultSkinHelper;

import java.util.UUID;

public class PlayerSkinWidget {
    private final int width;
    private final int height;
    
    private final UUID playerUuid;
    private final String playerName;
    private Role role;
    private final int x, y;
    private float hoverProgress = 0f;
    private final long creationTime;
    
    private final com.mojang.authlib.GameProfile gameProfile;
    
    private final net.minecraft.client.render.entity.model.PlayerEntityModel<net.minecraft.entity.LivingEntity> normalModel;
    private final net.minecraft.client.render.entity.model.PlayerEntityModel<net.minecraft.entity.LivingEntity> slimModel;
    
    public PlayerSkinWidget(UUID uuid, String name, Role role, int x, int y, int size) {
        this.playerUuid = uuid;
        this.playerName = name;
        this.role = role;
        this.x = x;
        this.y = y;
        this.width = size;
        this.height = size;

        this.creationTime = System.currentTimeMillis();
        this.gameProfile = new com.mojang.authlib.GameProfile(uuid, name);
        
        MinecraftClient client = MinecraftClient.getInstance();
        net.minecraft.client.render.entity.model.EntityModelLoader loader = client.getEntityModelLoader();
        
        this.normalModel = new net.minecraft.client.render.entity.model.PlayerEntityModel<>(
            loader.getModelPart(net.minecraft.client.render.entity.model.EntityModelLayers.PLAYER), false);
        this.slimModel = new net.minecraft.client.render.entity.model.PlayerEntityModel<>(
            loader.getModelPart(net.minecraft.client.render.entity.model.EntityModelLayers.PLAYER_SLIM), true);
        
        client.getSkinProvider().loadSkin(gameProfile, (type, id, texture) -> {}, true);
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public Role getRole() {
        return this.role;
    }
    
    public void render(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        boolean hovered = isMouseOver(mouseX, mouseY);
        
        float targetProgress = hovered ? 1f : 0f;
        float animSpeed = 0.15f;
        if (hoverProgress < targetProgress) {
            hoverProgress = Math.min(targetProgress, hoverProgress + animSpeed);
        } else if (hoverProgress > targetProgress) {
            hoverProgress = Math.max(targetProgress, hoverProgress - animSpeed);
        }
        
        int outlineColor = switch (role) {
            case RUNNER -> 0xFF00FF00;
            case HUNTER -> 0xFFFF0000;
            case NOBODY -> 0xFFAAAAAA;
        };
        
        int boxX = x;
        int boxY = y;
        int boxWidth = this.width;
        int boxHeight = this.height; 
        
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x40000000);
        
        if (hoverProgress > 0) {
            int alpha = (int)(hoverProgress * 0x40);
            int fillColor = (alpha << 24) | (outlineColor & 0x00FFFFFF);
            context.fill(boxX + 1, boxY + 1, boxX + boxWidth - 1, boxY + boxHeight - 1, fillColor);
        }
        
        Identifier texture = DefaultSkinHelper.getTexture(playerUuid);
        boolean slim = false;
        
        if (client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(playerUuid);
            if (entry != null) {
                  texture = entry.getSkinTexture();
                  slim = "slim".equals(entry.getModel());
            } else {
                  java.util.Map<com.mojang.authlib.minecraft.MinecraftProfileTexture.Type, com.mojang.authlib.minecraft.MinecraftProfileTexture> textures = client.getSkinProvider().getTextures(gameProfile);
                  if (textures.containsKey(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN)) {
                      texture = client.getSkinProvider().loadSkin(gameProfile.getProperties().containsKey("textures") ? gameProfile : client.getSession().getProfile()); 
                      slim = "slim".equals(DefaultSkinHelper.getModel(playerUuid));
                  }
            }
        }
        
        net.minecraft.client.render.entity.model.PlayerEntityModel<net.minecraft.entity.LivingEntity> model = 
            slim ? this.slimModel : this.normalModel;
            
        context.getMatrices().push();
        
        float centerX = boxX + boxWidth / 2.0f;
        float modelY = boxY + boxHeight * -0.05f; 
        float offsetX = boxWidth * 0.125f;
        context.getMatrices().translate(centerX + offsetX, modelY, 50.0F);
        
        float scale = this.height * 0.75F; 
        context.getMatrices().scale(scale, scale, scale); 
        
        float time = (System.currentTimeMillis() - this.creationTime) / 10.0F;
        float rotationY = time * 0.5F; 
        
        context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180.0F + rotationY - 45.0F));
        context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-15.0F)); 
        
        net.minecraft.client.render.RenderLayer renderLayer = net.minecraft.client.render.RenderLayer.getEntityTranslucent(texture);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, texture);
        
        float ageInSeconds = (System.currentTimeMillis() - this.creationTime) / 1000.0F;
        float swing = (float)Math.sin(ageInSeconds * 2.0F) * 0.1F; 
        
        model.leftArm.pitch = -0.62831855F + swing; 
        model.rightArm.pitch = -0.62831855F - swing;
        model.rightLeg.pitch = -1.4137167F;
        model.rightLeg.yaw = 0.31415927F;
        model.rightLeg.roll = 0.07853982F;
        model.leftLeg.pitch = -1.4137167F;
        model.leftLeg.yaw = -0.31415927F;
        model.leftLeg.roll = -0.07853982F;
        
        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;
        
        model.render(context.getMatrices(), context.getVertexConsumers().getBuffer(renderLayer), 
            15728880, net.minecraft.client.render.OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F); 
            
        context.getMatrices().pop();
        
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 100.0F);
        
        int thickness = 2;
        context.fill(boxX, boxY, boxX + boxWidth, boxY + thickness, outlineColor);
        context.fill(boxX, boxY + boxHeight - thickness, boxX + boxWidth, boxY + boxHeight, outlineColor);
        context.fill(boxX, boxY, boxX + thickness, boxY + boxHeight, outlineColor);
        context.fill(boxX + boxWidth - thickness, boxY, boxX + boxWidth, boxY + boxHeight, outlineColor);

        String displayName = playerName.length() > 10 ? playerName.substring(0, 9) + ".." : playerName;
        int nameWidth = client.textRenderer.getWidth(displayName);
        int nameX = boxX + (boxWidth - nameWidth) / 2;
        int nameY = boxY + boxHeight + 5;
        context.drawTextWithShadow(client.textRenderer, displayName, nameX, nameY, 0xFFFFFFFF);
        context.getMatrices().pop();
    }
    
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + this.width && mouseY >= y && mouseY < y + this.height;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
