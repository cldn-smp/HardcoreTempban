package pro.cloudnode.smp.hardcoretempban;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pro.cloudnode.smp.hardcoretempban.events.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class HardcoreTempban extends JavaPlugin {

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        for (DeadPlayer deadPlayer : DeadPlayer.load()) deadPlayers.put(deadPlayer.uuid, deadPlayer);

        Bukkit.getPluginManager().registerEvents(new AsyncChatEventListener(), this);
        Bukkit.getPluginManager().registerEvents(new EntityDamageEventListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerCommandPreprocessEventListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinEventListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerMoveEventListener(), this);
    }

    @Override
    public void onDisable() {
        DeadPlayer.save(deadPlayers.values().stream().toList());
    }

    /**
     * Dead players
     */
    private final HashMap<UUID, DeadPlayer> deadPlayers = new HashMap<>();

    public HashMap<UUID, DeadPlayer> deadPlayers() {
        return deadPlayers;
    }

    /**
     * Get plugin instance
     */
    public static HardcoreTempban getInstance() {
        return JavaPlugin.getPlugin(HardcoreTempban.class);
    }

    /**
     * Get jail location
     */
    public Location getJailLocation(Player player) throws Exception {
        final String worldName = this.getConfig().getString("ban.jail.world");
        final World world = worldName != null ? Bukkit.getWorld(worldName) : player.getWorld();
        if (world == null) throw new Exception("World not found. Is it loaded?");
        final int x = this.getConfig().getInt("ban.jail.x");
        final int z = this.getConfig().getInt("ban.jail.z");
        int y = this.getConfig().contains("ban.jail.y") ? this.getConfig().getInt("ban.jail.y") : this.getHighestBlockY(world, x, z);
        return this.getConfig().contains("ban.jail.yaw") && this.getConfig().contains("ban.jail.pitch")
                ? new Location(world, x, y, z, this.getConfig().getInt("ban.jail.yaw"), this.getConfig().getInt("ban.jail.pitch"))
                : new Location(world, x, y, z);
    }

    /**
     * Send player to "jail"
     * @param player Player to send to jail
     * @param deathMessage Death message
     */
    public void sendToJail(Player player, String deathMessage) throws Exception {
        this.teleportToJail(player);
        final String message = this.getConfig().getString("ban.jail.message");
        if (message != null) player.sendMessage(this.formatMessage(message, player, deathMessage));
        if (this.getConfig().contains("ban.jail.vanish") && this.getConfig().getBoolean("ban.jail.vanish")) this.vanish(player);
    }

    /**
     * Teleport player to jail
     * @param player Player to teleport
     */
    public void teleportToJail(Player player) throws Exception {
        final String server = this.getConfig().getString("ban.jail.server");
        if (server != null) {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteArray);
            out.writeUTF("Connect");
            out.writeUTF(server);
        }
        else player.teleport(this.getJailLocation(player));
    }

    /**
     * Check if player is in jail
     * @param player Player to check
     */
    public boolean isInJail(Player player) throws Exception {
        if (this.getConfig().contains("ban.jail.server")) return false;
        final Location jailLocation = this.getJailLocation(player);
        final Location playerLocation = player.getLocation();
        return jailLocation.getWorld().equals(playerLocation.getWorld())
                && jailLocation.getBlockX() == playerLocation.getBlockX()
                && jailLocation.getBlockY() == playerLocation.getBlockY()
                && jailLocation.getBlockZ() == playerLocation.getBlockZ();
    }

    /**
     * Kick player from server
     * @param player Player to kick
     * @param deathMessage Death message
     */
    public void kick(Player player, String deathMessage) throws Exception {
        final String kickMessageString = this.getConfig().getString("ban.ban.message");
        if (kickMessageString == null) throw new Exception("Ban message not configured");
        final Component kickMessage = this.formatMessage(kickMessageString, player, deathMessage);
        player.kick(kickMessage);
    }

    /**
     * Run ban commands
     * @param player Player to run command as
     * @param deathMessage Death message
     */
    public void runBanCommands(Player player, String deathMessage) throws Exception {
        final List<String> commands = this.getConfig().getStringList("ban.command.command");
        if (commands.size() == 0) throw new Exception("Ban commands not configured");
        for (String command : commands) {
            final Component component = this.formatMessage(command, player, deathMessage);
            final String formattedCommand = PlainTextComponentSerializer.plainText().serialize(component);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
        }
    }

    /**
     * Format message
     * Available placeholders:
     *   - <player>:    The player's name.
     *   - <death-msg>: The death message.
     *   - <duration>:  Ban duration (e.g. "2h 30m 10s").
     *   - <next-duration>: Ban duration of the next ban
     *   - <time>:      Unban time (e.g. "Sun 11 Jun 2023 05:29").
     *   - <deaths>:    Number of deaths (e.g. "3").
     *   - <nth-death>: Ordinal number of the death (e.g. "3rd").
     *
     * @param message Message to format
     * @param player Player to format message for
     * @param deathMessage Death message
     * @return Formatted message
     */
    private Component formatMessage(String message, Player player, String deathMessage) throws Exception {
        final DeadPlayer deadPlayer = this.deadPlayers().get(player.getUniqueId());
        if (deadPlayer == null) throw new Exception("Player not found in dead players list");
        return MiniMessage.miniMessage().deserialize(message,
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("death-msg", deathMessage),
                Placeholder.unparsed("duration", this.formatDuration(DeadPlayer.getBanDuration(deadPlayer.deathCount))),
                Placeholder.unparsed("next-duration", this.formatDuration(DeadPlayer.getBanDuration(deadPlayer.deathCount + 1))),
                Placeholder.unparsed("time", new SimpleDateFormat("E dd MMM yyyy HH:mm:ss 'UTC'", Locale.ENGLISH).format(deadPlayer.getBanExpiry())),
                Placeholder.unparsed("deaths", String.valueOf(deadPlayer.deathCount)),
                Placeholder.unparsed("nth-death", HardcoreTempban.getInstance().ordinal(deadPlayer.deathCount))
        );
    }

    /**
     * Vanish player
     * @param player Player to vanish
     */
    public void vanish(Player player) {
        for (Player p : Bukkit.getOnlinePlayers()) if (p.canSee(player)) p.hidePlayer(this, player);
    }

    /**
     * Hide all online vanished players for a player
     * @param player Player to hide vanished players for (NOT the player to hide)
     */
    public void hideVanishedPlayers(Player player) {
        for (Player p : Bukkit.getOnlinePlayers().stream().filter(p -> this.deadPlayers().containsKey(p.getUniqueId())).toList())
            if (player.canSee(p)) player.hidePlayer(this, p);
    }

    /**
     * Get the highest block in world at X and Z coordinates
     */
    public int getHighestBlockY(World world, int x, int z) {
        final Chunk chunk = world.getChunkAt(x, z, true);
        if (!chunk.isLoaded()) chunk.load();
        int y = world.getMinHeight();
        while (world.getBlockAt(x, y, z).isEmpty() && y < world.getMaxHeight()) ++y;
        return y;
    }

    /**
     * Format duration in seconds to human-readable format
     * @param durationInSeconds Duration in seconds
     */
    public String formatDuration(int durationInSeconds) {
        if (durationInSeconds < 0) throw new IllegalArgumentException("Duration must be a non-negative value.");

        if (durationInSeconds == 0) return "0s";

        int days = durationInSeconds / (24 * 60 * 60);
        int hours = (durationInSeconds % (24 * 60 * 60)) / (60 * 60);
        int minutes = (durationInSeconds % (60 * 60)) / 60;
        int seconds = durationInSeconds % 60;

        StringBuilder formattedDuration = new StringBuilder();

        if (days > 0) formattedDuration.append(days).append("d ");
        if (hours > 0) formattedDuration.append(hours).append("h ");
        if (minutes > 0) formattedDuration.append(minutes).append("m ");
        if (seconds > 0) formattedDuration.append(seconds).append("s");

        return formattedDuration.length() == 0 ? durationInSeconds + "s" : formattedDuration.toString().trim();
    }

    /**
     * Convert nominal to ordinal number
     * @param number Number to convert
     */
    public String ordinal(int number) {
        if (number >= 11 && number <= 13) return number + "th";
        return switch (number % 10) {
            case 1 -> number + "st";
            case 2 -> number + "nd";
            case 3 -> number + "rd";
            default -> number + "th";
        };
    }
}
