package io.d2a.aragok.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.metadata.NodeMetadataKey;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PrivilegesCommand {

    /**
     * Metadata stored on the temporary admin node
     */
    public record Info(UUID uuid) {
    }

    public static final String INFO_KEY = "aragok:info";
    public static final String USE_PERMISSION = "aragok.privileges.use";
    public static final String USE_OTHER_PERMISSION = "aragok.privileges.use.other";

    public static final String SUPER_USER_GROUP = "su";
    private static final UUID CONSOLE_UUID = new UUID(1L, 0L);
    private static final int MAX_PRIVILEGE_DURATION_MINUTES = 15;

    private LuckPerms getLuckPerms() {
        return LuckPermsProvider.get();
    }

    public PrivilegesCommand() {
    }

    private UUID getUuidFromSender(final CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getUniqueId();
        } else {
            return CONSOLE_UUID;
        }
    }

    private int execute(final CommandSender sender, final Player target, final int durationMinutes) {
        final UUID senderUuid = this.getUuidFromSender(sender);
        final User user = this.getLuckPerms().getPlayerAdapter(Player.class).getUser(target);

        InheritanceNode suGroupNode = null;
        boolean isForeign = true;

        // check if the user already has the super-user group
        for (final InheritanceNode node : user.getNodes(NodeType.INHERITANCE)) {
            if (!node.getGroupName().equals(SUPER_USER_GROUP)) {
                continue;
            }
            if (!node.getValue()) {
                continue;
            }
            suGroupNode = node;

            final Optional<Info> info = node.getMetadata(NodeMetadataKey.of(INFO_KEY, Info.class));
            if (info.isPresent()) {
                if (info.get().uuid.equals(senderUuid) || senderUuid.equals(CONSOLE_UUID)) {
                    isForeign = false;
                }
            }
        }

        // if suGroupNode is not null that means the user already has the super-user group
        if (suGroupNode != null) {
            // only allow removing if the sender is the player that added the su-role initially (or for self)
            if (sender != target && isForeign) {
                sender.sendRichMessage("<red>error: <white>" + target.getName() +
                        " <red>is already a super-user! You cannot override their privileges.");
                return Command.SINGLE_SUCCESS;
            }

            // remove the existing super-user node
            user.data().remove(suGroupNode);
            this.getLuckPerms().getUserManager().saveUser(user);

            if (sender != target) {
                sender.sendRichMessage("<green>Removed existing super-user privileges from <white>" + target.getName() + "<green>.");
                target.sendRichMessage("<red>Your existing super-user privileges have been removed by <white>" + sender.getName() + "<red>.");
            } else {
                sender.sendRichMessage("<green>Your existing super-user privileges have been removed.");
            }

            return Command.SINGLE_SUCCESS;
        }

        final InheritanceNode node = InheritanceNode.builder(SUPER_USER_GROUP)
                .expiry(durationMinutes, TimeUnit.MINUTES)
                .withMetadata(NodeMetadataKey.of(INFO_KEY, Info.class), new Info(this.getUuidFromSender(sender)))
                .build();

        user.data().add(node);
        this.getLuckPerms().getUserManager().saveUser(user);

        if (sender != target) {
            sender.sendRichMessage("<green>Granted <white>" + target.getName() +
                    " <green>temporary admin privileges for <white>" + durationMinutes + " minutes<green>.");
            target.sendRichMessage("<green>You have been granted temporary admin privileges for <white>" +
                    durationMinutes + " minutes<green> by <white>" + sender.getName() + "<green>!");
        } else {
            sender.sendRichMessage("<green>You have been granted temporary admin privileges for <white>" +
                    durationMinutes + " minutes<green>!");
        }

        return Command.SINGLE_SUCCESS;
    }

    public LiteralCommandNode<CommandSourceStack> build(final ComponentLogger logger) {
        final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("privileges")
                .requires(Commands.restricted(source ->
                        source.getSender().hasPermission(USE_PERMISSION)));

        // /privileges
        // gives the current player admin privileges for a short period of time
        root.executes(ctx -> {
            // make sure the sender is a player
            if (!(ctx.getSource().getSender() instanceof Player player)) {
                ctx.getSource().getSender()
                        .sendRichMessage("<red>error: Console must specify a player: <gray>/privileges <player>");
                return Command.SINGLE_SUCCESS;
            }
            return this.execute(ctx.getSource().getSender(), player, MAX_PRIVILEGE_DURATION_MINUTES);
        });

        // /privileges <player>
        // gives the specified player admin privileges for a short period of time
        root.then(Commands.argument("player", ArgumentTypes.player())
                .requires(Commands.restricted(source ->
                        source.getSender().hasPermission(USE_OTHER_PERMISSION)))
                .executes(ctx -> {
                    final Player target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                            .resolve(ctx.getSource()).getFirst();
                    return this.execute(ctx.getSource().getSender(), target, MAX_PRIVILEGE_DURATION_MINUTES);
                })
                // /privileges <player> <duration>
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, MAX_PRIVILEGE_DURATION_MINUTES))
                        .executes(ctx -> {
                            final Player target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                    .resolve(ctx.getSource()).getFirst();
                            final int duration = ctx.getArgument("duration", Integer.class);
                            return this.execute(ctx.getSource().getSender(), target, duration);
                        }))
                .build()
        );

        return root.build();
    }

}
