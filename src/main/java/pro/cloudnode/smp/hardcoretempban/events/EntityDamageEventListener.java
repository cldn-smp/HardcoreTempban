package pro.cloudnode.smp.hardcoretempban.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import pro.cloudnode.smp.hardcoretempban.HardcoreTempban;

public final class EntityDamageEventListener implements Listener {
    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && HardcoreTempban.getInstance().deadPlayers().containsKey(player.getUniqueId()))
            event.setCancelled(true);
    }
}
