package net.runelite.client.plugins.spooncoxadditions.loottracking;

import java.text.DecimalFormat;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.spooncoxadditions.SpoonCoxAdditionsConfig;

// Tracks points and raids since your last CoX unique, combined across regular and Challenge Mode raids
// (points scale with raid difficulty, so a shared points total is the fair way to combine the two).
// Detection mirrors what the RuneLite core Raids/LootTracker plugins already do: read personal points
// off the "raid complete" chat message, then read the reward chest's item container when it opens.
public class CoxDryLootHandler
{
	private static final DecimalFormat POINTS_FORMAT = new DecimalFormat("#,###");
	private static final String RAID_COMPLETE_MESSAGE = "congratulations - your raid is complete!";

	private static final String KEY_POINTS = "dryLootPoints";
	private static final String KEY_RAIDS = "dryLootRaids";
	private static final String KEY_LAST_ITEM = "dryLootLastItem";

	private final Client client;
	private final ConfigManager configManager;
	private final ChatMessageManager chatMessageManager;
	private final SpoonCoxAdditionsConfig config;

	private boolean chestHandled;
	private boolean pendingPointsCaptured;
	private int pendingPersonalPoints;

	public CoxDryLootHandler(Client client, ConfigManager configManager, ChatMessageManager chatMessageManager, SpoonCoxAdditionsConfig config)
	{
		this.client = client;
		this.configManager = configManager;
		this.chatMessageManager = chatMessageManager;
		this.config = config;
	}

	public void reset()
	{
		this.chestHandled = false;
		this.pendingPointsCaptured = false;
		this.pendingPersonalPoints = 0;
	}

	// Called with every standardized (lowercased) raid-instance chat message; only reacts to the raid
	// completion line, mirroring how the core Raids plugin's points message is triggered.
	public void onChatMessage(String standardizedMessage)
	{
		if (standardizedMessage.startsWith(RAID_COMPLETE_MESSAGE))
		{
			this.pendingPersonalPoints = this.client.getVarpValue(VarPlayerID.RAIDS_PLAYERSCORE);
			this.pendingPointsCaptured = true;
		}
	}

	public void onWidgetLoaded(int groupId)
	{
		if (groupId != InterfaceID.RAIDS_REWARDS || this.chestHandled)
		{
			return;
		}
		this.chestHandled = true;

		ItemContainer container = this.client.getItemContainer(InventoryID.RAIDS_REWARDS);
		if (container == null)
		{
			return;
		}

		String purpleItem = null;
		for (Item item : container.getItems())
		{
			if (item.getId() <= 0)
			{
				continue;
			}
			String name = CoxLootItems.getItemLookup().get(item.getId());
			if (name != null)
			{
				purpleItem = name;
				break;
			}
		}

		processRaidResult(purpleItem);
	}

	private void processRaidResult(String purpleItem)
	{
		int priorPoints = getPersistedInt(KEY_POINTS);
		int priorRaids = getPersistedInt(KEY_RAIDS);
		String lastItem = this.configManager.getRSProfileConfiguration(SpoonCoxAdditionsConfig.GROUP_NAME, KEY_LAST_ITEM);
		int thisRaidPoints = this.pendingPointsCaptured ? this.pendingPersonalPoints : 0;

		if (purpleItem != null)
		{
			announce(true, priorPoints, priorRaids, purpleItem);
			this.configManager.setRSProfileConfiguration(SpoonCoxAdditionsConfig.GROUP_NAME, KEY_POINTS, 0);
			this.configManager.setRSProfileConfiguration(SpoonCoxAdditionsConfig.GROUP_NAME, KEY_RAIDS, 0);
			this.configManager.setRSProfileConfiguration(SpoonCoxAdditionsConfig.GROUP_NAME, KEY_LAST_ITEM, purpleItem);
		}
		else
		{
			int newPoints = priorPoints + thisRaidPoints;
			int newRaids = priorRaids + 1;
			announce(false, newPoints, newRaids, lastItem);
			this.configManager.setRSProfileConfiguration(SpoonCoxAdditionsConfig.GROUP_NAME, KEY_POINTS, newPoints);
			this.configManager.setRSProfileConfiguration(SpoonCoxAdditionsConfig.GROUP_NAME, KEY_RAIDS, newRaids);
		}
	}

	private int getPersistedInt(String key)
	{
		Integer value = this.configManager.getRSProfileConfiguration(SpoonCoxAdditionsConfig.GROUP_NAME, key, int.class);
		return value != null ? value : 0;
	}

	private void announce(boolean isPurple, int points, int raids, String lastItem)
	{
		if (!this.config.dryLootTracking())
		{
			return;
		}

		ChatMessageBuilder builder = new ChatMessageBuilder();

		if (isPurple)
		{
			builder.append(ChatColorType.NORMAL)
				.append("You received ")
				.append(ChatColorType.HIGHLIGHT)
				.append(lastItem)
				.append(ChatColorType.NORMAL)
				.append("! Your dry streak of ")
				.append(ChatColorType.HIGHLIGHT)
				.append(POINTS_FORMAT.format(points))
				.append(ChatColorType.NORMAL)
				.append(" points across ")
				.append(ChatColorType.HIGHLIGHT)
				.append(raids + " raids")
				.append(ChatColorType.NORMAL)
				.append(" has ended.");
		}
		else
		{
			builder.append(ChatColorType.NORMAL)
				.append("No purple this raid. Dry streak: ")
				.append(ChatColorType.HIGHLIGHT)
				.append(POINTS_FORMAT.format(points))
				.append(ChatColorType.NORMAL)
				.append(" points across ")
				.append(ChatColorType.HIGHLIGHT)
				.append(raids + " raids")
				.append(ChatColorType.NORMAL)
				.append(lastItem != null ? " (last: " + lastItem + ")" : "");
		}

		this.chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.FRIENDSCHATNOTIFICATION)
			.runeLiteFormattedMessage(builder.build())
			.build());
	}
}
