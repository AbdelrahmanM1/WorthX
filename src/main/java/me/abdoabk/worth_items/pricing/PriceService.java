package me.abdoabk.worth_items.pricing;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

/**
 * PriceService — Central pricing engine.
 *
 * Priority order:
 *   1. Special-item override from config.yml
 *   2. Blacklist → always 0
 *   3. Cached price (generated previously this session / loaded from prices.yml)
 *   4. Tier-based seeded generation → stored in cache
 *
 * FIX 1: Config is read live via plugin.getConfig() so reloadConfig() is respected.
 * FIX 2: blacklist is no longer cached at construction — it's read fresh each call.
 * FIX 3: Random is seeded per-material so prices are stable across reload cycles.
 */
public class PriceService {

    private final JavaPlugin plugin;
    private final PriceStorage storage;
    private final TierResolver tierResolver;

    public PriceService(JavaPlugin plugin, PriceStorage storage, TierResolver tierResolver) {
        this.plugin       = plugin;
        this.storage      = storage;
        this.tierResolver = tierResolver;
    }

    /**
     * Returns the sell price for a material.
     * Result is deterministic after the first call (cached in PriceStorage).
     */
    public int getPrice(Material material) {
        // 1. Special override (always wins)
        int override = getSpecialOverride(material);
        if (override >= 0) return override;

        // 2. Blacklist
        if (isBlacklisted(material)) return 0;

        // 3. Cache hit
        if (storage.has(material)) return storage.get(material);

        // 4. Generate, store, and return
        int price = generatePrice(material);
        storage.set(material, price);
        return price;
    }

    /**
     * Clears the price cache so prices re-derive on the next request.
     * Called by fullReload() — config is reloaded externally before this.
     */
    public void reload() {
        storage.clearCache();
    }

    /** Returns true if this material has a manual override in config.yml. */
    public boolean hasSpecialOverride(Material material) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("special-items");
        return section != null && section.contains(material.name());
    }

    /** Returns true if material appears in the blacklist in config.yml. */
    public boolean isBlacklisted(Material material) {
        // FIX: read live from plugin.getConfig() — never stale after reload.
        return plugin.getConfig().getStringList("blacklist").contains(material.name());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private int getSpecialOverride(Material material) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("special-items");
        if (section == null || !section.contains(material.name())) return -1;
        return section.getInt(material.name());
    }

    private int generatePrice(Material material) {
        Tier tier    = tierResolver.resolve(material);
        String key   = tier.name().toLowerCase();

        int min = plugin.getConfig().getInt("tiers." + key + ".min", defaultMin(tier));
        int max = plugin.getConfig().getInt("tiers." + key + ".max", defaultMax(tier));

        if (min >= max) return min;

        // FIX: Seed the RNG with (configSeed XOR material ordinal) so that:
        //  - Prices are stable between reloads for the same seed.
        //  - Different materials always get different values.
        //  - Server admins can change "price-seed" to shuffle all prices intentionally.
        long seed = plugin.getConfig().getLong("price-seed", 0xDEADBEEFL) ^ material.ordinal();
        int range = max - min + 1;
        return min + (int) (new Random(seed).nextInt(range));
    }

    private int defaultMin(Tier tier) {
        return switch (tier) {
            case JUNK      -> 0;
            case COMMON    -> 5;
            case UNCOMMON  -> 15;
            case RARE      -> 40;
            case EPIC      -> 120;
            case LEGENDARY -> 400;
        };
    }

    private int defaultMax(Tier tier) {
        return switch (tier) {
            case JUNK      -> 5;
            case COMMON    -> 15;
            case UNCOMMON  -> 40;
            case RARE      -> 120;
            case EPIC      -> 400;
            case LEGENDARY -> 3000;
        };
    }
}