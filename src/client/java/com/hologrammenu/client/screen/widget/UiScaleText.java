package com.hologrammenu.client.screen.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public final class UiScaleText {
	private UiScaleText() {
	}

	public static int lineHeight(Font font) {
		return Math.max(1, Math.round(font.lineHeight * UiScale.TEXT_SCALE));
	}

	public static int width(Font font, Component text) {
		return Math.round(font.width(text.getVisualOrderText()) * UiScale.TEXT_SCALE);
	}

	public static int width(Font font, String text) {
		return Math.round(font.width(text) * UiScale.TEXT_SCALE);
	}

	public static int width(Font font, FormattedCharSequence text) {
		return Math.round(font.width(text) * UiScale.TEXT_SCALE);
	}

	public static void draw(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
		draw(graphics, font, text, x, y, color, true);
	}

	public static void draw(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color, boolean shadow) {
		withScale(graphics, x, y, () -> graphics.text(font, text, x, y, color, shadow));
	}

	public static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
		draw(graphics, font, text, x, y, color, true);
	}

	public static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color, boolean shadow) {
		withScale(graphics, x, y, () -> graphics.text(font, text, x, y, color, shadow));
	}

	public static void drawCentered(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
		withScale(graphics, x, y, () -> graphics.centeredText(font, text, x, y, color));
	}

	public static void item(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int slotSize) {
		float scale = slotSize / (float) UiScale.VANILLA_SLOT;
		if (Math.abs(scale - 1f) < 0.001f) {
			graphics.item(stack, x, y);
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.item(stack, 0, 0);
		graphics.pose().popMatrix();
	}

	public static void withScale(GuiGraphicsExtractor graphics, int anchorX, int anchorY, Runnable draw) {
		float scale = UiScale.TEXT_SCALE;
		if (Math.abs(scale - 1f) < 0.001f) {
			draw.run();
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(anchorX, anchorY);
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(-anchorX, -anchorY);
		draw.run();
		graphics.pose().popMatrix();
	}
}
