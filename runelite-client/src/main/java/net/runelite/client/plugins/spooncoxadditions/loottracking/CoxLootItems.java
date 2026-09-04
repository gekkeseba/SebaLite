package net.runelite.client.plugins.spooncoxadditions.loottracking;

import java.util.EnumSet;
import java.util.HashMap;
import net.runelite.api.gameval.ItemID;

public enum CoxLootItems
{
	TWISTED_BOW(ItemID.TWISTED_BOW, "Twisted bow"),
	TWISTED_BUCKLER(ItemID.TWISTED_BUCKLER, "Twisted buckler"),
	ELDER_MAUL(ItemID.ELDER_MAUL, "Elder maul"),
	DRAGON_CLAWS(ItemID.DRAGON_CLAWS, "Dragon claws"),
	DINHS_BULWARK(ItemID.DINHS_BULWARK, "Dinh's bulwark"),
	ANCESTRAL_HAT(ItemID.ANCESTRAL_HAT, "Ancestral hat"),
	ANCESTRAL_ROBE_TOP(ItemID.ANCESTRAL_ROBE_TOP, "Ancestral robe top"),
	ANCESTRAL_ROBE_BOTTOM(ItemID.ANCESTRAL_ROBE_BOTTOM, "Ancestral robe bottom"),
	ANCESTRAL_HAT_TWISTED(ItemID.ANCESTRAL_HAT_TWISTED, "Twisted ancestral hat"),
	ANCESTRAL_ROBE_TOP_TWISTED(ItemID.ANCESTRAL_ROBE_TOP_TWISTED, "Twisted ancestral robe top"),
	ANCESTRAL_ROBE_BOTTOM_TWISTED(ItemID.ANCESTRAL_ROBE_BOTTOM_TWISTED, "Twisted ancestral robe bottom"),
	KODAI_INSIGNIA(ItemID.KODAI_INSIGNIA, "Kodai insignia"),
	DEXTEROUS_PRAYER_SCROLL(ItemID.RAIDS_PRAYERSCROLL, "Dexterous prayer scroll"),
	ARCANE_PRAYER_SCROLL(ItemID.RAIDS_PRAYERSCROLL_AUGURY, "Arcane prayer scroll");

	private final int itemId;
	private final String itemName;

	private static final HashMap<Integer, String> ITEM_LOOKUP = new HashMap<>();

	static
	{
		EnumSet.allOf(CoxLootItems.class).forEach(item -> ITEM_LOOKUP.put(item.itemId, item.itemName));
	}

	CoxLootItems(int itemId, String itemName)
	{
		this.itemId = itemId;
		this.itemName = itemName;
	}

	public static HashMap<Integer, String> getItemLookup()
	{
		return ITEM_LOOKUP;
	}
}
