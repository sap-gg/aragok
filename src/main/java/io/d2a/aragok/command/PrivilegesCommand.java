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
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.metadata.NodeMetadataKey;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public record PrivilegesCommand(LuckPerms luckPerms) {

    /**
     * Metadata stored on the temporary admin node
     */
    public record PrivilegeInfo(@Nullable UUID uuid) {
    }

    public static final String INFO_KEY = "aragok:info";
    public static final String USE_PERMISSION = "aragok.privileges.use";
    public static final String USE_OTHER_PERMISSION = "aragok.privileges.use.other";

    public static final String SUPER_USER_GROUP = "su";
    private static final int MAX_PRIVILEGE_DURATION_MINUTES = 15;

    public PrivilegesCommand(final @NotNull LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    /**
     * Executes the /privileges command logic.
     *
     * @param sender          The command sender (player or console).
     * @param target          The target player to grant/revoke privileges.
     * @param durationMinutes The duration in minutes for which to grant privileges.
     */
    private void execute(final CommandSender sender, final Player target, final int durationMinutes) {
        final User user = this.luckPerms.getPlayerAdapter(Player.class).getUser(target);

        final Optional<InheritanceNode> existingNode = findExistingSuperUserGroupNode(user);
        if (existingNode.isPresent()) {
            this.revokePrivileges(sender, target, user, existingNode.get());
            return;
        }

        this.grantPrivileges(sender, target, user, durationMinutes);
    }

    /**
     * Revokes existing super-user privileges from the target player.
     *
     * @param sender The command sender (player or console).
     * @param target The target player whose privileges are to be revoked.
     * @param user   The LuckPerms user object for the target player.
     * @param node   The existing super-user inheritance node to be removed.
     */
    private void revokePrivileges(final CommandSender sender,
                                  final Player target,
                                  final User user,
                                  final InheritanceNode node) {
        final UUID senderUuid = uuidOrNull(sender);

        // make sure we are allowed to revoke the existing super-user node
        if (!(sender instanceof ConsoleCommandSender) && sender != target) { // console can always revoke privileges
            final Optional<PrivilegeInfo> info = node.getMetadata(NodeMetadataKey.of(INFO_KEY, PrivilegeInfo.class));
            if (info.isEmpty() || !isSameUuid(info.get().uuid(), senderUuid)) {
                sender.sendRichMessage("<red>error: <white>" + target.getName() +
                        " <red>is already a super-user! You cannot override their privileges.");
                return;
            }

        }

        // remove the existing super-user node
        user.data().remove(node);
        this.luckPerms.getUserManager().saveUser(user);

        if (sender != target) {
            sender.sendRichMessage("<green>Removed existing super-user privileges from <white>" + target.getName() + "<green>.");
            target.sendRichMessage("<red>Your existing super-user privileges have been removed by <white>" + sender.getName() + "<red>.");
        } else {
            sender.sendRichMessage("<green>Your existing super-user privileges have been removed.");
        }
    }

    /**
     * Grants temporary super-user privileges to the target player.
     *
     * @param sender          The command sender (player or console).
     * @param target          The target player to whom privileges are to be granted.
     * @param user            The LuckPerms user object for the target player.
     * @param durationMinutes The duration in minutes for which to grant privileges.
     */
    private void grantPrivileges(final CommandSender sender,
                                 final Player target,
                                 final User user,
                                 final int durationMinutes) {

        final InheritanceNode node = InheritanceNode.builder(SUPER_USER_GROUP)
                .expiry(durationMinutes, TimeUnit.MINUTES)
                .withMetadata(NodeMetadataKey.of(INFO_KEY, PrivilegeInfo.class), new PrivilegeInfo(uuidOrNull(sender)))
                .build();

        user.data().add(node);
        this.luckPerms.getUserManager().saveUser(user);

        if (sender != target) {
            sender.sendRichMessage("<green>Granted <white>" + target.getName() +
                    " <green>temporary admin privileges for <white>" + durationMinutes + " minutes<green>.");
            target.sendRichMessage("<green>You have been granted temporary admin privileges for <white>" +
                    durationMinutes + " minutes<green> by <white>" + sender.getName() + "<green>!");
        } else {
            sender.sendRichMessage("<green>You have been granted temporary admin privileges for <white>" +
                    durationMinutes + " minutes<green>!");
        }
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
            this.execute(ctx.getSource().getSender(), player, MAX_PRIVILEGE_DURATION_MINUTES);
            return Command.SINGLE_SUCCESS;
        });

        // /privileges <player>
        // gives the specified player admin privileges for a short period of time
        root.then(Commands.argument("player", ArgumentTypes.player())
                .requires(Commands.restricted(source ->
                        source.getSender().hasPermission(USE_OTHER_PERMISSION)))
                .executes(ctx -> {
                    final Player target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                            .resolve(ctx.getSource()).getFirst();
                    this.execute(ctx.getSource().getSender(), target, MAX_PRIVILEGE_DURATION_MINUTES);
                    return Command.SINGLE_SUCCESS;
                })
                // /privileges <player> <duration>
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, MAX_PRIVILEGE_DURATION_MINUTES))
                        .executes(ctx -> {
                            final Player target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                    .resolve(ctx.getSource()).getFirst();
                            final int duration = ctx.getArgument("duration", Integer.class);
                            this.execute(ctx.getSource().getSender(), target, duration);
                            return Command.SINGLE_SUCCESS;
                        }))
                .build()
        );

        return root.build();
    }

    private static @Nullable UUID uuidOrNull(final @Nullable CommandSender sender) {
        if (sender instanceof final Player player) {
            return player.getUniqueId();
        }
        return null;
    }

    private static boolean isSameUuid(final @Nullable UUID a, final @Nullable UUID b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.equals(b);
    }

    private static Optional<InheritanceNode> findExistingSuperUserGroupNode(final User user) {
        return user.getNodes(NodeType.INHERITANCE).stream()
                .filter(Node::getValue)
                .filter(n -> n.getGroupName().equals(SUPER_USER_GROUP))
                .findFirst();
    }

}
