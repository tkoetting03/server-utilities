package com.hologrammenu.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class StorageMenuHologramSettingsPanelWidget extends AbstractWidget {
	public static final int PANEL_WIDTH = ModPanelLayout.PANEL_WIDTH;
	public static final int CONTENT_TOP = UiScale.s(20);

	public StorageMenuHologramSettingsPanelWidget(int x, int y, int height) {
		super(x, y, PANEL_WIDTH, height, Component.translatable("screen.hologrammenu.storage_menu.hologram_settings_title"));
		this.active = false;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		int right = x + width;
		int bottom = y + height;

		graphics.fill(x, y, right, bottom, 0xF0181818);
		graphics.fill(x, y, right, y + 1, 0xFF6A6A6A);
		graphics.fill(x, bottom - 1, right, bottom, 0xFF2A2A2A);
		graphics.fill(x, y, x + 1, bottom, 0xFF6A6A6A);
		graphics.fill(right - 1, y, right, bottom, 0xFF2A2A2A);

		UiScaleText.draw(
			graphics,
			Minecraft.getInstance().font,
			getMessage(),
			x + ModPanelLayout.PANEL_PADDING,
			y + UiScale.s(6),
			0xFFFFFF
		);
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
	}
}
