package pro.cloudnode.smp.hardcoretempban;

import io.papermc.paper.plugin.configuration.PluginMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class HardcoreTempbanCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            final PluginMeta pluginMeta = HardcoreTempban.getInstance().getPluginMeta();
            this.sendMessage(sender, "info", new HashMap<>() {{
                put("name", pluginMeta.getName());
                put("description", pluginMeta.getDescription());
                put("version", pluginMeta.getVersion());
                put("authors", String.join(", ", pluginMeta.getAuthors()));
                put("contributors", String.join(", ", pluginMeta.getContributors()));
                put("website", pluginMeta.getWebsite());
            }});
        }
        else switch (args[0]) {
            case "reload" -> {
                if (!sender.hasPermission("hardcoretempban.reload")) this.sendMessage(sender, "no-permission");
                else {
                    HardcoreTempban.getInstance().reloadConfig();
                    this.sendMessage(sender, "plugin-reloaded");
                }
            }
            case "kick" -> {
                if (!sender.hasPermission("hardcoretempban.kick")) this.sendMessage(sender, "no-permission");
                else {
                    if (args.length < 2) this.sendMessage(sender, "usage", new HashMap<>() {{
                        put("command", label);
                        put("usage", "kick <dead player> [death-message]");
                    }});
                    else {
                        final Player player = HardcoreTempban.getInstance().getServer().getPlayer(args[1]);
                        if (player == null) this.sendMessage(sender, "player-not-found", new HashMap<>() {{
                            put("player", args[1]);
                        }});
                        else if (!HardcoreTempban.getInstance().deadPlayers().containsKey(player.getUniqueId())) this.sendMessage(sender, "player-not-dead", new HashMap<>() {{
                            put("player", player.getName());
                        }});
                        else {
                            String deathMessage = null;
                            if (args.length > 2) deathMessage = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                            try {
                                HardcoreTempban.getInstance().kick(player, deathMessage);
                            }
                            catch (Exception e) {
                                this.sendMessage(sender, "error", new HashMap<>() {{
                                    put("error", e.getMessage());
                                }});
                            }
                        }
                    }
                }
            }
            case "revive" -> {
                if (!sender.hasPermission("hardcoretempban.revive")) this.sendMessage(sender, "no-permission");
                else {
                    if (args.length != 3) this.sendMessage(sender, "usage", new HashMap<>() {{
                        put("command", label);
                        put("usage", "revive <dead player UUID>");
                    }});
                    else {
                        try {
                            final DeadPlayer deadPlayer = HardcoreTempban.getInstance().deadPlayers().get(UUID.fromString(args[1]));
                            if (deadPlayer == null || !deadPlayer.isBanned())
                                this.sendMessage(sender, "player-not-dead", new HashMap<>() {{
                                    put("player", args[1]);
                                }});
                            else {
                                --deadPlayer.deathCount;
                                deadPlayer.lastDeath = new Date(deadPlayer.lastDeath.getTime() - deadPlayer.getBanExpiry().getTime());
                                HardcoreTempban.getInstance().deadPlayers().put(deadPlayer.uuid, deadPlayer);
                            }
                        }
                        catch (Exception e) {
                            this.sendMessage(sender, "error", new HashMap<>() {{
                                put("error", e.getMessage());
                            }});
                        }
                    }
                }
            }
            default -> this.sendMessage(sender, "unknown-command", new HashMap<>() {{
                put("command", args[0]);
            }});
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        final ArrayList<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("hardcoretempban.reload")) completions.add("reload");
            if (sender.hasPermission("hardcoretempban.kick")) completions.add("kick");
            if (sender.hasPermission("hardcoretempban.revive")) completions.add("revive");
        }
        else if (args.length == 2) {
            if (args[0].equals("kick") || args[0].equals("revive")) {
                for (Player player : HardcoreTempban.getInstance().getServer().getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) completions.add(player.getName());
                }
            }
        }
        return completions;
    }

    /**
     * Get prefix component
     */
    private String getPrefix() {
        final String prefix = HardcoreTempban.getInstance().getConfig().getString("messages.prefix");
        return prefix == null ? "" : prefix;
    }

    /**
     * Send configured message to user
     * @param sender Command sender whom the message will be sent to
     * @param message The message id in the config
     * @param placeholders Placeholders to be replaced in the message
     */
    private void sendMessage(CommandSender sender, String message, HashMap<String, String> placeholders) {
        final String msg = HardcoreTempban.getInstance().getConfig().getString("messages." + message);
        if (msg == null) sender.sendMessage(Component.text().color(NamedTextColor.RED).content("(!) Message " + message + " not configured!"));
        else {
            ArrayList<TagResolver.@NotNull Single> p = new ArrayList<>(placeholders.entrySet().stream().map(entry -> Placeholder.unparsed(entry.getKey(), entry.getValue())).toList());
            final String prefix = HardcoreTempban.getInstance().getConfig().getString("messages.prefix");
            if (prefix != null) p.add(Placeholder.parsed("prefix", prefix));
            sender.sendMessage(MiniMessage.miniMessage().deserialize(msg, p.toArray(TagResolver.Single[]::new)));
        }
    }

    /**
     * Send configured message to user
     * @param sender Command sender whom the message will be sent to
     * @param message The message id in the config
     */
    private void sendMessage(CommandSender sender, String message) {
        this.sendMessage(sender, message, new HashMap<>());
    }
}
