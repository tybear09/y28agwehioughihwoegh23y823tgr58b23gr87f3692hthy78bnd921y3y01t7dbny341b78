package com.thiefsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HeartManager {

    public static final class Data {
        public HeartType type;
        public int power;

        Data(HeartType type, int power) {
            this.type = type;
            this.power = power;
        }
    }

    private final ThiefSMP plugin;
    private final File file;
    private final Map<UUID, Data> data = new HashMap<>();
    private boolean started;

    public HeartManager(ThiefSMP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerdata.yml");
        load();
    }

    public int maxPower() {
        return Math.max(1, plugin.getConfig().getInt("max-power", 7));
    }

    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }

    public Data get(UUID id) { return data.get(id); }
    public boolean has(UUID id) { return data.containsKey(id); }
    public void clearAll() { data.clear(); }

    public void set(UUID id, HeartType type, int power) {
        int clamped = Math.max(1, Math.min(maxPower(), power));
        data.put(id, new Data(type, clamped));
    }

    public void assignRandom(Player p) {
        HeartType type = HeartType.random();
        set(p.getUniqueId(), type, 1);
        announce(p, type);
    }

    public void announce(Player p, HeartType type) {
        p.showTitle(Title.title(
                Component.text(type.displayName() + " Heart", type.color()),
                Component.text("Collect \u2022 Claim \u2022 Grow \u2022 Survive", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("You received the " + type.displayName() + " Heart! ", type.color())
                .append(Component.text(type.description(), NamedTextColor.GRAY)));
        p.sendMessage(Component.text("Press F (swap hands) to use your ability.", NamedTextColor.YELLOW));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        started = yml.getBoolean("started", false);
        ConfigurationSection sec = yml.getConfigurationSection("players");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                HeartType type = HeartType.fromString(sec.getString(key + ".type", ""));
                if (type == null) continue;
                int power = Math.max(1, Math.min(maxPower(), sec.getInt(key + ".power", 1)));
                data.put(id, new Data(type, power));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("started", started);
        for (Map.Entry<UUID, Data> e : data.entrySet()) {
            yml.set("players." + e.getKey() + ".type", e.getValue().type.name());
            yml.set("players." + e.getKey() + ".power", e.getValue().power);
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save playerdata.yml: " + ex.getMessage());
        }
    }
}
