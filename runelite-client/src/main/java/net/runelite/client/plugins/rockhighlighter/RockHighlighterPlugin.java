package net.runelite.client.plugins.rockhighlighter;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
        name = "Rock Highlighter",
        description = "Highlights a specific game object (ID 57286) with a hull",
        tags = {"overlay", "highlight", "object"}
)
public class RockHighlighterPlugin extends Plugin
{
    private static final int OBJECT_ID = 57286;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private RockHighlighterOverlay overlay;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ConfigManager configManager;

    private final List<GameObject> objects = new ArrayList<>();

    // Provide the config implementation for Guice
    @Provides
    RockHighlighterConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(RockHighlighterConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        objects.clear();
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject obj = event.getGameObject();
        if (obj.getId() == OBJECT_ID)
        {
            objects.add(obj);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        objects.remove(event.getGameObject());
    }

    /**
     * Clear tracked objects when a new delve level starts or the duration message appears.
     * ChatMessage#getMessage() returns the raw message text [oai_citation:2‡static.runelite.net](https://static.runelite.net/runelite-api/apidocs/net/runelite/api/events/ChatMessage.html#:~:text=public%C2%A0String%C2%A0getMessage),
     * and ChatMessage#getType() allows us to filter by game messages [oai_citation:3‡static.runelite.net](https://static.runelite.net/runelite-api/apidocs/net/runelite/api/events/ChatMessage.html#:~:text=public%C2%A0ChatMessageType%C2%A0getType).
     */
    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        // Only handle normal game messages
        if (event.getType() == ChatMessageType.GAMEMESSAGE)
        {
            String message = Text.removeTags(event.getMessage());
            // Match both the initial "Delve level: X" and the completion "Delve level: X duration …"
            if (message.startsWith("Delve level:") || message.contains("Oh dear, you are dead!"))
            {
                objects.clear();
            }
        }
    }

    List<GameObject> getObjects()
    {
        return objects;
    }
}