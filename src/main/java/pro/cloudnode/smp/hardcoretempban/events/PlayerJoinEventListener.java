package pro.cloudnode.smp.hardcoretempban.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import pro.cloudnode.smp.hardcoretempban.DeadPlayer;
import pro.cloudnode.smp.hardcoretempban.HardcoreTempban;

public final class PlayerJoinEventListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final DeadPlayer deadPlayer = HardcoreTempban.getInstance().deadPlayers().get(player.getUniqueId());
        if (deadPlayer != null) {
            final String banType = HardcoreTempban.getInstance().getConfig().getString("ban.type");
            try {
                if (banType != null && banType.equals("ban")) HardcoreTempban.getInstance().kick(player, deadPlayer.deathMessage);
            }
            catch (Exception e) {
                HardcoreTempban.getInstance().getLogger().warning("Could not kick banned player: " + e.getMessage());
            }
        }

        HardcoreTempban.getInstance().hideVanishedPlayers(player);
    }
}
