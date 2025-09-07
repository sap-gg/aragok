package io.d2a.aragok.listener;

import io.d2a.aragok.compat.PrefixSuffixProvider;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listens to chat events and modifies the chat format by adding prefixes and suffixes
 * to player messages based on their permissions or roles.
 */
public record ChatListener(PrefixSuffixProvider prefixSuffixProvider) implements Listener {

    @EventHandler
    public void onChat(final AsyncChatEvent event) {
        final Player player = event.getPlayer();

        final Component prefix = this.prefixSuffixProvider.prefixComponent(player);
        final Component suffix = this.prefixSuffixProvider.suffixComponent(player);

        event.renderer((source, displayName, message, viewer) ->
                Component.empty()
                        .append(prefix)
                        .append(displayName.style(s -> s.colorIfAbsent(prefix.color())))
                        .append(suffix)
                        .append(Component.text(": "))
                        .append(message)
        );
    }

}
