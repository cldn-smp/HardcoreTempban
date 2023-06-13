package pro.cloudnode.smp.hardcoretempban.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import pro.cloudnode.smp.hardcoretempban.HardcoreTempban;

public final class PlayerCommandPreprocessEventListener implements Listener {
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (HardcoreTempban.getInstance().getConfig().contains("ban.jail.disable-commands") && HardcoreTempban.getInstance().getConfig().getBoolean("ban.jail.disable-commands") && HardcoreTempban.getInstance().deadPlayers().containsKey(player.getUniqueId()))
            event.setCancelled(true);
    }
}
