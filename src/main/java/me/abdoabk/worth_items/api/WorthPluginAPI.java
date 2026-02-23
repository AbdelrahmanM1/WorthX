package me.abdoabk.worth_items.api;

import me.abdoabk.worth_items.pricing.PriceService;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * WorthPluginAPI — Public API for the WorthItems plugin.
 *
 * Usage from another plugin:
 *   Plugin p = Bukkit.getPluginManager().getPlugin("WorthItems");
 *   if (p instanceof Worth_items wi) {
 *       WorthPluginAPI api = wi.getAPI();
 *       api.getPrice(Material.DIAMOND);
 *       api.getVault().deposit(player, 500);
 *       api.getEssentials().getNickname(player);
 *   }
 */
public class WorthPluginAPI {

    private final PriceService     priceService;
    private final VaultBridge      vaultBridge;
    private final EssentialsBridge essentialsBridge;

    public WorthPluginAPI(PriceService priceService,
                          VaultBridge vaultBridge,
                          EssentialsBridge essentialsBridge) {
        this.priceService     = priceService;
        this.vaultBridge      = vaultBridge;
        this.essentialsBridge = essentialsBridge;
    }

    // ── Price queries ────────────────────────────────────────────────────────

    public int getPrice(Material material) {
        if (material == null || material.isAir()) return 0;
        return priceService.getPrice(material);
    }

    public int getPrice(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        return priceService.getPrice(item.getType());
    }

    public int getTotalValue(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        return priceService.getPrice(item.getType()) * item.getAmount();
    }

    public int getTotalValue(ItemStack[] items) {
        if (items == null) return 0;
        int total = 0;
        for (ItemStack item : items) total += getTotalValue(item);
        return total;
    }

    public boolean hasSpecialPrice(Material material) {
        return material != null && priceService.hasSpecialOverride(material);
    }

    // ── Bridge accessors ─────────────────────────────────────────────────────

    /** Access Vault economy operations: deposit, withdraw, balance, format. */
    public VaultBridge getVault() { return vaultBridge; }

    /** Access EssentialsX: nickname, AFK, vanish, mute, balance, worth sync. */
    public EssentialsBridge getEssentials() { return essentialsBridge; }
}