package com.hologrammenu.client.screen.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class NpcHologramStackPanelWidget extends AbstractWidget {
	public NpcHologramStackPanelWidget(int x, int y, int height) {
		super(x, y, TextStylePanelWidget.PANEL_WIDTH, height, Component.empty());
		this.active = false;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xF0181818);
		graphics.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF6A6A6A);
		graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xFF2A2A2A);
		graphics.fill(getX(), getY(), getX() + 1, getY() + height, 0xFF6A6A6A);
		graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, 0xFF2A2A2A);
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
	}
}
