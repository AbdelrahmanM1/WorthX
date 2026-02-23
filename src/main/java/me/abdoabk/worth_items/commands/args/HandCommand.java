package me.abdoabk.worth_items.commands.args;

import me.abdoabk.worth_items.api.VaultBridge;
import me.abdoabk.worth_items.util.ConfigUtil;
import me.abdoabk.worth_items.util.ItemUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /worth hand
 *
 * FIX: Previously prepended the config currency symbol manually AND used
 * vaultBridge.format() in places — inconsistent and could cause "$$100" output.
 * Now uses vaultBridge.format() consistently everywhere, which delegates to the
 * economy plugin's own formatter (includes symbol, decimal places, etc.).
 * The configUtil.getCurrencySymbol() fallback is only used when Vault is absent.
 */
public class HandCommand {

    private final VaultBridge vaultBridge;
    private final ConfigUtil  configUtil;

    public HandCommand(VaultBridge vaultBridge, ConfigUtil configUtil) {
        this.vaultBridge = vaultBridge;
        this.configUtil  = configUtil;
    }

    public void execute(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use /worth hand.");
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage("§cYou are not holding any item.");
            return;
        }

        if (configUtil.isBlacklisted(held.getType().name())) {
            player.sendMessage("§cThis item is blacklisted and has no worth.");
            return;
        }

        int unitPrice  = vaultBridge.getWorth(held.getType());
        int totalPrice = vaultBridge.getWorth(held);
        String name    = ItemUtil.formatName(held.getType());

        // FIX: vaultBridge.format() includes the symbol from the economy plugin.
        //      Do NOT prepend getCurrencySymbol() as well — that caused "$$100".
        player.sendMessage("§8§m──────────────────────────");
        player.sendMessage(" §6" + name);
        player.sendMessage(" §7Unit worth:   §a" + vaultBridge.format(unitPrice));
        if (held.getAmount() > 1) {
            player.sendMessage(" §7Stack §8(x" + held.getAmount() + ")§7: §a"
                    + vaultBridge.format(totalPrice));
        }
        player.sendMessage("§8§m──────────────────────────");
    }
}