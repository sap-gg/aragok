package io.d2a.aragok.compat;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.entity.Player;

public class LuckPermsPrefixSuffixProvider implements PrefixSuffixProvider {

    private final LuckPerms luckPerms;

    public LuckPermsPrefixSuffixProvider(final LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    private CachedMetaData getMeta(final Player player) {
        return this.luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
    }

    private String emptyIfNull(final String str) {
        if (str == null) {
            return "";
        }
        return str;
    }

    @Override
    public String prefix(final Player player) {
        return this.emptyIfNull(this.getMeta(player).getPrefix());
    }

    @Override
    public String suffix(Player player) {
        return this.emptyIfNull(this.getMeta(player).getSuffix());
    }

}
