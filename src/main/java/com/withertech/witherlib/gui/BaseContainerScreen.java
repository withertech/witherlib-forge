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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.withertech.witherlib.WitherLib;
import com.withertech.witherlib.gui.widget.IHoverTextWidget;
import com.withertech.witherlib.gui.widget.ITickableWidget;
import com.withertech.witherlib.gui.widget.TextFieldWidget;
import com.withertech.witherlib.gui.widget.Widget;
import com.withertech.witherlib.util.ClientUtils;
import com.withertech.witherlib.util.ScreenUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.util.InputMappings;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.LinkedList;
import java.util.List;

/**
 * Created 1/20/2021 by SuperMartijn642
 */
public abstract class BaseContainerScreen<T extends BaseContainer> extends ContainerScreen<T>
{

	private static final ResourceLocation SLOT_TEXTURE = WitherLib.INSTANCE.MOD.modLocation(
			"textures/gui/slot.png");
	/**
	 * Have this because it replaced my variable name with an srg name for some reason
	 **/
	@Deprecated
	protected final T field_147002_h;
	protected final T container;
	private final List<Widget> widgets = new LinkedList<>();
	private final List<ITickableWidget> tickableWidgets = new LinkedList<>();
	private boolean drawSlots = true;

	/**
	 * @param screenContainer container the screen will be attached to
	 * @param title           title to be read by the narrator and to be displayed in the gui
	 */
	public BaseContainerScreen(T screenContainer, ITextComponent title)
	{
		super(screenContainer, screenContainer.player.inventory, title);
		this.container = screenContainer;
		this.field_147002_h = screenContainer;
	}

	/**
	 * @return the width of the screen
	 */
	protected abstract int sizeX();

	/**
	 * @return the height of the screen
	 */
	protected abstract int sizeY();

	/**
	 * @return the left edge of the screen
	 */
	protected int left()
	{
		return (this.width - this.sizeX()) / 2;
	}

	/**
	 * @return the top edge of the screen
	 */
	protected int top()
	{
		return (this.height - this.sizeY()) / 2;
	}

	@Override
	public int getXSize()
	{
		return this.sizeX();
	}

	@Override
	public int getYSize()
	{
		return this.sizeY();
	}

	@Override
	public int getGuiLeft()
	{
		return this.left();
	}

	@Override
	public int getGuiTop()
	{
		return this.top();
	}

	/**
	 * Sets whether slots should be drawn by the {@link BaseContainerScreen}.
	 *
	 * @param drawSlots whether slots should be drawn
	 */
	protected void setDrawSlots(boolean drawSlots)
	{
		this.drawSlots = drawSlots;
	}

	@Override
	public void init()
	{
		this.imageWidth = this.sizeX();
		this.imageHeight = this.sizeY();
		super.init();

		this.widgets.clear();
		this.tickableWidgets.clear();
		this.addWidgets();
	}

	/**
	 * Adds widgets to the screen via {@link #addWidget(Widget)}.
	 */
	protected abstract void addWidgets();

	/**
	 * Add the given {@code widget} to the screen.
	 *
	 * @param widget widget to be added
	 * @return the given {@code widget}
	 */
	protected <T extends Widget> T addWidget(T widget)
	{
		this.widgets.add(widget);
		if (widget instanceof ITickableWidget)
		{
			this.tickableWidgets.add((ITickableWidget) widget);
		}
		return widget;
	}

	/**
	 * Removes the given {@code widget} from the screen.
	 *
	 * @param widget widget to be removed
	 * @return the given {@code widget}
	 */
	protected <T extends Widget> T removeWidget(T widget)
	{
		this.widgets.remove(widget);
		if (widget instanceof ITickableWidget)
		{
			this.tickableWidgets.remove(widget);
		}
		return widget;
	}

	@Override
	public void tick()
	{
		for (Widget widget : this.widgets)
		{
			if (widget instanceof ITickableWidget)
			{
				((ITickableWidget) widget).tick();
			}
		}
	}

	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
	{
		this.renderBackground(matrixStack);

		matrixStack.translate(this.left(), this.top(), 0);
		this.renderBackground(matrixStack, mouseX - this.left(), mouseY - this.top());

		if (this.drawSlots)
		{
			for (Slot slot : this.menu.slots)
			{
				Minecraft.getInstance().getTextureManager().bind(SLOT_TEXTURE);
				ScreenUtils.drawTexture(matrixStack, slot.x - 1, slot.y - 1, 18, 18);
			}
		}
		matrixStack.translate(-this.left(), -this.top(), 0);

		super.render(matrixStack, mouseX, mouseY, partialTicks);
		// apparently some OpenGl settings are messed up after this

		RenderSystem.enableAlphaTest();
		GlStateManager._disableLighting();

		matrixStack.translate(this.left(), this.top(), 0);
		for (Widget widget : this.widgets)
		{
			widget.blitOffset = this.getBlitOffset();
			widget.wasHovered = widget.hovered;
			widget.hovered = mouseX - this.left() > widget.x && mouseX - this.left() < widget.x + widget.width &&
					mouseY - this.top() > widget.y && mouseY - this.top() < widget.y + widget.height;
			widget.render(matrixStack, mouseX - this.left(), mouseY - this.top(), partialTicks);
			widget.narrate();
		}

		this.renderForeground(matrixStack, mouseX - this.left(), mouseY - this.top());

		for (Widget widget : this.widgets)
		{
			if (widget instanceof IHoverTextWidget && widget.isHovered())
			{
				List<ITextComponent> text = ((IHoverTextWidget) widget).getHoverText();
				if (text != null)
				{
					this.renderComponentTooltip(matrixStack, text, mouseX - this.left(), mouseY - this.top());
				}
			}
		}
		matrixStack.translate(-this.left(), -this.top(), 0);
		super.renderTooltip(matrixStack, mouseX, mouseY);
		this.renderTooltips(matrixStack, mouseX, mouseY);
	}

