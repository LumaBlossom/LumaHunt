package luma.hunt.logic;

import luma.hunt.LumaHunt;
import luma.hunt.lobby.LobbyManager;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class HunterTracker {
    private static HunterTracker instance;
    private final Map<UUID, UUID> hunterTargets = new HashMap<>();
    private final List<UUID> runnerList = new ArrayList<>();

    private final Map<UUID, BlockPos> lastKnownPositions = new HashMap<>();

    private static final int UPDATE_DISTANCE_THRESHOLD = 5;

    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL_TICKS = 20;
    
    private HunterTracker() {}
    
    public static HunterTracker getInstance() {
        if (instance == null) {
            instance = new HunterTracker();
        }
        return instance;
    }
    
    public void initializeTracking(MinecraftServer server) {
        runnerList.clear();
        hunterTargets.clear();
        lastKnownPositions.clear();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Role role = LobbyManager.getInstance().getPlayerRole(player.getUuid());
            if (role == Role.RUNNER) {
                runnerList.add(player.getUuid());
            }
        }
        
        if (runnerList.isEmpty()) {
            LumaHunt.LOGGER.warn("No runners found for tracking!");
            return;
        }

        int runnerIndex = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Role role = LobbyManager.getInstance().getPlayerRole(player.getUuid());
            if (role == Role.HUNTER) {
                UUID targetRunner = runnerList.get(runnerIndex % runnerList.size());
                hunterTargets.put(player.getUuid(), targetRunner);
                runnerIndex++;
                
                giveTrackingCompass(player, targetRunner, server);
            }
        }
        
        LumaHunt.LOGGER.info("Hunter tracking initialized with " + runnerList.size() + " runners");
    }
    
    public void giveCompassToHunter(ServerPlayerEntity hunter, MinecraftServer server) {
        UUID targetId = hunterTargets.get(hunter.getUuid());
        if (targetId != null) {
            giveTrackingCompass(hunter, targetId, server);
        } else if (!runnerList.isEmpty()) {
            UUID targetRunner = runnerList.get(0);
            hunterTargets.put(hunter.getUuid(), targetRunner);
            giveTrackingCompass(hunter, targetRunner, server);
        }
    }
    
    public void cycleTarget(ServerPlayerEntity hunter, MinecraftServer server) {
        if (runnerList.isEmpty()) return;
        
        UUID currentTarget = hunterTargets.get(hunter.getUuid());
        int currentIndex = currentTarget != null ? runnerList.indexOf(currentTarget) : -1;
        int nextIndex = (currentIndex + 1) % runnerList.size();
        
        UUID newTarget = runnerList.get(nextIndex);
        hunterTargets.put(hunter.getUuid(), newTarget);

        lastKnownPositions.remove(newTarget);
        
        giveTrackingCompass(hunter, newTarget, server);

        ServerPlayerEntity runner = server.getPlayerManager().getPlayer(newTarget);
        String runnerName = runner != null ? runner.getName().getString() : "Unknown";
        hunter.sendMessage(Text.translatable("lumahunt.tracker.tracking", runnerName), true);
    }

    public void tick(MinecraftServer server) {
        tickCounter++;
        
        if (tickCounter < UPDATE_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        
        for (Map.Entry<UUID, UUID> entry : hunterTargets.entrySet()) {
            ServerPlayerEntity hunter = server.getPlayerManager().getPlayer(entry.getKey());
            ServerPlayerEntity runner = server.getPlayerManager().getPlayer(entry.getValue());
            
            if (hunter != null && runner != null) {
                updateCompassTarget(hunter, runner);
            }
        }
    }
    
    private int getDistanceSquared(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        int dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
    
    private void giveTrackingCompass(ServerPlayerEntity hunter, UUID targetId, MinecraftServer server) {
        ServerPlayerEntity runner = server.getPlayerManager().getPlayer(targetId);
        String runnerName = runner != null ? runner.getName().getString() : "Runner";

        ItemStack compass = new ItemStack(Items.COMPASS);

        compass.addEnchantment(Enchantments.VANISHING_CURSE, 1);
        
        NbtCompound nbt = compass.getOrCreateNbt();
        
        if (runner != null) {
            NbtCompound lodestoneNbt = new NbtCompound();
            lodestoneNbt.putString("dimension", runner.getWorld().getRegistryKey().getValue().toString());
            lodestoneNbt.put("pos", NbtHelper.fromBlockPos(runner.getBlockPos()));
            nbt.put("LodestonePos", lodestoneNbt.getCompound("pos"));
            nbt.putString("LodestoneDimension", lodestoneNbt.getString("dimension"));
            nbt.putBoolean("LodestoneTracked", false);

            lastKnownPositions.put(targetId, runner.getBlockPos());
        }
        compass.setCustomName(Text.translatable("lumahunt.tracker.item_name", runnerName));

        for (int i = 0; i < hunter.getInventory().size(); i++) {
            ItemStack stack = hunter.getInventory().getStack(i);
            if (isTrackerCompass(stack)) {
                hunter.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }

        hunter.giveItemStack(compass);
        hunter.sendMessage(Text.translatable("lumahunt.tracker.tracking", runnerName), true);
    }
    
    private void updateCompassTarget(ServerPlayerEntity hunter, ServerPlayerEntity runner) {
        for (int i = 0; i < hunter.getInventory().size(); i++) {
            ItemStack stack = hunter.getInventory().getStack(i);
            if (isTrackerCompass(stack)) {
                NbtCompound nbt = stack.getOrCreateNbt();
                nbt.put("LodestonePos", NbtHelper.fromBlockPos(runner.getBlockPos()));
                nbt.putString("LodestoneDimension", runner.getWorld().getRegistryKey().getValue().toString());
                nbt.putBoolean("LodestoneTracked", false);
                break;
            }
        }
    }
    
    private boolean isTrackerCompass(ItemStack stack) {
        return stack.getItem() == Items.COMPASS && 
               stack.hasCustomName() && 
               stack.getName().getString().startsWith("§dTracker:");
    }
    
    public void reset() {
        hunterTargets.clear();
        runnerList.clear();
        lastKnownPositions.clear();
        tickCounter = 0;
    }
    
    public UUID getHunterTarget(UUID hunterUuid) {
        return hunterTargets.get(hunterUuid);
    }
}
