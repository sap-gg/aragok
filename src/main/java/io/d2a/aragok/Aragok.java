package io.d2a.aragok;

import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Aragok extends JavaPlugin {

    private LuckPerms luckPerms;
    private PrefixSuffixProvider prefixSuffixProvider;
    private NametagService nametagService;

    @Override
    public void onEnable() {
        this.luckPerms = this.getServer().getServicesManager().load(LuckPerms.class);
        if (this.luckPerms == null) {
            this.getLogger().severe("LuckPerms not found! Disabling plugin.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.prefixSuffixProvider = new LuckPermsPrefixSuffixProvider(this.luckPerms);
        this.nametagService = new NametagService(
                this.getServer().getLogger(),
                this.getServer().getScoreboardManager().getMainScoreboard(),
                this.prefixSuffixProvider);

        final PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new ChatListener(this.prefixSuffixProvider), this);
        pluginManager.registerEvents(new JoinQuitListener(this.nametagService), this);

        // listen to LuckPerms user/meta changes
        LuckPermsLiveUpdates.subscribe(this, this.luckPerms, this.nametagService);

        // if reloaded: initialize nametags for all online players
        this.getServer().getOnlinePlayers().forEach(this.nametagService::applyTo);
        this.getLogger().info("Enabled Aragok!");
    }

}
