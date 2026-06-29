package com.hologrammenu.client.screen.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public class ClassicColorSwatchButton extends AbstractWidget {
	private final int color;
	private final Runnable onPress;

	public ClassicColorSwatchButton(int x, int y, int size, int color, Runnable onPress) {
		super(x, y, size, size, Component.empty());
		this.color = 0xFF000000 | color;
		this.onPress = onPress;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		graphics.fill(x, y, x + width, y + height, isHovered() ? 0xFFFFFFFF : 0xFF808080);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
	}

	@Override
	public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
		onPress.run();
	}
}
