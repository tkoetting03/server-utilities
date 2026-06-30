package com.serverutilities.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public class StorageMenuEditorPanelWidget extends AbstractWidget {
	public static final int PANEL_WIDTH = StorageMenuEditorMetrics.PANEL_WIDTH;

	private final int contentHeight;

	public StorageMenuEditorPanelWidget(int x, int y, int contentHeight) {
		super(x, y, PANEL_WIDTH, contentHeight, Component.translatable("screen.serverutilities.storage_menu.title"));
		this.contentHeight = contentHeight;
		this.active = false;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		int right = x + width;
		int bottom = y + contentHeight;

		graphics.fill(x, y, right, bottom, 0xF0181818);
		graphics.fill(x, y, right, y + 1, 0xFF6A6A6A);
		graphics.fill(x, bottom - 1, right, bottom, 0xFF2A2A2A);
		graphics.fill(x, y, x + 1, bottom, 0xFF6A6A6A);
		graphics.fill(right - 1, y, right, bottom, 0xFF2A2A2A);

		UiScaleText.draw(graphics, Minecraft.getInstance().font, getMessage(), x + StorageMenuEditorMetrics.PANEL_PADDING, y + UiScale.s(4), 0xFFFFFF);
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
	}
}
