package net.runelite.client.plugins.volatileearthadvisor;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "Volatile Earth Advisor",
        description = "Recommends which Volatile Earth to tag (1 = target, 2 = source) to clear max acid under feasible path constraints.",
        tags = {"cox", "acid", "volatile", "advisor"}
)
public class VolatileEarthAdvisorPlugin extends Plugin
{
    @Inject private OverlayManager overlayManager;
    @Inject private EventBus eventBus;

    @Inject private VolatileEarthAdvisorService advisorService;
    @Inject private VolatileEarthAdvisorWorldOverlay worldOverlay;
    @Inject private VolatileEarthAdvisorPanelOverlay panelOverlay;

    @Provides
    VolatileEarthAdvisorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(VolatileEarthAdvisorConfig.class);
    }

    @Override
    protected void startUp()
    {
        eventBus.register(advisorService);  // ensure @Subscribe handlers fire
        advisorService.startUp();
        overlayManager.add(worldOverlay);
        overlayManager.add(panelOverlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(worldOverlay);
        overlayManager.remove(panelOverlay);
        advisorService.shutDown();
        eventBus.unregister(advisorService);
    }
}