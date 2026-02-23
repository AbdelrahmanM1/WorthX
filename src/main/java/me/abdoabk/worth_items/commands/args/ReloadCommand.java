package me.abdoabk.worth_items.commands.args;

import me.abdoabk.worth_items.Worth_items;
import org.bukkit.command.CommandSender;

/**
 * /worth reload
 *
 * Delegates to Worth_items#fullReload() which:
 *   1. Calls plugin.reloadConfig() — picks up config.yml changes from disk.
 *   2. Calls priceService.reload() — clears the stale price cache.
 *   3. Calls precomputeAllPrices() — refills the cache with fresh config values.
 */
public class ReloadCommand {

    private final Worth_items plugin;

    public ReloadCommand(Worth_items plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender) {
        if (!sender.hasPermission("worthitems.reload")) {
            sender.sendMessage("§cYou don't have permission to reload Worth Items.");
            return;
        }

        plugin.fullReload();
        sender.sendMessage("§aWorth Items — config and prices reloaded successfully.");
    }
}