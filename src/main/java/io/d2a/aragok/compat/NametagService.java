package io.d2a.aragok.compat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class NametagService {

    private final Logger logger;

    private final Scoreboard mainScoreboard;
    private final PrefixSuffixProvider prefixSuffixProvider;

    private final Map<UUID, String> playerTeamNames = new HashMap<>();

    public NametagService(
            final Logger logger,
            final Scoreboard mainScoreboard,
            final PrefixSuffixProvider prefixSuffixProvider
    ) {
        this.logger = logger;
        this.mainScoreboard = mainScoreboard;
        this.prefixSuffixProvider = prefixSuffixProvider;
    }

    public void applyTo(final Player player) {
        this.logger.info("Applying nametag to player " + player.getName());

        final String teamName = this.getTeamNameFor(player);

        Team team = this.mainScoreboard.getTeam(teamName);
        if (team == null) {
            this.logger.info("Creating team " + teamName + " for player " + player.getName());
            team = this.mainScoreboard.registerNewTeam(teamName);
        }

        final Component prefix = this.prefixSuffixProvider.prefixComponent(player);
        team.prefix(prefix);
        team.suffix(this.prefixSuffixProvider.suffixComponent(player));

        final @Nullable TextColor prefixColor = prefix.color();
        if (prefixColor != null) {
            team.color(NamedTextColor.nearestTo(prefixColor));
        } else {
            team.color(null);
        }

        if (!team.hasEntity(player)) {
            team.addEntity(player);
            this.logger.fine("Added player " + player.getName() + " to team " + teamName);
        }

        if (player.getScoreboard() != this.mainScoreboard) {
            player.setScoreboard(this.mainScoreboard);
            this.logger.fine("Set main scoreboard for player " + player.getName());
        }
    }

    public void remove(final Player player) {
        this.logger.info("Removing nametag from player " + player.getName());

        final String teamName = this.playerTeamNames.remove(player.getUniqueId());
        if (teamName != null) {
            this.logger.info("Removing player " + player.getName() + " from team " + teamName);

            final Team team = this.mainScoreboard.getTeam(teamName);
            if (team != null) {
                this.logger.info("Found team " + teamName + ", removing player " + player.getName());

                team.removeEntity(player);
                if (team.getEntries().isEmpty()) {
                    team.unregister();
                }
            }
        }
    }

    private String getTeamNameFor(final Player player) {
        return this.playerTeamNames.computeIfAbsent(player.getUniqueId(), id ->
                ("aragok-" + id).substring(0, 16));
    }

}
