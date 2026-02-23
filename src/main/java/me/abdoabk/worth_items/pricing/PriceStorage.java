package me.abdoabk.worth_items.pricing;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * In-memory price cache backed by prices.yml.
 *
 * NOTE: Not thread-safe — all access must occur on the main server thread.
 * (EnumMap does not support concurrent modification.)
 * Prices survive restarts because they are persisted to prices.yml on shutdown
 * and re-loaded on startup.
 */
public class PriceStorage {

    private final JavaPlugin plugin;
    private final Map<Material, Integer> cache = new EnumMap<>(Material.class);
    private File pricesFile;
    private FileConfiguration pricesConfig;

    public PriceStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        loadFromDisk();
    }

    public boolean has(Material material) {
        return cache.containsKey(material);
    }

    public int get(Material material) {
        return cache.getOrDefault(material, 0);
    }

    public void set(Material material, int price) {
        cache.put(material, price);
        pricesConfig.set(material.name(), price);
    }

    public void saveToDisk() {
        try {
            pricesConfig.save(pricesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save prices.yml", e);
        }
    }

    /**
     * Clears the in-memory cache and deletes prices.yml.
     * Prices will be regenerated (with the same seed, so identical values)
     * on the next getPrice() call.
     */
    public void clearCache() {
        cache.clear();
        pricesConfig = new YamlConfiguration();
        if (pricesFile.exists()) pricesFile.delete();
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private void loadFromDisk() {
        pricesFile = new File(plugin.getDataFolder(), "prices.yml");
        if (!pricesFile.exists()) {
            pricesConfig = new YamlConfiguration();
            return;
        }
        pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);
        for (String key : pricesConfig.getKeys(false)) {
            try {
                Material mat = Material.valueOf(key);
                cache.put(mat, pricesConfig.getInt(key));
            } catch (IllegalArgumentException ignored) {
                // Unknown material name in prices.yml — skip silently.
            }
        }
        plugin.getLogger().info("Loaded " + cache.size() + " cached prices from prices.yml");
    }
}