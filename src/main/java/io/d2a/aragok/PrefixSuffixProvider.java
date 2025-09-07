package io.d2a.aragok;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public interface PrefixSuffixProvider {

    MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    String prefix(final Player player);

    String suffix(final Player player);

    default Component prefixComponent(final Player player) {
        final String rawPrefix = this.prefix(player);
        return rawPrefix.contains("<")
                ? MINI_MESSAGE.deserialize(rawPrefix)
                : LEGACY_SERIALIZER.deserialize(rawPrefix);
    }

    default Component suffixComponent(final Player player) {
        final String rawSuffix = this.suffix(player);
        return rawSuffix.contains("<")
                ? MINI_MESSAGE.deserialize(rawSuffix)
                : LEGACY_SERIALIZER.deserialize(rawSuffix);
    }

}
