package pro.cloudnode.smp.hardcoretempban.events;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pro.cloudnode.smp.hardcoretempban.HardcoreTempban;

public final class AsyncChatEventListener implements Listener {
    @EventHandler
    public void onAsyncChatEvent(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (HardcoreTempban.getInstance().getConfig().contains("ban.jail.mute") && HardcoreTempban.getInstance().getConfig().getBoolean("ban.jail.mute") && HardcoreTempban.getInstance().deadPlayers().containsKey(player.getUniqueId()))
            event.setCancelled(true);
    }
}
