package io.github.begonia599.inventoryWhitelistPlugin;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CustomWhitelistTabCompleter implements TabCompleter {

    private final InventoryWhitelistPlugin plugin;

    public CustomWhitelistTabCompleter(InventoryWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    private static final String[] COMMANDS = {"create", "add", "remove", "list", "delete", "reload"};

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("inventorywhitelist.custom")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList(COMMANDS), completions);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("add") || subCommand.equals("remove") ||
                    subCommand.equals("list") || subCommand.equals("delete")) {
                StringUtil.copyPartialMatches(args[1], plugin.getCustomGroups().keySet(), completions);
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String groupName = args[1].toLowerCase();

            if (subCommand.equals("add") || subCommand.equals("remove")) {
                // 补全物品名称或 'hand'
                List<String> materialNames = Arrays.stream(Material.values())
                        .map(Material::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                materialNames.add("hand"); 
                StringUtil.copyPartialMatches(args[2], materialNames, completions);
                if (subCommand.equals("remove") && plugin.getCustomGroups().containsKey(groupName)) {
                    List<String> existingItems = plugin.getCustomGroups().get(groupName).stream()
                            .map(Material::name)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());
                    StringUtil.copyPartialMatches(args[2], existingItems, completions);
                }
            }
        }
        Collections.sort(completions);
        return completions;
    }
}