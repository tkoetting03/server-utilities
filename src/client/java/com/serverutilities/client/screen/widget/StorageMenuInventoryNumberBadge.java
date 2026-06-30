package com.serverutilities.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class StorageMenuInventoryNumberBadge extends AbstractWidget {
	private final int inventoryNumber;

	public StorageMenuInventoryNumberBadge(int x, int y, int inventoryNumber) {
		super(
			x,
			y,
			StorageMenuEditorMetrics.CONTENT_WIDTH,
			StorageMenuEditorMetrics.INVENTORY_NUMBER_ROW_HEIGHT,
			Component.translatable("screen.serverutilities.storage_menu.inventory_number", inventoryNumber)
		);
		this.inventoryNumber = inventoryNumber;
		this.active = false;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		StorageMenuNumberBox.draw(graphics, x, y + 1, inventoryNumber, true);
		int labelX = x + StorageMenuNumberBox.SIZE + UiScale.s(6);
		int labelY = y + (StorageMenuNumberBox.SIZE - UiScaleText.lineHeight(Minecraft.getInstance().font)) / 2 + 1;
		UiScaleText.draw(graphics, Minecraft.getInstance().font, getMessage(), labelX, labelY, 0xFFCCCCCC);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
	}
}
