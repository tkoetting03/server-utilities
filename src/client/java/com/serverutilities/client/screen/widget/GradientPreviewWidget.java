package com.serverutilities.client.screen.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;

public class GradientPreviewWidget extends AbstractWidget {
	private final IntSupplier startColor;
	private final IntSupplier endColor;

	public GradientPreviewWidget(int x, int y, int width, int height, IntSupplier startColor, IntSupplier endColor) {
		super(x, y, width, height, Component.empty());
		this.startColor = startColor;
		this.endColor = endColor;
		this.active = false;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int start = startColor.getAsInt() & 0xFFFFFF;
		int end = endColor.getAsInt() & 0xFFFFFF;
		int left = getX();
		int top = getY();
		int right = left + width;
		int bottom = top + height;

		for (int x = 0; x < width; x++) {
			float t = width <= 1 ? 0.0F : x / (float) (width - 1);
			int rgb = lerpRgb(start, end, t);
			graphics.fill(left + x, top, left + x + 1, bottom, 0xFF000000 | rgb);
		}

		graphics.fill(left, top, right, top + 1, 0xFF808080);
		graphics.fill(left, bottom - 1, right, bottom, 0xFF404040);
		graphics.fill(left, top, left + 1, bottom, 0xFF808080);
		graphics.fill(right - 1, top, right, bottom, 0xFF404040);
	}

	private static int lerpRgb(int start, int end, float t) {
		int sr = (start >> 16) & 0xFF;
		int sg = (start >> 8) & 0xFF;
		int sb = start & 0xFF;
		int er = (end >> 16) & 0xFF;
		int eg = (end >> 8) & 0xFF;
		int eb = end & 0xFF;
		int r = Math.round(sr + (er - sr) * t);
		int g = Math.round(sg + (eg - sg) * t);
		int b = Math.round(sb + (eb - sb) * t);
		return (r << 16) | (g << 8) | b;
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
	}
}
