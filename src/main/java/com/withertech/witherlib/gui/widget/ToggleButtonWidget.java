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

package com.withertech.witherlib.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.withertech.witherlib.util.ScreenUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ToggleButtonWidget extends AbstractButtonWidget
{
	private final Supplier<Boolean> getState;
	private Component text;

	public ToggleButtonWidget(
			int x,
			int y,
			int width,
			int height,
			Component text,
			Runnable onPress,
			Supplier<Boolean> getState
	)
	{
		super(x, y, width, height, onPress);
		this.text = text;
		this.getState = getState;
	}

	public void setText(Component text)
	{
		this.text = text;
	}

	@Override
	protected List<Component> getNarrationMessage()
	{
		return Collections.singletonList(this.text);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
	{
		ScreenUtils.drawButtonBackground(
				poseStack,
				this.x,
				this.y,
				this.width,
				this.height,
				(this.getState.get() ? 5 : 0) / 10f,
				(this.active ? this.isHovered() ? 5 : 0 : 10) / 15f
		);
		ScreenUtils.drawCenteredStringWithShadow(
				poseStack,
				Minecraft.getInstance().font,
				this.text,
				this.x + this.width / 2f,
				this.y + this.height / 2f - 5,
				this.active ? 0xFFFFFFFF : Integer.MAX_VALUE
		);
	}
}
