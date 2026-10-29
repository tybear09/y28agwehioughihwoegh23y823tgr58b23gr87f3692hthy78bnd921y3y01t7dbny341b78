package com.thiefsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Keyed;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Locale;

public final class HeartListener implements Listener {

    private final ThiefSMP plugin;

    public HeartListener(ThiefSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.items().discover(p);
        HeartManager hm = plugin.hearts();
        if (hm.isStarted() && !hm.has(p.getUniqueId())) {
            hm.assignRandom(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.abilities().clearCooldown(e.getPlayer().getUniqueId());
        plugin.hearts().save();
    }

    /** F key (swap hands) casts the heart ability. */
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (!plugin.hearts().has(p.getUniqueId())) return;
        e.setCancelled(true);
        plugin.abilities().cast(p);
    }

    /** Right-click a Heart item to claim it. */
    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getItem();
        HeartItems items = plugin.items();
        if (!items.isHeart(item)) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        HeartType type = items.getType(item);
        if (type == null) {
            p.sendMessage(Component.text("The base Heart is a crafting material. Combine it with ingredients to make an elemental Heart.", NamedTextColor.GRAY));
            return;
        }

        HeartManager hm = plugin.hearts();
        HeartManager.Data d = hm.get(p.getUniqueId());
        int power = d == null ? 1 : d.power;
        item.setAmount(item.getAmount() - 1);
        hm.set(p.getUniqueId(), type, power);
        hm.announce(p, type);
        hm.save();
    }

    /** Killing players grows your power, dying shrinks it. */
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;

        HeartManager hm = plugin.hearts();
        int gain = plugin.getConfig().getInt("power-gain-on-kill", 1);
        int loss = plugin.getConfig().getInt("power-loss-on-death", 1);

        HeartManager.Data vd = hm.get(victim.getUniqueId());
        if (vd != null && loss > 0) {
            hm.set(victim.getUniqueId(), vd.type, vd.power - loss);
            victim.sendMessage(Component.text("You lost power! Now power " + hm.get(victim.getUniqueId()).power + "/" + hm.maxPower() + ".", NamedTextColor.RED));
        }

        HeartManager.Data kd = hm.get(killer.getUniqueId());
        if (kd != null && gain > 0) {
            hm.set(killer.getUniqueId(), kd.type, kd.power + gain);
            killer.sendMessage(Component.text("You stole power from " + victim.getName() + "! Now power "
                    + hm.get(killer.getUniqueId()).power + "/" + hm.maxPower() + ".", NamedTextColor.GREEN));
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        }
        hm.save();
    }

    /** Frost hearts are immune to freezing damage. */
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FREEZE) return;
        if (!(e.getEntity() instanceof Player p)) return;
        HeartManager.Data d = plugin.hearts().get(p.getUniqueId());
        if (d != null && d.type == HeartType.FROST) {
            e.setCancelled(true);
        }
    }

    /** Stops Heart items from being used in any recipe other than ours. */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        Recipe recipe = e.getRecipe();
        String ns = plugin.getName().toLowerCase(Locale.ROOT);
        if (recipe instanceof Keyed k && k.getKey().getNamespace().equals(ns)) return;

        for (ItemStack item : e.getInventory().getMatrix()) {
            if (plugin.items().isHeart(item)) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }
}
