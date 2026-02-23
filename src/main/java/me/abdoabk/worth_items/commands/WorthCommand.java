package me.abdoabk.worth_items.commands;

import me.abdoabk.worth_items.Worth_items;
import me.abdoabk.worth_items.api.EssentialsBridge;
import me.abdoabk.worth_items.api.VaultBridge;
import me.abdoabk.worth_items.commands.args.*;
import me.abdoabk.worth_items.pricing.PriceService;
import me.abdoabk.worth_items.util.ConfigUtil;
import me.abdoabk.worth_items.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /worth <hand|inventory|blocks|reload|[material]>
 *
 * Routes to the appropriate subcommand handler.
 * Adding a new subcommand = create a class in args/, add one case here.
 *
 * FIX 1: `special` variable in handleMaterialLookup was computed but never used — removed.
 * FIX 2: Tab-complete now hides "reload" from players without worthitems.reload permission.
 * FIX 3: Material lookup uses vaultBridge.format() for consistent currency display.
 */
public class WorthCommand implements CommandExecutor, TabCompleter {

    private final HandCommand       handCommand;
    private final InventoryCommands inventoryCommands;
    private final BlocksCommand     blocksCommand;
    private final ReloadCommand     reloadCommand;
    private final PriceService      priceService;
    private final VaultBridge       vaultBridge;

    public WorthCommand(Worth_items plugin,
                        PriceService priceService,
                        VaultBridge vaultBridge,
                        EssentialsBridge essentialsBridge,
                        ConfigUtil configUtil) {
        this.priceService = priceService;
        this.vaultBridge  = vaultBridge;

        handCommand       = new HandCommand(vaultBridge, configUtil);
        inventoryCommands = new InventoryCommands(vaultBridge, configUtil);
        blocksCommand     = new BlocksCommand(vaultBridge, configUtil);
        reloadCommand     = new ReloadCommand(plugin);   // FIX: pass plugin, not priceService
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "hand"            -> handCommand.execute(sender);
            case "inventory", "inv"-> inventoryCommands.execute(sender);
            case "blocks"          -> blocksCommand.execute(sender);
            case "reload"          -> reloadCommand.execute(sender);
            default                -> handleMaterialLookup(sender, args[0]);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> matched = new ArrayList<>();

            // FIX: Only show "reload" to players who hold the permission.
            List<String> subs = new ArrayList<>(List.of("hand", "inventory", "blocks"));
            if (sender.hasPermission("worthitems.reload")) subs.add("reload");

            subs.stream()
                    .filter(s -> s.startsWith(input))
                    .forEach(matched::add);

            // Also suggest material names when the user has started typing something.
            if (!input.isEmpty()) {
                Arrays.stream(Material.values())
                        .map(m -> m.name().toLowerCase())
                        .filter(n -> n.startsWith(input))
                        .limit(15)
                        .forEach(matched::add);
            }

            return matched;
        }
        return List.of();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void handleMaterialLookup(CommandSender sender, String input) {
        Material material = Material.matchMaterial(input.toUpperCase());
        if (material == null || material.isAir()) {
            sender.sendMessage("§cUnknown item or subcommand: §7" + input);
            sendHelp(sender);
            return;
        }

        int price = priceService.getPrice(material);
        // FIX: `special` was computed here but never used. It's removed.
        // If you want to annotate special prices in the future, re-add it with output.

        String name = ItemUtil.formatName(material);

        sender.sendMessage("§8§m──────────────────────────");
        sender.sendMessage(" §6" + name);
        sender.sendMessage(" §7Worth: §a" + vaultBridge.format(price));
        if (priceService.hasSpecialOverride(material)) {
            sender.sendMessage(" §e⭐ Custom price set in config.");
        }
        sender.sendMessage("§8§m──────────────────────────");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m──────────────────────────");
        sender.sendMessage(" §6Worth Items §7— Commands");
        sender.sendMessage(" §f/worth hand        §7— held item worth");
        sender.sendMessage(" §f/worth inventory   §7— full inventory worth");
        sender.sendMessage(" §f/worth blocks      §7— blocks-only worth");
        sender.sendMessage(" §f/worth <material>  §7— look up any item");
        if (sender.hasPermission("worthitems.reload")) {
            sender.sendMessage(" §f/worth reload      §7— reset price cache");
        }
        sender.sendMessage("§8§m──────────────────────────");
    }
}