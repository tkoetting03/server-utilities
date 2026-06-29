package com.hologrammenu.client.screen.widget;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class VanillaIconButton extends Button {
	private static final int ICON_PADDING = UiScale.s(3);
	private static final int TEXT_GAP = UiScale.s(3);

	private final ItemStack icon;

	public VanillaIconButton(int x, int y, int width, int height, Component message, ItemStack icon, OnPress onPress) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		this.icon = icon == null ? ItemStack.EMPTY : icon.copy();
	}

	public static VanillaIconButton create(
		int x,
		int y,
		int width,
		int height,
		Component message,
		ItemStack icon,
		OnPress onPress
	) {
		return new VanillaIconButton(x, y, width, height, message, icon, onPress);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		extractDefaultSprite(graphics);
		int iconSize = Math.max(UiScale.s(10), Math.min(getHeight() - UiScale.s(4), UiScale.s(14)));
		int iconX = getX() + ICON_PADDING;
		int iconY = getY() + (getHeight() - iconSize) / 2;
		if (!icon.isEmpty()) {
			UiScaleText.item(graphics, icon, iconX, iconY, iconSize);
		}

		int textLeft = iconX + iconSize + TEXT_GAP;
		int textRight = getX() + getWidth() - UiScale.s(3);
		int textTop = getY() + 1;
		int textBottom = getY() + getHeight();
		ActiveTextCollector output = graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE);
		output.acceptScrolling(getMessage(), (textLeft + textRight) / 2, textLeft, textRight, textTop, textBottom);
	}
}
