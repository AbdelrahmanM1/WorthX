package me.abdoabk.worth_items.util;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * ConfigUtil
 *
 * FIX: Removed the cached `config` field. All reads now go through
 * plugin.getConfig() so that after reloadConfig() is called the new
 * values are immediately visible — no stale-reference bugs.
 */
public final class ConfigUtil {

    private final JavaPlugin plugin;

    public ConfigUtil(JavaPlugin plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
    }

    /** Returns true if item lore injection is enabled. */
    public boolean isLoreEnabled() {
        return plugin.getConfig().getBoolean("display.inject-lore", true);
    }

    /** Returns the currency symbol (e.g. "$"). */
    public String getCurrencySymbol() {
        return plugin.getConfig().getString("display.currency-symbol", "$");
    }

    /**
     * Reloads config.yml from disk.
     * Callers (e.g. ReloadCommand) should prefer Worth_items#fullReload()
     * so price cache and precompute also refresh.
     */
    public void reload() {
        plugin.reloadConfig();
    }

    /** Returns minimum price for the given tier name. */
    public int getTierMin(String tier) {
        return plugin.getConfig().getInt("tiers." + tier + ".min", defaultMin(tier));
    }

    /** Returns maximum price for the given tier name. */
    public int getTierMax(String tier) {
        return plugin.getConfig().getInt("tiers." + tier + ".max", defaultMax(tier));
    }

    /** Returns true if the given material name appears in the blacklist. */
    public boolean isBlacklisted(String materialName) {
        return plugin.getConfig().getStringList("blacklist").contains(materialName);
    }

    // ── Defaults ────────────────────────────────────────────────────────────

    private int defaultMin(String tier) {
        return switch (tier.toLowerCase()) {
            case "junk"      -> 0;
            case "common"    -> 5;
            case "uncommon"  -> 15;
            case "rare"      -> 40;
            case "epic"      -> 120;
            case "legendary" -> 400;
            default          -> 0;
        };
    }

    private int defaultMax(String tier) {
        return switch (tier.toLowerCase()) {
            case "junk"      -> 5;
            case "common"    -> 15;
            case "uncommon"  -> 40;
            case "rare"      -> 120;
            case "epic"      -> 400;
            case "legendary" -> 3000;
            default          -> 5;
        };
    }
}