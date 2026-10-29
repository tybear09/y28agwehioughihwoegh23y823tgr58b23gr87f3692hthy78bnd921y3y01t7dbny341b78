package com.thiefsmp;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public enum HeartType {
    SPEED("Speed", NamedTextColor.AQUA, Material.LIGHT_BLUE_DYE, 8,
            "Permanent Swiftness I (II at power 7). Ability: Dash."),
    EARTH("Earth", NamedTextColor.GREEN, Material.GREEN_DYE, 3,
            "Throw grass/dirt projectiles: 1 heart, 2.5 hearts at power 7."),
    WATER("Water", NamedTextColor.BLUE, Material.BLUE_DYE, 3,
            "Fast in water, debuffed on land. Water projectile: 1.5 hearts, 3.5 at power 7."),
    FIRE("Fire", NamedTextColor.GOLD, Material.RED_DYE, 4,
            "Throw fireballs that ignite the target: 1 heart, 4 hearts at power 7."),
    FROST("Frost", NamedTextColor.DARK_AQUA, Material.CYAN_DYE, 4,
            "Immune to freezing. Frost shard: 1 heart + slowness, 3 hearts at power 7."),
    LIGHTNING("Lightning", NamedTextColor.YELLOW, Material.YELLOW_DYE, 12,
            "Permanent Haste. Smite: area lightning strike, 3 hearts, 5 at power 7."),
    SHADOW("Shadow", NamedTextColor.DARK_GRAY, Material.BLACK_DYE, 10,
            "Permanent Night Vision. Shadow Step: teleport forward and turn invisible."),
    LIFE("Life", NamedTextColor.LIGHT_PURPLE, Material.PINK_DYE, 20,
            "+2 max hearts (+4 at power 7). Healing Pulse heals you.");

    private final String displayName;
    private final NamedTextColor color;
    private final Material material;
    private final int cooldownSeconds;
    private final String description;

    HeartType(String displayName, NamedTextColor color, Material material, int cooldownSeconds, String description) {
        this.displayName = displayName;
        this.color = color;
        this.material = material;
        this.cooldownSeconds = cooldownSeconds;
        this.description = description;
    }

    public String displayName() { return displayName; }
    public NamedTextColor color() { return color; }
    public Material material() { return material; }
    public int cooldownSeconds() { return cooldownSeconds; }
    public String description() { return description; }
    public String key() { return name().toLowerCase(Locale.ROOT); }

    public static HeartType fromString(String s) {
        if (s == null) return null;
        for (HeartType t : values()) {
            if (t.name().equalsIgnoreCase(s.trim())) return t;
        }
        return null;
    }

    public static HeartType random() {
        HeartType[] all = values();
        return all[ThreadLocalRandom.current().nextInt(all.length)];
    }
}
