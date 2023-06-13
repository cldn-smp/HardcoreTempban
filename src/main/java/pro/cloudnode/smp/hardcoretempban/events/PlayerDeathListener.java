package pro.cloudnode.smp.hardcoretempban.events;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import pro.cloudnode.smp.hardcoretempban.DeadPlayer;
import pro.cloudnode.smp.hardcoretempban.HardcoreTempban;

import java.util.Calendar;
import java.util.UUID;

public final class PlayerDeathListener implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final UUID uuid = player.getUniqueId();
        final DeadPlayer deadPlayer = HardcoreTempban.getInstance().deadPlayers().get(uuid) != null
                ? HardcoreTempban.getInstance().deadPlayers().get(uuid)
                : new DeadPlayer(uuid);
        ++deadPlayer.deathCount;
        deadPlayer.lastDeath = Calendar.getInstance().getTime();
        final String deathMessage = event.deathMessage() == null ? "" : PlainTextComponentSerializer.plainText().serialize(event.deathMessage());
        HardcoreTempban.getInstance().deadPlayers().put(uuid, deadPlayer);
        final String banType = HardcoreTempban.getInstance().getConfig().getString("ban.type");
        if (banType == null) throw new RuntimeException("Ban type is not set in config.yml");
        try {
            switch (banType) {
                case "ban" -> HardcoreTempban.getInstance().kick(player, deathMessage);
                case "jail" -> HardcoreTempban.getInstance().sendToJail(player, deathMessage);
                case "command" -> HardcoreTempban.getInstance().runBanCommands(player, deathMessage);
                default -> HardcoreTempban.getInstance().getLogger().warning("Unknown ban type: " + banType);
            }
        }
        catch (Exception e) {
            HardcoreTempban.getInstance().getLogger().warning("Could not ban player: " + e.getMessage());
        }
    }
}
