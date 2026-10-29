package com.thiefsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class Abilities {

    private final ThiefSMP plugin;
    private final HeartManager hearts;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public Abilities(ThiefSMP plugin, HeartManager hearts) {
        this.plugin = plugin;
        this.hearts = hearts;
    }

    public void clearCooldown(UUID id) {
        cooldowns.remove(id);
    }

    // ------------------------------------------------------------------
    // Passive effects + action bar (runs once per second)
    // ------------------------------------------------------------------

    public void tick() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            HeartManager.Data d = hearts.get(p.getUniqueId());
            if (d == null || p.getGameMode() == GameMode.SPECTATOR) continue;

            applyPassives(p, d);

            long left = cooldowns.getOrDefault(p.getUniqueId(), 0L) - now;
            Component status = left > 0
                    ? Component.text((left / 1000 + 1) + "s", NamedTextColor.RED)
                    : Component.text("READY (press F)", NamedTextColor.GREEN);
            p.sendActionBar(Component.text(d.type.displayName() + " Heart ", d.type.color())
                    .append(Component.text("| Power " + d.power + "/" + hearts.maxPower() + " | Ability: ", NamedTextColor.GRAY))
                    .append(status));
        }
    }

    private void applyPassives(Player p, HeartManager.Data d) {
        boolean max = d.power >= hearts.maxPower();
        switch (d.type) {
            case SPEED -> eff(p, PotionEffectType.SPEED, max ? 1 : 0, 60);
            case EARTH -> { }
            case WATER -> {
                if (p.isInWater()) {
                    eff(p, PotionEffectType.DOLPHINS_GRACE, max ? 1 : 0, 60);
                    eff(p, PotionEffectType.WATER_BREATHING, 0, 60);
                } else {
                    eff(p, PotionEffectType.SLOWNESS, 0, 60);
                }
            }
            case FIRE -> eff(p, PotionEffectType.FIRE_RESISTANCE, 0, 60);
            case FROST -> {
                if (max) eff(p, PotionEffectType.RESISTANCE, 0, 60);
            }
            case LIGHTNING -> eff(p, PotionEffectType.HASTE, max ? 1 : 0, 60);
            case SHADOW -> eff(p, PotionEffectType.NIGHT_VISION, 0, 300);
            case LIFE -> {
                eff(p, PotionEffectType.HEALTH_BOOST, max ? 1 : 0, 60);
                if (max) eff(p, PotionEffectType.REGENERATION, 0, 60);
            }
        }
    }

    private void eff(Player p, PotionEffectType type, int amplifier, int duration) {
        p.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, true));
    }

    // ------------------------------------------------------------------
    // Active abilities
    // ------------------------------------------------------------------

    public void cast(Player p) {
        HeartManager.Data d = hearts.get(p.getUniqueId());
        if (d == null) return;

        long now = System.currentTimeMillis();
        long ready = cooldowns.getOrDefault(p.getUniqueId(), 0L);
        if (now < ready) {
            p.sendActionBar(Component.text("Ability on cooldown: " + ((ready - now) / 1000 + 1) + "s", NamedTextColor.RED));
            return;
        }

        boolean max = d.power >= hearts.maxPower();
        boolean used = switch (d.type) {
            case SPEED -> dash(p, max);
            case EARTH -> earth(p, max);
            case WATER -> water(p, max);
            case FIRE -> fire(p, max);
            case FROST -> frost(p, max);
            case LIGHTNING -> lightning(p, max);
            case SHADOW -> shadow(p, max);
            case LIFE -> life(p, max);
        };

        if (used) {
            double multiplier = 1.0 - 0.03 * (d.power - 1); // higher power = shorter cooldown
            cooldowns.put(p.getUniqueId(), now + (long) (d.type.cooldownSeconds() * 1000L * multiplier));
        }
    }

    private boolean dash(Player p, boolean max) {
        Vector v = p.getLocation().getDirection().multiply(max ? 1.9 : 1.4);
        v.setY(v.getY() + 0.2);
        p.setVelocity(v);
        World w = p.getWorld();
        w.spawnParticle(Particle.CLOUD, p.getLocation(), 20, 0.3, 0.1, 0.3, 0.05);
        w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        return true;
    }

    private boolean earth(Player p, boolean max) {
        BlockData grass = Material.GRASS_BLOCK.createBlockData();
        BlockData dirt = Material.DIRT.createBlockData();
        shoot(p, max ? 5.0 : 2.0, Sound.BLOCK_GRASS_BREAK,
                loc -> loc.getWorld().spawnParticle(Particle.BLOCK, loc, 6, 0.15, 0.15, 0.15, 0.0,
                        ThreadLocalRandom.current().nextBoolean() ? grass : dirt),
                le -> { });
        return true;
    }

    private boolean water(Player p, boolean max) {
        shoot(p, max ? 7.0 : 3.0, Sound.ENTITY_GENERIC_SPLASH,
                loc -> loc.getWorld().spawnParticle(Particle.SPLASH, loc, 12, 0.2, 0.2, 0.2, 0.05),
                le -> le.setFireTicks(0));
        return true;
    }

    private boolean fire(Player p, boolean max) {
        shoot(p, max ? 8.0 : 2.0, Sound.ENTITY_BLAZE_SHOOT,
                loc -> {
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 8, 0.15, 0.15, 0.15, 0.01);
                    loc.getWorld().spawnParticle(Particle.SMOKE, loc, 2, 0.1, 0.1, 0.1, 0.01);
                },
                le -> {
                    le.setFireTicks(100);
                    le.getWorld().spawnParticle(Particle.EXPLOSION, le.getLocation().add(0, 1, 0), 1);
                });
        return true;
    }

    private boolean frost(Player p, boolean max) {
        shoot(p, max ? 6.0 : 2.0, Sound.ENTITY_SNOW_GOLEM_SHOOT,
                loc -> loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 8, 0.15, 0.15, 0.15, 0.01),
                le -> {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, max ? 100 : 60, max ? 2 : 1));
                    le.setFreezeTicks(le.getMaxFreezeTicks());
                });
        return true;
    }

    private boolean lightning(Player p, boolean max) {
        World w = p.getWorld();
        Location eye = p.getEyeLocation();
        RayTraceResult r = w.rayTrace(eye, eye.getDirection(), 30, FluidCollisionMode.NEVER, true, 0.3,
                en -> en instanceof LivingEntity && !en.equals(p));

        Location target;
        if (r != null) {
            target = r.getHitPosition().toLocation(w);
        } else {
            target = eye.clone().add(eye.getDirection().multiply(30));
            target.setY(w.getHighestBlockYAt(target) + 1);
        }

        w.strikeLightningEffect(target);
        for (LivingEntity le : w.getNearbyLivingEntities(target, 3.0)) {
            if (le.equals(p) || !validTarget(le)) continue;
            le.damage(max ? 10.0 : 6.0, p);
        }
        return true;
    }

    private boolean shadow(Player p, boolean max) {
        World w = p.getWorld();
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        double dist = max ? 12 : 8;

        RayTraceResult r = w.rayTraceBlocks(eye, dir, dist, FluidCollisionMode.NEVER, true);
        double travel = r == null ? dist : Math.max(0, r.getHitPosition().distance(eye.toVector()) - 0.7);

        Location dest = eye.clone().add(dir.clone().multiply(travel)).subtract(0, p.getEyeHeight(), 0);
        dest.setYaw(eye.getYaw());
        dest.setPitch(eye.getPitch());

        boolean ok = false;
        for (int i = 0; i < 3; i++) {
            if (dest.getBlock().isPassable() && dest.clone().add(0, 1, 0).getBlock().isPassable()) {
                ok = true;
                break;
            }
            dest.add(0, 1, 0);
        }
        if (!ok) {
            p.sendActionBar(Component.text("No room to step there.", NamedTextColor.RED));
            return false;
        }

        Location from = p.getLocation();
        w.spawnParticle(Particle.PORTAL, from.clone().add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.5);
        w.playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        p.teleport(dest);
        p.setFallDistance(0);
        w.spawnParticle(Particle.PORTAL, dest.clone().add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.5);
        w.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.3f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0, false, false, true));
        return true;
    }

    private boolean life(Player p, boolean max) {
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = attr == null ? 20.0 : attr.getValue();
        p.setHealth(Math.min(maxHp, p.getHealth() + (max ? 10.0 : 6.0)));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, max ? 2 : 1));
        if (max) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 0));
        }
        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 1.5, 0), 10, 0.5, 0.4, 0.5);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        return true;
    }

    // ------------------------------------------------------------------
    // Projectile helper (particle-trail projectile, no blocks are damaged)
    // ------------------------------------------------------------------

    private void shoot(Player shooter, double damageHp, Sound sound,
                       Consumer<Location> trail, Consumer<LivingEntity> onHit) {
        Location eye = shooter.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        eye.getWorld().playSound(eye, sound, 1f, 1f);

        new BukkitRunnable() {
            final Location loc = eye.clone().add(dir.clone().multiply(0.8));
            int ticks = 0;

            @Override
            public void run() {
                if (!shooter.isOnline()) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 2; i++) {
                    loc.add(dir.clone().multiply(0.7));
                    if (!loc.getBlock().isPassable()) {
                        cancel();
                        return;
                    }
                    trail.accept(loc);
                    for (LivingEntity le : loc.getWorld().getNearbyLivingEntities(loc, 0.7)) {
                        if (le.equals(shooter) || !validTarget(le)) continue;
                        le.damage(damageHp, shooter);
                        onHit.accept(le);
                        cancel();
                        return;
                    }
                }
                if (++ticks > 30) cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean validTarget(LivingEntity le) {
        if (le instanceof ArmorStand) return false;
        if (le instanceof Player pl) {
            return pl.getGameMode() == GameMode.SURVIVAL || pl.getGameMode() == GameMode.ADVENTURE;
        }
        return true;
    }
}
