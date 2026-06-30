package com.serverutilities.client.screen.widget;

import com.serverutilities.ServerUtilitiesMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

public class RgbColorPickerWidget extends AbstractWidget {
	private static final AtomicInteger TEXTURE_SEQ = new AtomicInteger();

	private static final int BASE_SWATCH_SIZE = UiScale.s(22);
	private static final int BASE_GAP = UiScale.s(2);
	private static final int BASE_SV_HEIGHT = UiScale.s(66);
	private static final int BASE_HUE_HEIGHT = UiScale.s(10);

	private static final int TEXTURE_SV_WIDTH = 128;
	private static final int TEXTURE_SV_HEIGHT = 64;
	private static final int TEXTURE_HUE_WIDTH = 128;
	private static final int TEXTURE_HUE_HEIGHT = 8;

	private final IntConsumer onColorChanged;
	private final int swatchSize;
	private final int gap;
	private final int svHeight;
	private final int hueHeight;
	private final int pickerWidth;
	private final Identifier svTextureId;
	private final Identifier hueTextureId;
	private final DynamicTexture svTexture;
	private final DynamicTexture hueTexture;
	private final NativeImage svImage;
	private final NativeImage hueImage;

	private float hue;
	private float saturation;
	private float value;
	private boolean svTextureBuilt;
	private DragTarget dragTarget = DragTarget.NONE;

	public static int layoutHeight() {
		return layoutHeight(1.0D);
	}

	public static int layoutHeight(double scale) {
		return scaled(BASE_SV_HEIGHT, scale) + scaled(BASE_HUE_HEIGHT, scale) + scaled(BASE_GAP, scale) + 2;
	}

	public RgbColorPickerWidget(int x, int y, int contentWidth, int initialColor, IntConsumer onColorChanged) {
		this(x, y, contentWidth, initialColor, onColorChanged, 1.0D);
	}

	public RgbColorPickerWidget(int x, int y, int contentWidth, int initialColor, IntConsumer onColorChanged, double scale) {
		super(x, y, contentWidth, layoutHeight(scale), Component.empty());
		this.onColorChanged = onColorChanged;
		this.swatchSize = scaled(BASE_SWATCH_SIZE, scale);
		this.gap = scaled(BASE_GAP, scale);
		this.svHeight = scaled(BASE_SV_HEIGHT, scale);
		this.hueHeight = scaled(BASE_HUE_HEIGHT, scale);
		this.pickerWidth = Math.max(UiScale.s(40), contentWidth - swatchSize - gap);

		svImage = new NativeImage(TEXTURE_SV_WIDTH, TEXTURE_SV_HEIGHT, false);
		hueImage = new NativeImage(TEXTURE_HUE_WIDTH, TEXTURE_HUE_HEIGHT, false);
		svTexture = new DynamicTexture(() -> "serverutilities_sv_picker", svImage);
		hueTexture = new DynamicTexture(() -> "serverutilities_hue_picker", hueImage);

		int seq = TEXTURE_SEQ.incrementAndGet();
		svTextureId = ServerUtilitiesMod.id("dynamic/color_sv_" + seq);
		hueTextureId = ServerUtilitiesMod.id("dynamic/color_hue_" + seq);
		var textureManager = Minecraft.getInstance().getTextureManager();
		textureManager.register(svTextureId, svTexture);
		textureManager.register(hueTextureId, hueTexture);

		buildHueTexture();
		setColor(initialColor);
	}

	private static int scaled(int base, double scale) {
		return Math.max(1, (int) Math.round(base * scale));
	}

	public void setColor(int rgb) {
		float[] hsv = HsvColor.fromRgb(rgb & 0xFFFFFF);
		float newHue = hsv[0];
		if (Math.abs(newHue - hue) > 0.01F || !svTextureBuilt) {
			hue = newHue;
			buildSvTexture();
		} else {
			hue = newHue;
		}
		saturation = hsv[1];
		value = hsv[2];
	}

	public int getColor() {
		return HsvColor.toRgb(hue, saturation, value);
	}

