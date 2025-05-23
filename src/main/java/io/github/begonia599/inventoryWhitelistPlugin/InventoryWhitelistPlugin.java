package io.github.begonia599.inventoryWhitelistPlugin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.inventories.MultiverseInventoriesApi;
import org.mvplugins.multiverse.inventories.profile.data.PlayerProfile;
import org.mvplugins.multiverse.inventories.profile.data.ProfileData;
import org.mvplugins.multiverse.inventories.share.Sharable;
import org.mvplugins.multiverse.inventories.share.SharableHandler;

import org.mvplugins.multiverse.inventories.MultiverseInventories;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroupManager;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroup;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class InventoryWhitelistPlugin extends JavaPlugin implements Listener {

    private MultiverseInventoriesApi inventoriesApi;
    private MultiverseInventories multiverseInventories;
    private Map<String, List<Material>> customGroups;
    private Map<String, Sharable<ItemStack[]>> customSharables;

    @Override
    public void onEnable() {
        try {
            this.inventoriesApi = MultiverseInventoriesApi.get();
            this.multiverseInventories = (MultiverseInventories) getServer().getPluginManager().getPlugin("Multiverse-Inventories");
            if (this.multiverseInventories == null) {
                getLogger().severe("Multiverse-Inventories plugin not found! Plugin will be disabled.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } catch (IllegalStateException e) {
            getLogger().severe("Multiverse-Inventories API not initialized! Plugin will be disabled. Error: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("InventoryWhitelistPlugin has been enabled!");
        getLogger().info("Multiverse-Inventories API successfully hooked!");

        this.saveDefaultConfig();
        this.customGroups = new HashMap<>();
        this.customSharables = new HashMap<>();
        this.loadCustomGroups();

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Registered PlayerChangedWorldEvent listener.");

        Objects.requireNonNull(this.getCommand("mvi")).setExecutor(new CustomWhitelistCommand(this));
        Objects.requireNonNull(this.getCommand("mvi")).setTabCompleter(new CustomWhitelistTabCompleter(this));
        getLogger().info("Registered command 'mvi'.");
    }

    @Override
    public void onDisable() {
        getLogger().info("InventoryWhitelistPlugin has been disabled!");
    }

    public MultiverseInventoriesApi getInventoriesApi() {
        return inventoriesApi;
    }

    public MultiverseInventories getMultiverseInventories() {
        return multiverseInventories;
    }

    public Map<String, List<Material>> getCustomGroups() {
        return customGroups;
    }

    public Map<String, Sharable<ItemStack[]>> getCustomSharables() {
        return customSharables;
    }

    public void reloadCustomGroups() {
        loadCustomGroups();
    }

    public void saveCustomGroups() {
        getConfig().set("custom-groups", null);
        for (Map.Entry<String, List<Material>> entry : customGroups.entrySet()) {
            getConfig().set("custom-groups." + entry.getKey(), entry.getValue().stream()
                    .map(m -> "minecraft:" + m.name().toLowerCase())
                    .collect(Collectors.toList()));
        }
        saveConfig();
        getLogger().info("Saved " + customGroups.size() + " custom inventory whitelist groups.");
    }

    private void loadCustomGroups() {
        customGroups.clear();
        customSharables.clear();

        if (getConfig().getConfigurationSection("custom-groups") == null) {
            getLogger().warning("config.yml 中未找到 'custom-groups' 配置节，将跳过加载自定义组。");
            return;
        }

        for (String groupName : getConfig().getConfigurationSection("custom-groups").getKeys(false)) {
            List<String> items = getConfig().getStringList("custom-groups." + groupName);
            List<Material> materials = items.stream()
                    .map(this::parseItemName)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            customGroups.put(groupName, materials);

            Sharable<ItemStack[]> sharable = new Sharable.Builder<>(
                    "custom." + groupName,
                    ItemStack[].class,
                    new CustomSharableHandler(groupName, this)
            ).build();

            customSharables.put(groupName, sharable);
            getLogger().info("Created custom sharable: custom." + groupName);
        }
        getLogger().info("Loaded " + customGroups.size() + " custom inventory whitelist groups.");
    }

    private Material parseItemName(String itemName) {
        String formatted = itemName.startsWith("minecraft:") ? itemName : "minecraft:" + itemName.toLowerCase();
        return Material.matchMaterial(formatted);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("inventorywhitelist.bypass")) {
            player.sendMessage(getConfig().getString("messages.bypass", "你已绕过白名单限制！"));
            return;
        }

        WorldGroupManager groupManager = this.inventoriesApi.getWorldGroupManager();

        List<WorldGroup> worldGroups = groupManager.getGroupsForWorld(player.getWorld().getName());

        if (worldGroups == null || worldGroups.isEmpty()) {
            player.sendMessage(getConfig().getString("messages.no-group", "目标世界未分组，请检查配置！"));
            return;
        }

        for (WorldGroup currentGroup : worldGroups) {
            for (Map.Entry<String, Sharable<ItemStack[]>> entry : customSharables.entrySet()) {
                String groupName = entry.getKey();
                Sharable<ItemStack[]> sharable = entry.getValue();

                if (currentGroup.getShares().isSharing(sharable)) {
                    List<Material> whitelist = customGroups.get(groupName);
                    if (whitelist != null && !whitelist.isEmpty()) {
                        PlayerInventory inventory = player.getInventory();
                        ItemStack[] contents = inventory.getContents();
                        List<ItemStack> filteredItems = new ArrayList<>(); 
                        List<ItemStack> itemsToRemove = new ArrayList<>();

                        for (ItemStack item : contents) {
                            if (item != null) {
                                if (whitelist.contains(item.getType())) {
                                    filteredItems.add(item); 
                                } else {
                                    itemsToRemove.add(item); 
                                }
                            }
                        }

                        if (!itemsToRemove.isEmpty()) {
                            inventory.clear(); 
                            inventory.setContents(filteredItems.toArray(new ItemStack[0])); 
                            player.updateInventory();
                            player.sendMessage(getConfig().getString("messages.cleared", "已清除非白名单物品！"));
                            return;
                        }                       
                    }
                }
            }
        }
    }

    static class CustomSharableHandler implements SharableHandler<ItemStack[]> {
        private final String groupName;
        private InventoryWhitelistPlugin plugin;

        public CustomSharableHandler(String groupName, InventoryWhitelistPlugin plugin) {
            this.groupName = groupName;
            this.plugin = plugin;
        }

        @Override
        public void updateProfile(
                @NotNull ProfileData profileData,
                @NotNull Player player) {

            PlayerProfile profile = (PlayerProfile) profileData;
            Sharable<ItemStack[]> currentSharable = plugin.customSharables.get(groupName);

            List<Material> whitelist = plugin.customGroups.get(groupName);
            if (whitelist == null) {
                profile.set(currentSharable, new ItemStack[0]);
                return;
            }

            PlayerInventory inventory = player.getInventory();
            ItemStack[] contents = inventory.getContents();
            List<ItemStack> filtered = new ArrayList<>();
            for (ItemStack item : contents) {
                if (item != null && whitelist.contains(item.getType())) {
                    filtered.add(item);
                }
            }
            profile.set(currentSharable, filtered.toArray(new ItemStack[0]));
        }

        @Override
        public boolean updatePlayer(
                @NotNull Player player,
                @NotNull ProfileData profileData) {

            PlayerProfile profile = (PlayerProfile) profileData;
            Sharable<ItemStack[]> currentSharable = plugin.customSharables.get(groupName);

            ItemStack[] value = profile.get(currentSharable);

            if (value == null) {
                player.getInventory().clear();
                player.updateInventory();
                return false;
            }
            player.getInventory().clear();
            player.getInventory().setContents(value);
            player.updateInventory();
            return true;
        }
    }
}