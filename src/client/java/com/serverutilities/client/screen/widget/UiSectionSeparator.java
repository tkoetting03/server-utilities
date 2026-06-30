package com.serverutilities.client.screen.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class UiSectionSeparator {
	public static final int HEIGHT = UiScale.s(10);

	private static final int LINE_COLOR = 0xFF9A9A9A;
	private static final int LINE_SHADOW = 0xFF3A3A3A;
	private static final int LABEL_COLOR = 0xFFE8E8E8;
	private static final int LABEL_BACKGROUND = 0xF0181818;

	private UiSectionSeparator() {
	}

	public static void draw(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width) {
		int lineThickness = Math.max(1, UiScale.s(2));
		int labelWidth = UiScaleText.width(font, label);
		int gap = UiScale.s(4);
		int labelX = x + Math.max(0, (width - labelWidth) / 2);
		int lineY = y + (HEIGHT - lineThickness) / 2;
		int textY = y + Math.max(0, (HEIGHT - UiScaleText.lineHeight(font)) / 2);

		int leftEnd = labelX - gap;
		if (leftEnd > x) {
			drawLine(graphics, x, leftEnd, lineY, lineThickness);
		}

		int rightStart = labelX + labelWidth + gap;
		if (rightStart < x + width) {
			drawLine(graphics, rightStart, x + width, lineY, lineThickness);
		}

		int padX = UiScale.s(3);
		int padY = UiScale.s(1);
		graphics.fill(
			labelX - padX,
			textY - padY,
			labelX + labelWidth + padX,
			textY + UiScaleText.lineHeight(font) + padY,
			LABEL_BACKGROUND
		);
		UiScaleText.draw(graphics, font, label, labelX, textY, LABEL_COLOR);
	}

	private static void drawLine(GuiGraphicsExtractor graphics, int left, int right, int lineY, int lineThickness) {
		graphics.fill(left, lineY, right, lineY + lineThickness, LINE_COLOR);
		graphics.fill(left, lineY + lineThickness, right, lineY + lineThickness + 1, LINE_SHADOW);
	}
}
