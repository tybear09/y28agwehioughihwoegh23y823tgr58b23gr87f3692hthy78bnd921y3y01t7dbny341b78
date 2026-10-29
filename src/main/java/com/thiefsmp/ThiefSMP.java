
package com.thiefsmp;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ThiefSMP extends JavaPlugin {

    private HeartManager hearts;
    private HeartItems items;
    private Abilities abilities;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        hearts = new HeartManager(this);
        items = new HeartItems(this);
        items.registerRecipes();
        abilities = new Abilities(this, hearts);

        getServer().getPluginManager().registerEvents(new HeartListener(this), this);

        PluginCommand command = getCommand("thiefsmp");
        if (command != null) {
            ThiefCommand handler = new ThiefCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        // Passive effects + action bar once per second
        getServer().getScheduler().runTaskTimer(this, abilities::tick, 20L, 20L);
        // Autosave every 5 minutes
        getServer().getScheduler().runTaskTimer(this, hearts::save, 6000L, 6000L);

        for (Player p : Bukkit.getOnlinePlayers()) {
            items.discover(p);
        }
        getLogger().info("Thief SMP enabled.");
    }

    @Override
    public void onDisable() {
        if (hearts != null) hearts.save();
        if (items != null) items.unregisterRecipes();
    }

    public HeartManager hearts() { return hearts; }
    public HeartItems items() { return items; }
    public Abilities abilities() { return abilities; }
}
