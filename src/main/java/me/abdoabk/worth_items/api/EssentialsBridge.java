package me.abdoabk.worth_items.api;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.Worth;
import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;
import me.abdoabk.worth_items.pricing.PriceService;
import net.ess3.api.MaxMoneyException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.logging.Logger;

/**
 * EssentialsBridge — wrapper around the EssentialsX API.
 *
 * Provides balance get/set/deposit/withdraw and metadata helpers
 * (nickname, AFK, vanish, mute) plus runtime price sync.
 *
 * FIX: syncToEssentials log message corrected — Worth.setPrice() updates
 *      runtime prices only, not the worth.yml file on disk.
 */
public class EssentialsBridge {

    private final Essentials essentials;
    private final boolean available;
    private final Logger log;
    private final JavaPlugin plugin;

    public EssentialsBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log    = plugin.getLogger();

        org.bukkit.plugin.Plugin ess =
                plugin.getServer().getPluginManager().getPlugin("Essentials");

        if (ess instanceof Essentials hooked) {
            this.essentials = hooked;
            this.available  = true;
            log.info("EssentialsBridge: hooked → v" + ess.getDescription().getVersion());
        } else {
            this.essentials = null;
            this.available  = false;
            log.warning("EssentialsBridge: Essentials not found — features disabled.");
        }
    }

    public boolean isAvailable() { return available; }

    // ── Balance operations ───────────────────────────────────────────────────

    public BigDecimal getBalance(Player player) {
        if (!available || player == null) return BigDecimal.ZERO;
        try {
            return Economy.getMoneyExact(player.getUniqueId());
        } catch (UserDoesNotExistException e) {
            log.warning("EssentialsBridge: getBalance — user not found: " + player.getName());
            return BigDecimal.ZERO;
        }
    }

    public boolean has(Player player, double amount) {
        if (!available || player == null) return false;
        try {
            return Economy.hasEnough(player.getUniqueId(), BigDecimal.valueOf(amount));
        } catch (UserDoesNotExistException e) {
            log.warning("EssentialsBridge: has — user not found: " + player.getName());
            return false;
        }
    }

    public boolean setBalance(Player player, double amount) {
        if (!available || player == null) return false;
        try {
            Economy.setMoney(player.getUniqueId(), BigDecimal.valueOf(amount));
            return true;
        } catch (MaxMoneyException | UserDoesNotExistException | NoLoanPermittedException e) {
            log.warning("EssentialsBridge: setBalance failed for " + player.getName()
                    + " — " + e.getMessage());
            return false;
        }
    }

    public boolean deposit(Player player, double amount) {
        if (!available || player == null) return false;
        try {
            Economy.add(player.getUniqueId(), BigDecimal.valueOf(amount));
            return true;
        } catch (MaxMoneyException | UserDoesNotExistException | NoLoanPermittedException e) {
            log.warning("EssentialsBridge: deposit failed for " + player.getName()
                    + " — " + e.getMessage());
            return false;
        }
    }

    public boolean withdraw(Player player, double amount) {
        if (!available || player == null) return false;
        try {
            Economy.subtract(player.getUniqueId(), BigDecimal.valueOf(amount));
            return true;
        } catch (MaxMoneyException | UserDoesNotExistException | NoLoanPermittedException e) {
            log.warning("EssentialsBridge: withdraw failed for " + player.getName()
                    + " — " + e.getMessage());
            return false;
        }
    }

    // ── Player metadata ──────────────────────────────────────────────────────

    public String getNickname(Player player) {
        User user = getUser(player);
        if (user == null) return player.getName();
        String nick = user.getNickname();
        return (nick != null && !nick.isEmpty()) ? nick : player.getName();
    }

    public boolean isAfk(Player player) {
        User user = getUser(player);
        return user != null && user.isAfk();
    }

    public boolean isVanished(Player player) {
        User user = getUser(player);
        return user != null && user.isVanished();
    }

    public boolean isMuted(Player player) {
        User user = getUser(player);
        return user != null && user.isMuted();
    }

    // ── Worth integration ────────────────────────────────────────────────────

    public double getEssentialsWorth(Material material) {
        if (!available || material == null) return -1;
        try {
            Worth worth = essentials.getWorth();
            BigDecimal val = worth.getPrice(essentials, new ItemStack(material, 1));
            return val == null ? -1 : val.doubleValue();
        } catch (Exception e) {
            log.warning("EssentialsBridge: getEssentialsWorth failed for "
                    + material.name() + " — " + e.getMessage());
            return -1;
        }
    }

    /**
     * Pushes all plugin prices into EssentialsX at runtime (in-memory only).
     *
     * FIX: Log message previously said "→ Essentials worth.yml" which is wrong.
     * Worth.setPrice() sets runtime prices; it does NOT write to worth.yml on disk.
     * The correct description is "→ Essentials runtime worth cache".
     */
    public void syncToEssentials(PriceService priceService) {
        if (!available) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            Worth worth = essentials.getWorth();
            int synced = 0;

            for (Material material : Material.values()) {
                if (!material.isItem() || material.isAir()) continue;

                try {
                    double price = priceService.getPrice(material);
                    if (price <= 0) continue;

                    worth.setPrice(essentials, new ItemStack(material, 1), price);
                    synced++;
                } catch (Exception e) {
                    log.warning("EssentialsBridge: sync failed for "
                            + material.name() + " — " + e.getMessage());
                }
            }

            // FIX: Correct log message — runtime cache, not worth.yml file.
            log.info("EssentialsBridge: synced " + synced
                    + " prices → Essentials runtime worth cache (in-memory only).");
        });
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private User getUser(Player player) {
        if (!available || player == null) return null;
        return essentials.getUser(player);
    }
}