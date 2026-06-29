package com.hologrammenu.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class StorageMenuNumberBox {
	public static final int SIZE = UiScale.s(UiScale.NUMBER_BOX_BASE);
	public static final int LABEL_HEIGHT = UiScale.s(8);

	private StorageMenuNumberBox() {
	}

	public static void drawFrame(GuiGraphicsExtractor graphics, int x, int y, boolean highlighted) {
		int fill = highlighted ? 0xFF3D5A8C : 0xFF2A2A2A;
		int border = highlighted ? 0xFF9FC0FF : 0xFF8B8B8B;

		graphics.fill(x, y, x + SIZE, y + SIZE, fill);
		graphics.fill(x, y, x + SIZE, y + 1, border);
		graphics.fill(x, y + SIZE - 1, x + SIZE, y + SIZE, border);
		graphics.fill(x, y, x + 1, y + SIZE, border);
		graphics.fill(x + SIZE - 1, y, x + SIZE, y + SIZE, border);
	}

	public static void drawLabelAbove(GuiGraphicsExtractor graphics, int boxX, int boxY, int number) {
		String label = Integer.toString(number);
		int textX = boxX + (SIZE - UiScaleText.width(Minecraft.getInstance().font, label)) / 2;
		int textY = boxY - LABEL_HEIGHT + 1;
		UiScaleText.draw(graphics, Minecraft.getInstance().font, label, textX, textY, 0xFFFFFF, true);
	}

	public static void draw(GuiGraphicsExtractor graphics, int x, int y, int number, boolean highlighted) {
		drawFrame(graphics, x, y, highlighted);

		String label = Integer.toString(number);
		int textX = x + (SIZE - UiScaleText.width(Minecraft.getInstance().font, label)) / 2;
		int textY = y + (SIZE - UiScaleText.lineHeight(Minecraft.getInstance().font)) / 2;
		UiScaleText.draw(graphics, Minecraft.getInstance().font, label, textX, textY, 0xFFFFFF, true);
	}
}
