package com.hologrammenu.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class ShopItemPickerPanelWidget extends AbstractWidget {
	public static final int COLS = 9;
	public static final int VISIBLE_ROWS = 6;
	private static final int SLOT_SIZE = UiScale.s(UiScale.PICKER_SLOT_BASE);

	public static final int CONTENT_TOP = UiScale.s(18);

	private final Component title;

	public ShopItemPickerPanelWidget(int x, int y, Component title) {
		this(x, y, title, false);
	}

	public ShopItemPickerPanelWidget(int x, int y, Component title, boolean confirmSelection) {
		super(x, y, panelWidth(), panelHeight(confirmSelection), title);
		this.title = title;
		this.active = false;
	}

	public static int panelWidth() {
		return COLS * SLOT_SIZE + ModPanelLayout.PANEL_PADDING * 2;
	}

	public static int contentWidth() {
		return panelWidth() - ModPanelLayout.PANEL_PADDING * 2;
	}

	public static int gridHeight() {
		return VISIBLE_ROWS * SLOT_SIZE;
	}

	public static int panelHeight() {
		return panelHeight(false);
	}

	public static int panelHeight(boolean confirmSelection) {
		int fieldHeight = LabeledFieldLayout.FIELD_HEIGHT;
		int labeledRow = LabeledFieldLayout.LABEL_HEIGHT + LabeledFieldLayout.LABEL_GAP + fieldHeight;
		int confirmButtonHeight = confirmSelection ? ModPanelLayout.ROW_GAP + fieldHeight : 0;
		return CONTENT_TOP
			+ labeledRow + ModPanelLayout.SECTION_GAP
			+ gridHeight()
			+ confirmButtonHeight
			+ ModPanelLayout.PANEL_PADDING;
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
			y + UiScale.s(6),
			0xFFFFFF
		);
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
	}
}
