// CustomWhitelistCommand.java
package io.github.begonia599.inventoryWhitelistPlugin;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.inventories.share.Sharable;
import org.mvplugins.multiverse.inventories.share.Sharable.Builder; 

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CustomWhitelistCommand implements CommandExecutor {

    private final InventoryWhitelistPlugin plugin;

    public CustomWhitelistCommand(InventoryWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("inventorywhitelist.custom")) {
            sender.sendMessage(plugin.getConfig().getString("messages.no-permission", "你没有权限执行此命令！"));
            return true;
        }

        if (args.length < 1) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreateCommand(sender, args);
            case "add":
                return handleAddCommand(sender, args);
            case "remove":
                return handleRemoveCommand(sender, args);
            case "list":
                return handleListCommand(sender, args);
            case "delete":
                return handleDeleteCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§b--- InventoryWhitelist 帮助 ---");
        sender.sendMessage("§e/mvi create <组名> §7- 创建一个新的白名单组");
        sender.sendMessage("§e/mvi add <组名> [物品名称|hand] §7- 向组中添加物品 (输入物品名称或手持物品)");
        sender.sendMessage("§e/mvi remove <组名> [物品名称|hand] §7- 从组中移除物品 (输入物品名称或手持物品)");
        sender.sendMessage("§e/mvi list [组名] §7- 列出所有组或指定组的物品");
        sender.sendMessage("§e/mvi delete <组名> §7- 删除一个白名单组");
        sender.sendMessage("§e/mvi reload §7- 重新加载插件配置");
        sender.sendMessage("§b----------------------------");
    }

    private boolean handleCreateCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /mvi create <组名>");
            return true;
        }
        String groupName = args[1].toLowerCase();
        Map<String, List<Material>> customGroups = plugin.getCustomGroups();
        Map<String, Sharable<ItemStack[]>> customSharables = plugin.getCustomSharables();

        if (customGroups.containsKey(groupName)) {
            sender.sendMessage("§c错误: 白名单组 '" + groupName + "' 已存在！");
            return true;
        }

        customGroups.put(groupName, new ArrayList<>());

        Sharable<ItemStack[]> sharable = new Sharable.Builder<>(
                "custom." + groupName,
                ItemStack[].class,
                new InventoryWhitelistPlugin.CustomSharableHandler(groupName, plugin)
        ).build();
        customSharables.put(groupName, sharable);

        plugin.saveCustomGroups();
        sender.sendMessage("§a成功创建白名单组: '" + groupName + "'.");
        sender.sendMessage("§e请手动编辑 Multiverse-Inventories 的 groups.yml，将 'custom." + groupName + "' 添加到目标世界组的 'shares' 列表中。");
        return true;
    }

    private boolean handleAddCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /mvi add <组名> [物品名称|hand]");
            return true;
        }
        String groupName = args[1].toLowerCase();
        String itemIdentifier = args[2].toLowerCase();
        Map<String, List<Material>> customGroups = plugin.getCustomGroups();

        if (!customGroups.containsKey(groupName)) {
            sender.sendMessage("§c错误: 白名单组 '" + groupName + "' 不存在！");
            return true;
        }

        Material material = null;
        if ("hand".equalsIgnoreCase(itemIdentifier)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c错误: 只有玩家才能使用 'hand' 选项。");
                return true;
            }
            Player player = (Player) sender;
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType() == Material.AIR) {
                sender.sendMessage("§c错误: 你手中没有物品！");
                return true;
            }
            material = itemInHand.getType();
        } else {
            material = Material.matchMaterial(itemIdentifier);
            if (material == null) {
                sender.sendMessage("§c错误: 无效的物品名称 '" + itemIdentifier + "'。");
                return true;
            }
        }

        List<Material> groupItems = customGroups.get(groupName);
        if (groupItems.contains(material)) {
            sender.sendMessage("§c物品 '" + material.name().toLowerCase() + "' 已经存在于组 '" + groupName + "' 中。");
            return true;
        }

        groupItems.add(material);
        plugin.saveCustomGroups();
        sender.sendMessage("§a成功将 '" + material.name().toLowerCase() + "' 添加到组 '" + groupName + "'.");
        return true;
    }

    private boolean handleRemoveCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /mvi remove <组名> [物品名称|hand]");
            return true;
        }
        String groupName = args[1].toLowerCase();
        String itemIdentifier = args[2].toLowerCase();
        Map<String, List<Material>> customGroups = plugin.getCustomGroups();

        if (!customGroups.containsKey(groupName)) {
            sender.sendMessage("§c错误: 白名单组 '" + groupName + "' 不存在！");
            return true;
        }

        Material material = null;
        if ("hand".equalsIgnoreCase(itemIdentifier)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c错误: 只有玩家才能使用 'hand' 选项。");
                return true;
            }
            Player player = (Player) sender;
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType() == Material.AIR) {
                sender.sendMessage("§c错误: 你手中没有物品！");
                return true;
            }
            material = itemInHand.getType();
        } else {
            material = Material.matchMaterial(itemIdentifier);
            if (material == null) {
                sender.sendMessage("§c错误: 无效的物品名称 '" + itemIdentifier + "'。");
                return true;
            }
        }

        List<Material> groupItems = customGroups.get(groupName);
        if (!groupItems.contains(material)) {
            sender.sendMessage("§c物品 '" + material.name().toLowerCase() + "' 不存在于组 '" + groupName + "' 中。");
            return true;
        }

        groupItems.remove(material);
        plugin.saveCustomGroups();
        sender.sendMessage("§a成功将 '" + material.name().toLowerCase() + "' 从组 '" + groupName + "' 移除。");
        return true;
    }

    private boolean handleListCommand(CommandSender sender, String[] args) {
        Map<String, List<Material>> customGroups = plugin.getCustomGroups();

        if (args.length == 1) {
            if (customGroups.isEmpty()) {
                sender.sendMessage("§e目前没有创建任何白名单组。");
                return true;
            }
            sender.sendMessage("§b--- 已创建的白名单组 ---");
            customGroups.keySet().forEach(groupName -> sender.sendMessage("§a- " + groupName));
            sender.sendMessage("§b-----------------------");
        } else if (args.length == 2) { 
            String groupName = args[1].toLowerCase();
            if (!customGroups.containsKey(groupName)) {
                sender.sendMessage("§c错误: 白名单组 '" + groupName + "' 不存在！");
                return true;
            }
            List<Material> groupItems = customGroups.get(groupName);
            if (groupItems.isEmpty()) {
                sender.sendMessage("§e组 '" + groupName + "' 中没有物品。");
                return true;
            }
            sender.sendMessage("§b--- 组 '" + groupName + "' 中的物品 ---");
            groupItems.stream()
                    .map(Material::name)
                    .map(String::toLowerCase)
                    .forEach(itemName -> sender.sendMessage("§a- " + itemName));
            sender.sendMessage("§b---------------------------");
        } else {
            sender.sendMessage("§c用法: /mvi list [组名]");
        }
        return true;
    }

    private boolean handleDeleteCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /mvi delete <组名>");
            return true;
        }
        String groupName = args[1].toLowerCase();
        Map<String, List<Material>> customGroups = plugin.getCustomGroups();
        Map<String, Sharable<ItemStack[]>> customSharables = plugin.getCustomSharables();

        if (!customGroups.containsKey(groupName)) {
            sender.sendMessage("§c错误: 白名单组 '" + groupName + "' 不存在！");
            return true;
        }

        customGroups.remove(groupName);
        customSharables.remove("custom." + groupName); 
        plugin.saveCustomGroups(); 
        sender.sendMessage("§a成功删除白名单组: '" + groupName + "'.");
        sender.sendMessage("§e请手动编辑 Multiverse-Inventories 的 groups.yml，移除 'custom." + groupName + "'。");
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        plugin.reloadConfig();
        plugin.reloadCustomGroups(); 
        sender.sendMessage("§aInventoryWhitelistPlugin 配置已重新加载！");
        return true;
    }
}