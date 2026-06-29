package com.hologrammenu.client.screen.widget;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class IconPlaceholderButton extends Button {
	private static final int ICON_PADDING = UiScale.s(4);
	private static final int TEXT_GAP = UiScale.s(4);

	public IconPlaceholderButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
	}

	public static IconPlaceholderButton create(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
		return new IconPlaceholderButton(x, y, width, height, message, onPress);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		extractDefaultSprite(graphics);
		int iconSize = Math.max(UiScale.s(8), Math.min(getHeight() - UiScale.s(6), UiScale.s(12)));
		int iconX = getX() + ICON_PADDING;
		int iconY = getY() + (getHeight() - iconSize) / 2;
		graphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, active ? 0xFF606060 : 0xFF303030);
		graphics.fill(iconX + 1, iconY + 1, iconX + iconSize - 1, iconY + iconSize - 1, active ? 0xFF1F1F1F : 0xFF151515);
		int crossColor = active ? 0xFF8A8A8A : 0xFF555555;
		int centerX = iconX + iconSize / 2;
		int centerY = iconY + iconSize / 2;
		graphics.fill(centerX, iconY + 3, centerX + 1, iconY + iconSize - 3, crossColor);
		graphics.fill(iconX + 3, centerY, iconX + iconSize - 3, centerY + 1, crossColor);

		int textLeft = iconX + iconSize + TEXT_GAP;
		int textRight = getX() + getWidth() - UiScale.s(3);
		int textTop = getY() + 1;
		int textBottom = getY() + getHeight();
		ActiveTextCollector output = graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE);
		output.acceptScrolling(getMessage(), (textLeft + textRight) / 2, textLeft, textRight, textTop, textBottom);
	}
}
