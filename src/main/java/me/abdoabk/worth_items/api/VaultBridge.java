package me.abdoabk.worth_items.api;

import me.abdoabk.worth_items.pricing.PriceService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * VaultBridge — thin wrapper around the Vault economy API.
 *
 * Hooks into the economy provider on the next server tick so that
 * all plugins (including EssentialsX) have finished registering their
 * services before we attempt to grab one.
 */
public class VaultBridge {

    private Economy economy;
    private final PriceService priceService;
    private final JavaPlugin plugin;
    private boolean available = false;

    public VaultBridge(JavaPlugin plugin, PriceService priceService) {
        this.plugin       = plugin;
        this.priceService = priceService;

        // Delay hook by one tick so all plugins finish registering first.
        plugin.getServer().getScheduler().runTask(plugin, this::hook);
    }

    // ── Availability ─────────────────────────────────────────────────────────

    public boolean isAvailable() { return available; }

    public String getEconomyName() {
        return available ? economy.getName() : "None";
    }

    // ── Worth queries ────────────────────────────────────────────────────────

    public int getWorth(Material material) {
        if (material == null || material.isAir()) return 0;
        return priceService.getPrice(material);
    }

    public int getWorth(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        return priceService.getPrice(item.getType()) * item.getAmount();
    }

    public int getWorth(Material material, int amount) {
        if (material == null || material.isAir() || amount <= 0) return 0;
        return priceService.getPrice(material) * amount;
    }

    public String getWorthFormatted(Material material) {
        return format(getWorth(material));
    }

    public String getWorthFormatted(ItemStack item) {
        return format(getWorth(item));
    }

    // ── Economy operations ───────────────────────────────────────────────────

    public SellResult sellItem(OfflinePlayer player, ItemStack item) {
        if (!available)                              return SellResult.fail("Vault is not available.");
        if (item == null || item.getType().isAir()) return SellResult.fail("Item is null or air.");

        int earned = getWorth(item);
        if (earned <= 0) return SellResult.fail(item.getType().name() + " has no sell value.");

        EconomyResponse response = economy.depositPlayer(player, earned);
        if (!response.transactionSuccess()) return SellResult.fail(response.errorMessage);

        return SellResult.success(earned, item.getAmount(), item.getType());
    }

    public SellResult sellItem(OfflinePlayer player, Material material, int amount) {
        if (!available)                           return SellResult.fail("Vault is not available.");
        if (material == null || material.isAir()) return SellResult.fail("Material is null or air.");
        if (amount <= 0)                          return SellResult.fail("Amount must be > 0.");

        int earned = getWorth(material, amount);
        if (earned <= 0) return SellResult.fail(material.name() + " has no sell value.");

        EconomyResponse response = economy.depositPlayer(player, earned);
        if (!response.transactionSuccess()) return SellResult.fail(response.errorMessage);

        return SellResult.success(earned, amount, material);
    }

    public double getBalance(OfflinePlayer player) {
        if (!available) return 0;
        return economy.getBalance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (!available) return false;
        return economy.has(player, amount);
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!available) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!available) return false;
        if (!economy.has(player, amount)) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Formats an amount using the Vault economy formatter (includes symbol/name).
     * Falls back to a plain integer string if Vault is unavailable.
     */
    public String format(double amount) {
        if (!available) return String.valueOf((int) amount);
        return economy.format(amount);
    }

    // ── SellResult ───────────────────────────────────────────────────────────

    public record SellResult(
            boolean success,
            int earned,
            int amount,
            Material material,
            String errorMessage
    ) {
        public static SellResult success(int earned, int amount, Material material) {
            return new SellResult(true, earned, amount, material, null);
        }

        public static SellResult fail(String reason) {
            return new SellResult(false, 0, 0, null, reason);
        }
    }

    // ── Internal hook ────────────────────────────────────────────────────────

    private void hook() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("VaultBridge: Vault not found — economy features disabled.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().warning(
                    "VaultBridge: No economy provider registered — is EssentialsX/CMI installed?");
            return;
        }

        economy   = rsp.getProvider();
        available = economy != null;

        if (available) {
            plugin.getLogger().info("VaultBridge: Economy hooked → " + economy.getName());
        }
    }
}