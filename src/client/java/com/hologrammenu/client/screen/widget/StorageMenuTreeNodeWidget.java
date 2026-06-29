package com.hologrammenu.client.screen.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class StorageMenuTreeNodeWidget extends AbstractWidget {
	private final int inventoryNumber;
	private final boolean selected;
	private final Runnable onSelect;

	public StorageMenuTreeNodeWidget(int x, int y, int inventoryNumber, boolean selected, Runnable onSelect) {
		super(
			x,
			y - StorageMenuNumberBox.LABEL_HEIGHT,
			StorageMenuNumberBox.SIZE,
			StorageMenuNumberBox.SIZE + StorageMenuNumberBox.LABEL_HEIGHT,
			Component.literal(Integer.toString(inventoryNumber))
		);
		this.inventoryNumber = inventoryNumber;
		this.selected = selected;
		this.onSelect = onSelect;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		boolean highlighted = selected || isHovered();
		int boxX = getX();
		int boxY = getY() + StorageMenuNumberBox.LABEL_HEIGHT;
		StorageMenuNumberBox.drawLabelAbove(graphics, boxX, boxY, inventoryNumber);
		StorageMenuNumberBox.drawFrame(graphics, boxX, boxY, highlighted);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		onSelect.run();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
