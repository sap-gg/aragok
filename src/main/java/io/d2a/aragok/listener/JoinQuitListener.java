package io.d2a.aragok.listener;

import io.d2a.aragok.compat.NametagService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public record JoinQuitListener(NametagService nametagService) implements Listener {

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        this.nametagService.applyTo(event.getPlayer());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        this.nametagService.remove(event.getPlayer());
    }

}
