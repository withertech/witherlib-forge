/*
 * witherlib-forge
 * Copyright (C) 2021 WitherTech
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.withertech.witherlib.gui;

import com.withertech.witherlib.util.ClientUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * Created 1/26/2021 by SuperMartijn642
 */
public abstract class ItemBaseScreen extends ObjectBaseScreen<ItemStack>
{

	private final Supplier<ItemStack> stackSupplier;

	private ItemBaseScreen(Component title, Supplier<ItemStack> itemStackSupplier)
	{
		super(title);
		this.stackSupplier = itemStackSupplier;
	}

	protected ItemBaseScreen(Component title, int playerSlot)
	{
		this(title, () -> ClientUtils.getPlayer().getInventory().getItem(playerSlot));
	}

	protected ItemBaseScreen(Component title, InteractionHand hand)
	{
		this(title, () -> ClientUtils.getPlayer().getItemInHand(hand));
	}

	@Override
	protected ItemStack getObject()
	{
		return this.stackSupplier.get();
	}
}
