package pro.cloudnode.smp.hardcoretempban.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import pro.cloudnode.smp.hardcoretempban.HardcoreTempban;

public final class PlayerMoveEventListener implements Listener {
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        if (HardcoreTempban.getInstance().deadPlayers().get(player.getUniqueId()) != null) {
            final String banType = HardcoreTempban.getInstance().getConfig().getString("ban.type");
            if (banType != null && banType.equals("jail")) {
                final boolean freeze = HardcoreTempban.getInstance().getConfig().getBoolean("ban.jail.freeze");
                final boolean freezeLook = HardcoreTempban.getInstance().getConfig().getBoolean("ban.jail.freeze-look");
                if (freezeLook) event.setCancelled(true);
                if (freeze || freezeLook) try {
                    if (!HardcoreTempban.getInstance().isInJail(player))
                        HardcoreTempban.getInstance().teleportToJail(player);
                }
                catch (Exception e) {
                    HardcoreTempban.getInstance().getLogger().warning("Could not teleport player to jail: " + e.getMessage());
                }
            }
        }
    }
}
