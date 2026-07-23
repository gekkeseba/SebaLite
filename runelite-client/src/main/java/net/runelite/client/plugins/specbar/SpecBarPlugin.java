/*
 * Copyright (c) 2024, Sebastiaan Hendriks
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.specbar;

import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Spec Bar",
	description = "Forces the special attack bar visible immediately on weapon switch, eliminating the one-tick delay before you can fire spec",
	tags = {"special", "spec", "attack", "bar", "weapon", "switch"},
	enabledByDefault = false
)
public class SpecBarPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	// True when the currently equipped weapon supports a special attack.
	// Updated by onScriptPostFired after COMBAT_INTERFACE_SETUP runs each tick.
	private boolean hasSpec;

	// True from the frame the player clicks Wield/Wear/Equip until the next
	// COMBAT_INTERFACE_SETUP script fires and confirms the new weapon's spec status.
	// Keeps the bar speculatively visible during the round-trip tick delay.
	private boolean speculativeShow;

	@Override
	protected void startUp()
	{
		clientThread.invoke(() ->
		{
			Widget specBar = client.getWidget(InterfaceID.CombatInterface.SP_ATTACKBAR);
			if (specBar != null)
			{
				hasSpec = !specBar.isHidden();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		hasSpec = false;
		speculativeShow = false;
	}

	/**
	 * Detect the moment the player clicks to equip a weapon and set the
	 * speculative flag so we can show the bar for the duration of the tick
	 * delay before the server confirms the weapon change.
	 */
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!event.isItemOp())
		{
			return;
		}
		String option = event.getMenuOption();
		if ("Wield".equals(option) || "Wear".equals(option) || "Equip".equals(option))
		{
			speculativeShow = true;
		}
	}

	/**
	 * After the game's COMBAT_INTERFACE_SETUP script runs it has already set
	 * SP_ATTACKBAR to the correct hidden state for the newly equipped weapon.
	 * We read that state here to update our cache, then clear the speculative
	 * flag — from this point on the real weapon status drives visibility.
	 */
	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() != ScriptID.COMBAT_INTERFACE_SETUP)
		{
			return;
		}

		Widget specBar = client.getWidget(InterfaceID.CombatInterface.SP_ATTACKBAR);
		if (specBar != null)
		{
			hasSpec = !specBar.isHidden();
		}
		speculativeShow = false;
	}

	/**
	 * Every client frame: if the weapon has spec (or we're speculatively
	 * showing during a weapon switch), force the bar visible.
	 *
	 * This runs AFTER ScriptPostFired in the same tick, so it never fights
	 * the game's legitimate hide (a non-spec weapon correctly keeps hasSpec
	 * false from ScriptPostFired, so we never override it here).
	 */
	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!hasSpec && !speculativeShow)
		{
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Widget specBar = client.getWidget(InterfaceID.CombatInterface.SP_ATTACKBAR);
		if (specBar != null)
		{
			specBar.setHidden(false);
		}

		// Also ensure individual children the game may hide separately are visible.
		Widget specButton = client.getWidget(InterfaceID.CombatInterface.SPECIAL_ATTACK);
		if (specButton != null)
		{
			specButton.setHidden(false);
		}

		Widget currentEnergy = client.getWidget(InterfaceID.CombatInterface.CURRENTENERGY);
		if (currentEnergy != null)
		{
			currentEnergy.setHidden(false);
		}
	}
}