	@Override
	protected void renderBg(MatrixStack matrixStack, float partialTicks, int x, int y)
	{
	}

	@Override
	protected void renderLabels(MatrixStack matrixStack, int x, int y)
	{
	}

	/**
	 * Renders the screen's background. This will be called first in the render chain.
	 */
	protected void renderBackground(MatrixStack matrixStack, int mouseX, int mouseY)
	{
		this.drawScreenBackground(matrixStack);
	}

	/**
	 * Renders the screen's foreground.
	 * Widgets are drawn after this.
	 */
	protected void renderForeground(MatrixStack matrixStack, int mouseX, int mouseY)
	{
		ScreenUtils.drawString(matrixStack, this.font, this.title, 8, 7, 4210752);
	}

	/**
	 * Renders tooltips for the given {@code mouseX} and {@code mouseY}.
	 * This will be called last in the render chain.
	 */
	protected void renderTooltips(MatrixStack matrixStack, int mouseX, int mouseY)
	{
	}

	/**
	 * Draws the default screen background.
	 * Same as {@link ScreenUtils#drawScreenBackground(MatrixStack, float, float, float, float)}.
	 */
	protected void drawScreenBackground(MatrixStack matrixStack, float x, float y, float width, float height)
	{
		ScreenUtils.drawScreenBackground(matrixStack, x, y, width, height);
	}

	/**
	 * Draws the default screen background with width {@link #sizeX()} and height {@link #sizeY()}.
	 */
	protected void drawScreenBackground(MatrixStack matrixStack)
	{
		ScreenUtils.drawScreenBackground(matrixStack, 0, 0, this.sizeX(), this.sizeY());
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		mouseX -= this.left();
		mouseY -= this.top();

		this.onMousePress((int) mouseX, (int) mouseY, button);

		for (Widget widget : this.widgets)
		{
			widget.mouseClicked((int) mouseX, (int) mouseY, button);
		}

		mouseX += this.left();
		mouseY += this.top();

		return super.mouseClicked(mouseX, mouseY, button);
	}

	/**
	 * Called whenever a mouse button is pressed down.
	 */
	protected void onMousePress(int mouseX, int mouseY, int button)
	{
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		mouseX -= this.left();
		mouseY -= this.top();

		this.onMouseRelease((int) mouseX, (int) mouseY, button);

		for (Widget widget : this.widgets)
		{
			widget.mouseReleased((int) mouseX, (int) mouseY, button);
		}

		mouseX += this.left();
		mouseY += this.top();

		return super.mouseReleased(mouseX, mouseY, button);
	}

	/**
	 * Called whenever a mouse button is released.
	 */
	protected void onMouseRelease(int mouseX, int mouseY, int button)
	{
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta)
	{
		mouseX -= this.left();
		mouseY -= this.top();

		this.onMouseScroll((int) mouseX, (int) mouseY, delta);

		for (Widget widget : this.widgets)
		{
			widget.mouseScrolled((int) mouseX, (int) mouseY, delta);
		}

		mouseX += this.left();
		mouseY += this.top();

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	/**
	 * Called whenever the user performs a scroll action.
	 */
	protected void onMouseScroll(int mouseX, int mouseY, double scroll)
	{
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if (this.keyPressed(keyCode))
		{
			return true;
		}

		InputMappings.Input key = InputMappings.getKey(keyCode, scanCode);
		if (ClientUtils.getMinecraft().options.keyInventory.isActiveAndMatches(key))
		{
			this.onClose();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	/**
	 * Called whenever a key is pressed down.
	 */
	public boolean keyPressed(int keyCode)
	{
		boolean handled = false;

		for (Widget widget : this.widgets)
		{
			if (widget instanceof TextFieldWidget && ((TextFieldWidget) widget).canWrite())
			{
				handled = true;
			}
			widget.keyPressed(keyCode);
		}

		return handled;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers)
	{
		return this.keyReleased(keyCode);
	}

	/**
	 * Called whenever a key is released.
	 */
	public boolean keyReleased(int keyCode)
	{
		for (Widget widget : this.widgets)
		{
			widget.keyReleased(keyCode);
		}

		return false;
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers)
	{
		return this.charTyped(codePoint);
	}

	/**
	 * Called whenever a character key is released with the given character {@code c}.
	 */
	public boolean charTyped(char c)
	{
		for (Widget widget : this.widgets)
		{
			widget.charTyped(c);
		}

		return false;
	}
}
