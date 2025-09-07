package io.d2a.aragok;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Listens to LuckPerms user data changes and updates nametags accordingly.
 */
public class LuckPermsLiveUpdates {

    private LuckPermsLiveUpdates() {
    }

    public static void subscribe(final Plugin plugin, final LuckPerms luckPerms, final NametagService nametagService) {
        luckPerms.getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, event -> {
            final Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
            if (player != null && player.isOnline()) {
                nametagService.applyTo(player);
            }
        });
    }

}
