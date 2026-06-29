package com.hologrammenu.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class PresetPickerPanelWidget extends AbstractWidget {
	public static final int COLS = 4;
	public static final int VISIBLE_ROWS = 6;
	private static final int SLOT_SIZE = UiScale.s(UiScale.PRESET_PICKER_SLOT_BASE);
	private static final int CELL_WIDTH = UiScale.s(64);
	private static final int LABEL_HEIGHT = Math.max(1, Math.round(9 * UiScale.TEXT_SCALE));
	private static final int CELL_HEIGHT = SLOT_SIZE + LABEL_HEIGHT + UiScale.s(6);

	public static final int CONTENT_TOP = UiScale.s(22);

	private final Component title;

	public PresetPickerPanelWidget(int x, int y, int width, int height, Component title) {
		super(x, y, width, height, title);
		this.title = title;
		this.active = false;
	}

	public static int slotSize() {
		return SLOT_SIZE;
	}

	public static int cellWidth() {
		return CELL_WIDTH;
	}

	public static int cellHeight() {
		return CELL_HEIGHT;
	}

	public static int gridWidth() {
		return COLS * CELL_WIDTH;
	}

	public static int panelWidth() {
		return gridWidth() + ModPanelLayout.PANEL_PADDING * 2;
	}

	public static int contentWidth() {
		return panelWidth() - ModPanelLayout.PANEL_PADDING * 2;
	}

	public static int gridHeight() {
		return VISIBLE_ROWS * CELL_HEIGHT;
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
			title,
			x + ModPanelLayout.PANEL_PADDING,
			y + UiScale.s(8),
			0xFFFFFF
		);
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
	}
}
