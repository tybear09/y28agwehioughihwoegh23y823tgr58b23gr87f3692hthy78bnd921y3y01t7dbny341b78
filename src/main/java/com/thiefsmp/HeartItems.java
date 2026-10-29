package com.thiefsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class HeartItems {

    private final ThiefSMP plugin;
    private final NamespacedKey typeKey;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();

    public HeartItems(ThiefSMP plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "heart_type");
    }

    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    /** The base "Heart" used as the center of every elemental recipe. */
    public ItemStack baseHeart() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain("Heart", NamedTextColor.RED));
        meta.lore(List.of(plain("Crafting base for elemental Hearts.", NamedTextColor.GRAY)));
        meta.setEnchantmentGlintOverride(true);
        meta.setMaxStackSize(1);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "base");
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack heart(HeartType type) {
        ItemStack item = new ItemStack(type.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(type.displayName() + " Heart", type.color()));
        meta.lore(List.of(
                plain(type.description(), NamedTextColor.GRAY),
                plain("Right-click to claim this Heart.", NamedTextColor.YELLOW)));
        meta.setEnchantmentGlintOverride(true);
        meta.setMaxStackSize(1);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    public boolean isHeart(ItemStack item) {
        return item != null
                && item.getType() != Material.AIR
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING);
    }

    /** Returns the elemental type, or null for the base heart / non-hearts. */
    public HeartType getType(ItemStack item) {
        if (!isHeart(item)) return null;
        String v = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        return HeartType.fromString(v);
    }

    public void registerRecipes() {
        // Base Heart
        NamespacedKey baseKey = new NamespacedKey(plugin, "heart");
        Bukkit.removeRecipe(baseKey);
        ShapedRecipe base = new ShapedRecipe(baseKey, baseHeart());
        base.shape("DGD", "GAG", "DGD");
        base.setIngredient('D', Material.DIAMOND);
        base.setIngredient('G', Material.GOLD_INGOT);
        base.setIngredient('A', Material.GOLDEN_APPLE);
        Bukkit.addRecipe(base);
        recipeKeys.add(baseKey);

        // From the recipe sheet
        shapeless(HeartType.SPEED, Material.DIAMOND, Material.FEATHER, Material.BLAZE_POWDER);
        shapeless(HeartType.EARTH, Material.GRASS_BLOCK, Material.DIRT, Material.EMERALD);
        shapeless(HeartType.WATER, Material.WATER_BUCKET, Material.PRISMARINE_SHARD, Material.DIAMOND);
        shapeless(HeartType.FIRE, Material.BLAZE_ROD, Material.MAGMA_CREAM, Material.REDSTONE);

        // Extra hearts
        shapeless(HeartType.FROST, Material.BLUE_ICE, Material.SNOW_BLOCK, Material.PRISMARINE_CRYSTALS);
        shapeless(HeartType.LIGHTNING, Material.LIGHTNING_ROD, Material.AMETHYST_SHARD, Material.GLOWSTONE_DUST);
        shapeless(HeartType.SHADOW, Material.ENDER_PEARL, Material.OBSIDIAN, Material.FERMENTED_SPIDER_EYE);
        shapeless(HeartType.LIFE, Material.GLISTERING_MELON_SLICE, Material.GOLDEN_CARROT, Material.HONEY_BOTTLE);
    }

    private void shapeless(HeartType type, Material... ingredients) {
        NamespacedKey key = new NamespacedKey(plugin, type.key() + "_heart");
        Bukkit.removeRecipe(key);
        ShapelessRecipe recipe = new ShapelessRecipe(key, heart(type));
        recipe.addIngredient(new RecipeChoice.ExactChoice(baseHeart()));
        for (Material m : ingredients) {
            recipe.addIngredient(m);
        }
        Bukkit.addRecipe(recipe);
        recipeKeys.add(key);
    }

    public void unregisterRecipes() {
        for (NamespacedKey key : recipeKeys) {
            Bukkit.removeRecipe(key);
        }
        recipeKeys.clear();
    }

    public void discover(Player p) {
        p.discoverRecipes(recipeKeys);
    }
}
