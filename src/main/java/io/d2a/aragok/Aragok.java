package io.d2a.aragok;

import io.d2a.aragok.command.PrivilegesCommand;
import io.d2a.aragok.compat.LuckPermsPrefixSuffixProvider;
import io.d2a.aragok.compat.NametagService;
import io.d2a.aragok.compat.PrefixSuffixProvider;
import io.d2a.aragok.listener.ChatListener;
import io.d2a.aragok.listener.JoinQuitListener;
import io.d2a.aragok.compat.LuckPermsLiveUpdates;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Aragok extends JavaPlugin {

    @Override
    public void onEnable() {
        final LuckPerms luckPerms = this.getServer().getServicesManager().load(LuckPerms.class);
        if (luckPerms == null) {
            this.getLogger().severe("LuckPerms not found! Disabling plugin.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        final PrefixSuffixProvider prefixSuffixProvider = new LuckPermsPrefixSuffixProvider(luckPerms);
        final NametagService nametagService = new NametagService(
                this.getLogger(),
                this.getServer().getScoreboardManager().getMainScoreboard(),
                prefixSuffixProvider);

        final PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new ChatListener(prefixSuffixProvider), this);
        pluginManager.registerEvents(new JoinQuitListener(nametagService), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            this.getLogger().info("Registering aragok commands...");
            commands.registrar().register(new PrivilegesCommand(luckPerms).build(), "Gain Admin Privileges");
        });

        // listen to LuckPerms user/meta changes
        LuckPermsLiveUpdates.subscribe(this, luckPerms, nametagService);

        // if reloaded: initialize nametags for all online players
        this.getServer().getOnlinePlayers().forEach(nametagService::applyTo);
        this.getLogger().info("Enabled Aragok!");
    }

}
