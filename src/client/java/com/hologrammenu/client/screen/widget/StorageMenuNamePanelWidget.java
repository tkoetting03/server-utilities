package com.hologrammenu.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public class StorageMenuNamePanelWidget extends AbstractWidget {
	public static final int PANEL_WIDTH = ModPanelLayout.PANEL_WIDTH;
	public static final int CONTENT_TOP = UiScale.s(18);

	public StorageMenuNamePanelWidget(int x, int y) {
		super(x, y, PANEL_WIDTH, panelHeight(), Component.translatable("screen.hologrammenu.storage_menu.name_title"));
		this.active = false;
	}

	public static int panelHeight() {
		int buttonH = UiLayoutHelper.defaultButtonHeight();
		int gap = ModPanelLayout.ROW_GAP;
		return CONTENT_TOP
			+ LabeledFieldLayout.FIELD_HEIGHT + gap
			+ buttonH + gap
			+ buttonH + ModPanelLayout.PANEL_PADDING;
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

		UiScaleText.draw(graphics, Minecraft.getInstance().font, getMessage(), x + ModPanelLayout.PANEL_PADDING, y + UiScale.s(6), 0xFFFFFF);
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
	}
}
