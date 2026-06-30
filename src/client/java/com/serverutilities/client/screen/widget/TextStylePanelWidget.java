package com.serverutilities.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public class TextStylePanelWidget extends AbstractWidget {
	public static final int PANEL_WIDTH = TextStylePanelLayout.PANEL_WIDTH;

	public static final int CONTENT_LEFT = TextStylePanelLayout.CONTENT_LEFT;
	public static final int CONTENT_WIDTH = TextStylePanelLayout.CONTENT_WIDTH;

	private final int partCount;
	private final int contentTopOffset;
	private final boolean gradientExpanded;
	private final boolean partsCollapsed;

	public TextStylePanelWidget(int x, int y, int partCount) {
		this(x, y, partCount, 0, false);
	}

	public TextStylePanelWidget(int x, int y, int partCount, int contentTopOffset) {
		this(x, y, partCount, contentTopOffset, false);
	}

	public TextStylePanelWidget(int x, int y, int partCount, int contentTopOffset, boolean gradientExpanded) {
		this(x, y, partCount, contentTopOffset, gradientExpanded, false);
	}

	public TextStylePanelWidget(
		int x,
		int y,
		int partCount,
		int contentTopOffset,
		boolean gradientExpanded,
		boolean partsCollapsed
	) {
		super(
			x,
			y,
			PANEL_WIDTH,
			panelHeight(partCount, contentTopOffset, gradientExpanded, partsCollapsed),
			Component.translatable("screen.serverutilities.text_style.title")
		);
		this.partCount = Math.max(1, partCount);
		this.contentTopOffset = contentTopOffset;
		this.gradientExpanded = gradientExpanded;
		this.partsCollapsed = partsCollapsed;
		this.active = false;
	}

	public int partCount() {
		return partCount;
	}

	public static int panelHeight(int partCount) {
		return panelHeight(partCount, 0, false);
	}

	public static int panelHeight(int partCount, int contentTopOffset) {
		return panelHeight(partCount, contentTopOffset, false);
	}

	public static int panelHeight(int partCount, int contentTopOffset, boolean gradientExpanded) {
		return panelHeight(partCount, contentTopOffset, gradientExpanded, false);
	}

	public static int panelHeight(int partCount, int contentTopOffset, boolean gradientExpanded, boolean partsCollapsed) {
		return TextStylePanelLayout.metrics(partCount, contentTopOffset, gradientExpanded, partsCollapsed).panelHeight(partCount);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		int right = x + width;
		int bottom = y + height;
		TextStylePanelLayout.Metrics layout = TextStylePanelLayout.metrics(
			partCount,
			contentTopOffset,
			gradientExpanded,
			partsCollapsed
		);

		graphics.fill(x, y, right, bottom, 0xF0181818);
		graphics.fill(x, y, right, y + 1, 0xFF6A6A6A);
		graphics.fill(x, bottom - 1, right, bottom, 0xFF2A2A2A);
		graphics.fill(x, y, x + 1, bottom, 0xFF6A6A6A);
		graphics.fill(right - 1, y, right, bottom, 0xFF2A2A2A);

		var font = Minecraft.getInstance().font;
		int contentLeft = x + CONTENT_LEFT;
		UiSectionSeparator.draw(
			graphics,
			font,
			Component.translatable("screen.serverutilities.text_style.colors"),
			contentLeft,
			y + layout.colorsLabelTop(partCount),
			CONTENT_WIDTH
		);
		UiSectionSeparator.draw(
			graphics,
			font,
			Component.translatable("screen.serverutilities.text_style.text_effects"),
			contentLeft,
			y + layout.effectsLabelTop(partCount),
			CONTENT_WIDTH
		);

		int effectsTop = y + layout.effectsTop(partCount);
		int effectsBottom = effectsTop + layout.effectsBlockHeight();
		int effectsLeft = contentLeft;
		int effectsRight = effectsLeft + CONTENT_WIDTH;
		graphics.fill(effectsLeft, effectsTop, effectsRight, effectsTop + 1, 0xFF6A6A6A);
		graphics.fill(effectsLeft, effectsBottom - 1, effectsRight, effectsBottom, 0xFF2A2A2A);
		graphics.fill(effectsLeft, effectsTop, effectsLeft + 1, effectsBottom, 0xFF6A6A6A);
		graphics.fill(effectsRight - 1, effectsTop, effectsRight, effectsBottom, 0xFF2A2A2A);

		UiScaleText.withScale(graphics, x, y, () ->
			UiScaleText.draw(graphics, font, getMessage(), contentLeft, y + ModPanelLayout.TITLE_TEXT_TOP, 0xFFFFFF)
		);
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
		return false;
	}
}
