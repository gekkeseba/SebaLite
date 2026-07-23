package net.runelite.client.plugins.spoontob;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.OverlayManager;

@Singleton
public abstract class Room
{
	protected final SpoonTobPlugin plugin;
	protected final SpoonTobConfig config;

	@Inject
	protected OverlayManager overlayManager;

	@Inject
	protected Room(SpoonTobPlugin plugin, SpoonTobConfig config)
	{
		this.plugin = plugin;
		this.config = config;
	}

	public void init()
	{
	}

	public void load()
	{
	}

	public void unload()
	{
	}
}
