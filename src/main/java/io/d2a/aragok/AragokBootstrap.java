package io.d2a.aragok;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class AragokBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(final @NotNull BootstrapContext context) {
        context.getLogger().info("Bootstrapping Aragok...");

        // nothing to bootstrap (yet)
    }

}
