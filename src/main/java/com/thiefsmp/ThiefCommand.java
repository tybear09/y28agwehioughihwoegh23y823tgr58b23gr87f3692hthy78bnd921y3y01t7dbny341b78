package com.thiefsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ThiefCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ALL_SUBS = List.of("start", "stop", "set", "power", "give", "reroll", "info", "list");
    private static final List<String> PUBLIC_SUBS = List.of("info", "list");
    private static final List<String> PLAYER_SUBS = List.of("set", "power", "give", "reroll");

    private final ThiefSMP plugin;

    public ThiefCommand(ThiefSMP plugin) {
        this.plugin = plugin;
    }

    private void send(CommandSender s, String text, NamedTextColor color) {
        s.sendMessage(Component.text(text, color));
    }

    private boolean isAdmin(CommandSender s) {
        if (s.hasPermission("thiefsmp.admin")) return true;
        send(s, "You need to be an operator to do that.", NamedTextColor.RED);
        return false;
    }

    private Player target(CommandSender s, String name) {
        Player t = Bukkit.getPlayerExact(name);
        if (t == null) send(s, "Player '" + name + "' is not online.", NamedTextColor.RED);
        return t;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        HeartManager hm = plugin.hearts();

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                if (!isAdmin(sender)) return true;
                hm.setStarted(true);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    hm.assignRandom(p);
                }
                hm.save();
                Bukkit.broadcast(Component.text("Thief SMP has begun! Everyone received a random Heart.", NamedTextColor.GOLD));
            }
            case "stop" -> {
                if (!isAdmin(sender)) return true;
                hm.setStarted(false);
                hm.clearAll();
                hm.save();
                Bukkit.broadcast(Component.text("Thief SMP has ended. All Hearts were removed.", NamedTextColor.GOLD));
            }
            case "set" -> {
                if (!isAdmin(sender)) return true;
                if (args.length < 3) {
                    send(sender, "Usage: /thiefsmp set <player> <heart>", NamedTextColor.RED);
                    return true;
                }
                Player t = target(sender, args[1]);
                if (t == null) return true;
                HeartType type = HeartType.fromString(args[2]);
                if (type == null) {
                    send(sender, "Unknown heart. Use /thiefsmp list.", NamedTextColor.RED);
                    return true;
                }
                HeartManager.Data d = hm.get(t.getUniqueId());
                hm.set(t.getUniqueId(), type, d == null ? 1 : d.power);
                hm.announce(t, type);
                hm.save();
                send(sender, "Set " + t.getName() + " to the " + type.displayName() + " Heart.", NamedTextColor.GREEN);
            }
            case "power" -> {
                if (!isAdmin(sender)) return true;
                if (args.length < 3) {
                    send(sender, "Usage: /thiefsmp power <player> <1-" + hm.maxPower() + ">", NamedTextColor.RED);
                    return true;
                }
                Player t = target(sender, args[1]);
                if (t == null) return true;
                HeartManager.Data d = hm.get(t.getUniqueId());
                if (d == null) {
                    send(sender, t.getName() + " has no Heart yet.", NamedTextColor.RED);
                    return true;
                }
                int n;
                try {
                    n = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    send(sender, "Power must be a number.", NamedTextColor.RED);
                    return true;
                }
                hm.set(t.getUniqueId(), d.type, n);
                hm.save();
                send(sender, t.getName() + " is now power " + hm.get(t.getUniqueId()).power + "/" + hm.maxPower() + ".", NamedTextColor.GREEN);
            }
            case "give" -> {
                if (!isAdmin(sender)) return true;
                if (args.length < 3) {
                    send(sender, "Usage: /thiefsmp give <player> <heart|base>", NamedTextColor.RED);
                    return true;
                }
                Player t = target(sender, args[1]);
                if (t == null) return true;
                ItemStack item;
                if (args[2].equalsIgnoreCase("base")) {
                    item = plugin.items().baseHeart();
                } else {
                    HeartType type = HeartType.fromString(args[2]);
                    if (type == null) {
                        send(sender, "Unknown heart. Use /thiefsmp list.", NamedTextColor.RED);
                        return true;
                    }
                    item = plugin.items().heart(type);
                }
                t.getInventory().addItem(item).values()
                        .forEach(leftover -> t.getWorld().dropItemNaturally(t.getLocation(), leftover));
                send(sender, "Gave " + t.getName() + " a Heart item.", NamedTextColor.GREEN);
            }
            case "reroll" -> {
                if (!isAdmin(sender)) return true;
                if (args.length < 2) {
                    send(sender, "Usage: /thiefsmp reroll <player>", NamedTextColor.RED);
                    return true;
                }
                Player t = target(sender, args[1]);
                if (t == null) return true;
                hm.assignRandom(t);
                hm.save();
                send(sender, "Rerolled " + t.getName() + "'s Heart.", NamedTextColor.GREEN);
            }
            case "info" -> {
                Player t = null;
                if (args.length > 1) {
                    t = target(sender, args[1]);
                    if (t == null) return true;
                } else if (sender instanceof Player self) {
                    t = self;
                }
                if (t == null) {
                    send(sender, "Usage: /thiefsmp info <player>", NamedTextColor.RED);
                    return true;
                }
                HeartManager.Data d = hm.get(t.getUniqueId());
                if (d == null) {
                    send(sender, t.getName() + " has no Heart.", NamedTextColor.GRAY);
                } else {
                    sender.sendMessage(Component.text(t.getName() + ": " + d.type.displayName() + " Heart, power "
                            + d.power + "/" + hm.maxPower(), d.type.color()));
                    send(sender, d.type.description(), NamedTextColor.GRAY);
                }
            }
            case "list" -> {
                send(sender, "--- Hearts ---", NamedTextColor.GOLD);
                for (HeartType type : HeartType.values()) {
                    sender.sendMessage(Component.text(type.displayName() + ": ", type.color())
                            .append(Component.text(type.description(), NamedTextColor.GRAY)));
                }
            }
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender s) {
        send(s, "--- Thief SMP ---", NamedTextColor.GOLD);
        send(s, "/thiefsmp info [player]  - show a heart", NamedTextColor.YELLOW);
        send(s, "/thiefsmp list           - list all hearts", NamedTextColor.YELLOW);
        if (s.hasPermission("thiefsmp.admin")) {
            send(s, "/thiefsmp start          - give everyone a random heart", NamedTextColor.YELLOW);
            send(s, "/thiefsmp stop           - remove all hearts", NamedTextColor.YELLOW);
            send(s, "/thiefsmp set <player> <heart>", NamedTextColor.YELLOW);
            send(s, "/thiefsmp power <player> <n>", NamedTextColor.YELLOW);
            send(s, "/thiefsmp give <player> <heart|base>", NamedTextColor.YELLOW);
            send(s, "/thiefsmp reroll <player>", NamedTextColor.YELLOW);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        boolean admin = sender.hasPermission("thiefsmp.admin");

        if (args.length == 1) {
            return filter(admin ? ALL_SUBS : PUBLIC_SUBS, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (sub.equals("info") || (admin && PLAYER_SUBS.contains(sub)))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return filter(names, args[1]);
        }

        if (args.length == 3 && admin) {
            List<String> options = new ArrayList<>();
            switch (sub) {
                case "set" -> addHeartNames(options);
                case "give" -> {
                    addHeartNames(options);
                    options.add("base");
                }
                case "power" -> {
                    for (int i = 1; i <= plugin.hearts().maxPower(); i++) options.add(String.valueOf(i));
                }
                default -> { }
            }
            return filter(options, args[2]);
        }
        return List.of();
    }

    private void addHeartNames(List<String> out) {
        for (HeartType t : HeartType.values()) out.add(t.key());
    }

    private List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) out.add(o);
        }
        return out;
    }
}
