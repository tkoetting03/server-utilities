package com.serverutilities.client.screen.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;

public final class UiSelectionOutline {
	private static final int COLOR = 0xFFFFFFFF;

	private UiSelectionOutline() {
	}

	public static void draw(GuiGraphicsExtractor graphics, AbstractWidget widget) {
		draw(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
	}

	/** Matches the on-screen size when the widget body is rendered with a center scale transform. */
	public static void drawForWidget(GuiGraphicsExtractor graphics, AbstractWidget widget, float renderScale) {
		if (renderScale >= 0.999F) {
			draw(graphics, widget);
			return;
		}
		int x = widget.getX();
		int y = widget.getY();
		int width = widget.getWidth();
		int height = widget.getHeight();
		int centerX = x + width / 2;
		int centerY = y + height / 2;
		int scaledWidth = Math.max(1, Math.round(width * renderScale));
		int scaledHeight = Math.max(1, Math.round(height * renderScale));
		draw(graphics, centerX - scaledWidth / 2, centerY - scaledHeight / 2, scaledWidth, scaledHeight);
	}

	public static void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			return;
		}
		int right = x + width;
		int bottom = y + height;
		graphics.fill(x, y, right, y + 1, COLOR);
		graphics.fill(x, bottom - 1, right, bottom, COLOR);
		graphics.fill(x, y, x + 1, bottom, COLOR);
		graphics.fill(right - 1, y, right, bottom, COLOR);
	}
}
