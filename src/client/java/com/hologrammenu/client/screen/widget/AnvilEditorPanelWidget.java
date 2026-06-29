package com.hologrammenu.client.screen.widget;

import com.hologrammenu.client.screen.AnvilEditorTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public class AnvilEditorPanelWidget extends AbstractWidget {
	private final AnvilEditorTab activeTab;
	private final int partCount;
	private final int lineCount;
	private final boolean gradientExpanded;

	public AnvilEditorPanelWidget(int x, int y, AnvilEditorTab activeTab, int partCount, int lineCount) {
		this(x, y, activeTab, partCount, lineCount, false);
	}

	public AnvilEditorPanelWidget(int x, int y, AnvilEditorTab activeTab, int partCount, int lineCount, boolean gradientExpanded) {
		this(x, y, activeTab, partCount, lineCount, gradientExpanded, false);
	}

	public AnvilEditorPanelWidget(
		int x,
		int y,
		AnvilEditorTab activeTab,
		int partCount,
		int lineCount,
		boolean gradientExpanded,
		boolean loreColorTableOpen
	) {
		super(
			x,
			y,
			AnvilEditorMetrics.PANEL_WIDTH,
			panelHeight(activeTab, partCount, lineCount, gradientExpanded, loreColorTableOpen),
			panelTitle(activeTab)
		);
		this.activeTab = activeTab;
		this.partCount = Math.max(1, partCount);
		this.lineCount = Math.max(1, lineCount);
		this.gradientExpanded = gradientExpanded;
		this.active = false;
	}

	public static int panelHeight(AnvilEditorTab activeTab, int partCount, int lineCount) {
		return panelHeight(activeTab, partCount, lineCount, false);
	}

	public static int panelHeight(AnvilEditorTab activeTab, int partCount, int lineCount, boolean gradientExpanded) {
		return panelHeight(activeTab, partCount, lineCount, gradientExpanded, false);
	}

	public static int panelHeight(
		AnvilEditorTab activeTab,
		int partCount,
		int lineCount,
		boolean gradientExpanded,
		boolean loreColorTableOpen
	) {
		return switch (activeTab) {
			case STYLE -> AnvilEditorMetrics.stylePanelHeight(partCount, gradientExpanded);
			case LORE -> AnvilEditorMetrics.lorePanelHeight(lineCount, loreColorTableOpen, gradientExpanded);
		};
	}

	private static Component panelTitle(AnvilEditorTab activeTab) {
		return switch (activeTab) {
			case STYLE -> Component.translatable("screen.hologrammenu.text_style.title");
			case LORE -> Component.translatable("screen.hologrammenu.anvil.lore_title");
		};
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

		var font = Minecraft.getInstance().font;
		int contentLeft = x + ModPanelLayout.PANEL_PADDING;
		if (activeTab == AnvilEditorTab.STYLE) {
			TextStylePanelLayout.Metrics layout = TextStylePanelLayout.metrics(
				partCount,
				AnvilEditorMetrics.tabRowHeight(),
				gradientExpanded
			);
			UiSectionSeparator.draw(
				graphics,
				font,
				Component.translatable("screen.hologrammenu.text_style.colors"),
				contentLeft,
				y + layout.colorsLabelTop(partCount),
				ModPanelLayout.CONTENT_WIDTH
			);
			UiSectionSeparator.draw(
				graphics,
				font,
				Component.translatable("screen.hologrammenu.text_style.text_effects"),
				contentLeft,
				y + layout.effectsLabelTop(partCount),
				ModPanelLayout.CONTENT_WIDTH
			);

			int effectsTop = y + layout.effectsTop(partCount);
			int effectsBottom = effectsTop + layout.effectsBlockHeight();
			int effectsRight = contentLeft + ModPanelLayout.CONTENT_WIDTH;
			graphics.fill(contentLeft, effectsTop, effectsRight, effectsTop + 1, 0xFF6A6A6A);
			graphics.fill(contentLeft, effectsBottom - 1, effectsRight, effectsBottom, 0xFF2A2A2A);
			graphics.fill(contentLeft, effectsTop, contentLeft + 1, effectsBottom, 0xFF6A6A6A);
			graphics.fill(effectsRight - 1, effectsTop, effectsRight, effectsBottom, 0xFF2A2A2A);
		}

		UiScaleText.withScale(graphics, x, y, () -> {
			UiScaleText.draw(graphics, font, getMessage(), contentLeft, y + ModPanelLayout.TITLE_TEXT_TOP, 0xFFFFFF);
			if (activeTab == AnvilEditorTab.LORE) {
				UiScaleText.draw(
					graphics,
					font,
					Component.translatable("screen.hologrammenu.anvil.lore_lines_label"),
					contentLeft,
					y + AnvilEditorMetrics.tabContentTop(),
					0xA0A0A0
				);
			}
		});
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
	}
}