	public void destroy() {
		var textureManager = Minecraft.getInstance().getTextureManager();
		textureManager.release(svTextureId);
		textureManager.release(hueTextureId);
		svTexture.close();
		hueTexture.close();
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		int pickerX = x + swatchSize + gap;
		int pickerY = y;

		int swatchColor = 0xFF000000 | getColor();
		graphics.fill(x, pickerY, x + swatchSize, pickerY + swatchSize, 0xFF707070);
		graphics.fill(x + 1, pickerY + 1, x + swatchSize - 1, pickerY + swatchSize - 1, swatchColor);

		String hex = String.format("#%06X", getColor());
		UiScaleText.draw(graphics, Minecraft.getInstance().font, Component.literal(hex), x, pickerY + swatchSize + 2, 0xA0A0A0);

		graphics.blit(
			RenderPipelines.GUI_TEXTURED,
			svTextureId,
			pickerX,
			pickerY,
			0.0F,
			0.0F,
			pickerWidth,
			svHeight,
			TEXTURE_SV_WIDTH,
			TEXTURE_SV_HEIGHT,
			TEXTURE_SV_WIDTH,
			TEXTURE_SV_HEIGHT
		);

		int hueY = pickerY + svHeight + gap;
		graphics.blit(
			RenderPipelines.GUI_TEXTURED,
			hueTextureId,
			pickerX,
			hueY,
			0.0F,
			0.0F,
			pickerWidth,
			hueHeight,
			TEXTURE_HUE_WIDTH,
			TEXTURE_HUE_HEIGHT,
			TEXTURE_HUE_WIDTH,
			TEXTURE_HUE_HEIGHT
		);

		drawSvCursor(graphics, pickerX, pickerY);
		drawHueCursor(graphics, pickerX, hueY);
	}

	private void drawSvCursor(GuiGraphicsExtractor graphics, int pickerX, int pickerY) {
		int cursorX = pickerX + Math.round(saturation * (pickerWidth - 1));
		int cursorY = pickerY + Math.round((1.0F - value) * (svHeight - 1));
		graphics.fill(cursorX, pickerY, cursorX + 1, pickerY + svHeight, 0x80FFFFFF);
		graphics.fill(pickerX, cursorY, pickerX + pickerWidth, cursorY + 1, 0x80FFFFFF);
	}

	private void drawHueCursor(GuiGraphicsExtractor graphics, int pickerX, int hueY) {
		int cursorX = pickerX + Math.round((hue / 360.0F) * (pickerWidth - 1));
		graphics.fill(cursorX, hueY - 1, cursorX + 1, hueY + hueHeight + 1, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!isActive() || !isMouseOver(event.x(), event.y())) {
			return false;
		}

		if (updateFromMouse(event.x(), event.y())) {
			onColorChanged.accept(getColor());
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragTarget == DragTarget.NONE) {
			return false;
		}

		updateFromMouse(event.x(), event.y());
		onColorChanged.accept(getColor());
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragTarget != DragTarget.NONE) {
			dragTarget = DragTarget.NONE;
			return true;
		}
		return false;
	}

	private boolean updateFromMouse(double mouseX, double mouseY) {
		int pickerX = getX() + swatchSize + gap;
		int pickerY = getY();
		int hueY = pickerY + svHeight + gap;

		if (mouseY >= pickerY && mouseY < pickerY + svHeight
			&& mouseX >= pickerX && mouseX < pickerX + pickerWidth) {
			dragTarget = DragTarget.SV;
			saturation = clamp01((float) ((mouseX - pickerX) / Math.max(1, pickerWidth - 1)));
			value = clamp01(1.0F - (float) ((mouseY - pickerY) / Math.max(1, svHeight - 1)));
			return true;
		}

		if (mouseY >= hueY && mouseY < hueY + hueHeight
			&& mouseX >= pickerX && mouseX < pickerX + pickerWidth) {
			dragTarget = DragTarget.HUE;
			float newHue = clamp01((float) ((mouseX - pickerX) / Math.max(1, pickerWidth - 1))) * 360.0F;
			if (Math.abs(newHue - hue) > 0.01F) {
				hue = newHue;
				buildSvTexture();
			} else {
				hue = newHue;
			}
			return true;
		}

		return false;
	}

	private void buildHueTexture() {
		for (int x = 0; x < TEXTURE_HUE_WIDTH; x++) {
			float hueSample = (x / (float) (TEXTURE_HUE_WIDTH - 1)) * 360.0F;
			int rgb = HsvColor.toRgb(hueSample, 1.0F, 1.0F);
			int pixel = 0xFF000000 | rgb;
			for (int y = 0; y < TEXTURE_HUE_HEIGHT; y++) {
				hueImage.setPixel(x, y, pixel);
			}
		}
		hueTexture.upload();
	}

	private void buildSvTexture() {
		for (int y = 0; y < TEXTURE_SV_HEIGHT; y++) {
			float brightness = 1.0F - (y / (float) (TEXTURE_SV_HEIGHT - 1));
			for (int x = 0; x < TEXTURE_SV_WIDTH; x++) {
				float sat = x / (float) (TEXTURE_SV_WIDTH - 1);
				int rgb = HsvColor.toRgb(hue, sat, brightness);
				svImage.setPixel(x, y, 0xFF000000 | rgb);
			}
		}
		svTexture.upload();
		svTextureBuilt = true;
	}

	private static float clamp01(float value) {
		return Math.clamp(value, 0.0F, 1.0F);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
	}

	private enum DragTarget {
		NONE,
		SV,
		HUE
	}
}
