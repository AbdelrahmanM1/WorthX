package me.abdoabk.worth_items.commands.args;

import me.abdoabk.worth_items.api.VaultBridge;
import me.abdoabk.worth_items.util.ConfigUtil;
import me.abdoabk.worth_items.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * /worth inventory (/worth inv)
 *
 * Shows the worth of every item in the player's inventory, grouped by material.
 * Uses vaultBridge.format() for consistent currency display matching HandCommand.
 */
public class InventoryCommands {

    private final VaultBridge vaultBridge;
    private final ConfigUtil  configUtil;

    public InventoryCommands(VaultBridge vaultBridge, ConfigUtil configUtil) {
        this.vaultBridge = vaultBridge;
        this.configUtil  = configUtil;
    }

    public void execute(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use /worth inventory.");
            return;
        }

        ItemStack[] contents = player.getInventory().getContents();
        Map<Material, Integer> totals = new HashMap<>();

        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) continue;
            if (configUtil.isBlacklisted(item.getType().name())) continue;
            if (vaultBridge.getWorth(item.getType()) <= 0) continue;
            totals.merge(item.getType(), item.getAmount(), Integer::sum);
        }

        if (totals.isEmpty()) {
            player.sendMessage("§cYour inventory contains no items with a sell value.");
            return;
        }

        int grandTotal = 0;

        player.sendMessage("§8§m──────────────────────────");
        player.sendMessage(" §6Inventory Worth");
        player.sendMessage("§8§m──────────────────────────");

        for (Map.Entry<Material, Integer> entry : totals.entrySet()) {
            Material mat  = entry.getKey();
            int amount    = entry.getValue();
            int subtotal  = vaultBridge.getWorth(mat, amount);
            grandTotal   += subtotal;

            player.sendMessage(" §7" + ItemUtil.formatName(mat)
                    + " §8x" + amount
                    + " §7→ §a" + vaultBridge.format(subtotal));
        }

        player.sendMessage("§8§m──────────────────────────");
        player.sendMessage(" §6Total: §a§l" + vaultBridge.format(grandTotal));
        player.sendMessage("§8§m──────────────────────────");
    }
}