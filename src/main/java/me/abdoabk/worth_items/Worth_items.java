package me.abdoabk.worth_items;

import me.abdoabk.worth_items.api.EssentialsBridge;
import me.abdoabk.worth_items.api.VaultBridge;
import me.abdoabk.worth_items.api.WorthPluginAPI;
import me.abdoabk.worth_items.commands.WorthCommand;
import me.abdoabk.worth_items.listener.BlockDropListener;
import me.abdoabk.worth_items.listener.InventoryListener;
import me.abdoabk.worth_items.pricing.PriceService;
import me.abdoabk.worth_items.pricing.PriceStorage;
import me.abdoabk.worth_items.pricing.TierResolver;
import me.abdoabk.worth_items.util.ConfigUtil;
import me.abdoabk.worth_items.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public final class Worth_items extends JavaPlugin {

    private PriceStorage priceStorage;
    private PriceService priceService;
    private VaultBridge vaultBridge;
    private EssentialsBridge essentialsBridge;
    private WorthPluginAPI worthAPI;
    private ConfigUtil configUtil;

    @Override
    public void onEnable() {
        ItemUtil.init(this);
        saveDefaultConfig();

        configUtil = new ConfigUtil(this);

        priceStorage = new PriceStorage(this);
        priceService = new PriceService(this, priceStorage, new TierResolver());

        essentialsBridge = new EssentialsBridge(this);

        // FIX: VaultBridge hooks on next tick — Essentials sync must wait 2 ticks
        // to ensure Vault's economy provider is registered first.
        vaultBridge = new VaultBridge(this, priceService);

        worthAPI = new WorthPluginAPI(priceService, vaultBridge, essentialsBridge);

        // Precompute prices synchronously — does NOT need Vault.
        precomputeAllPrices();

        // FIX: Sync to Essentials on tick 2 (after VaultBridge hooks on tick 1).
        getServer().getScheduler().runTaskLater(this, () -> {
            if (essentialsBridge.isAvailable()) {
                essentialsBridge.syncToEssentials(priceService);
            }
        }, 2L);

        registerListeners();
        registerCommands();

        getLogger().info("Worth Items Plugin Enabled - Made by 3bdoabk");
    }

    @Override
    public void onDisable() {
        if (priceStorage != null) priceStorage.saveToDisk();
        getLogger().info("Worth Items Plugin Disabled - Made by 3bdoabk");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new InventoryListener(this, priceService, configUtil), this);

        // FIX: BlockDropListener was declared but never registered — fixed here.
        getServer().getPluginManager().registerEvents(
                new BlockDropListener(priceService, configUtil), this);
    }

    private void registerCommands() {
        WorthCommand worthCommand =
                new WorthCommand(this, priceService, vaultBridge, essentialsBridge, configUtil);

        getCommand("worth").setExecutor(worthCommand);
        getCommand("worth").setTabCompleter(worthCommand);
    }

    /**
     * Reload all plugin state from disk.
     * Called by ReloadCommand so all components refresh together.
     */
    public void fullReload() {
        reloadConfig();                  // reload config.yml from disk
        priceService.reload();          // clear price cache (will re-derive from fresh config)
        precomputeAllPrices();          // fill cache again immediately
        getLogger().info("Worth Items — full reload complete.");
    }

    public WorthPluginAPI getAPI() {
        return worthAPI;
    }

    /**
     * Precompute prices for all non-blacklisted items and persist to prices.yml.
     */
    private void precomputeAllPrices() {
        int count = 0;
        for (Material material : Material.values()) {
            if (!material.isItem() || material.isAir()) continue;
            if (configUtil.isBlacklisted(material.name())) continue;

            int price = priceService.getPrice(material);
            if (price > 0) count++;
        }
        priceStorage.saveToDisk();
        getLogger().info("Precomputed " + count + " item prices → prices.yml");
    }
}