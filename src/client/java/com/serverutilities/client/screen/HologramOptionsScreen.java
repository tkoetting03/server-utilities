package com.serverutilities.client.screen;

import com.serverutilities.client.screen.widget.FloatScaleSlider;
import com.serverutilities.client.screen.widget.HologramHeightSlider;
import com.serverutilities.client.screen.widget.ModPanelLayout;
import com.serverutilities.client.screen.widget.UiLayoutHelper;
import com.serverutilities.client.screen.widget.UiScaleText;
import com.serverutilities.hologram.HologramLineStack;
import com.serverutilities.hologram.HologramScale;
import com.serverutilities.network.ModPackets;
import com.serverutilities.text.StyledText;
import com.serverutilities.text.TextFormats;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HologramOptionsScreen extends Screen {
	private static final float MENU_SCALE = 0.7F;
	private static final int LINES_PER_PAGE = 4;
	private static final int LINE_BOX_PADDING = scaled(6);
	private static final int LINE_TITLE_HEIGHT = scaled(12);
	private static final int LINE_TEXT_BUTTON_GAP = scaled(5);
	private static final int STYLE_BUTTON_WIDTH = scaled(20);
	private static final int REMOVE_BUTTON_WIDTH = scaled(15);
	private static final int LINE_ACTION_AREA_WIDTH = STYLE_BUTTON_WIDTH + REMOVE_BUTTON_WIDTH + LINE_TEXT_BUTTON_GAP * 2;

	private final int entityId;
	private final List<HologramLineStack.Line> lines = new ArrayList<>();
	private final List<EditBox> textFields = new ArrayList<>();
	private final List<Integer> textFieldLineIndices = new ArrayList<>();
	private TextStyleOverlay styleOverlay;
	private int activeLineIndex = 0;
	private int page;
	private float groupHeightOffset;
	private boolean seeThroughWalls = true;

	public HologramOptionsScreen(int entityId, List<HologramLineStack.Line> lines) {
		super(Component.translatable("screen.serverutilities.hologram_options.title"));
		this.entityId = entityId;
		this.lines.addAll(splitGroupHeight(HologramLineStack.normalize(lines)));
		seeThroughWalls = this.lines.stream().findFirst().map(HologramLineStack.Line::seeThroughWalls).orElse(true);
	}

	@Override
	protected void init() {
		boolean restoreOpen = styleOverlay != null && styleOverlay.isOpen();
		var savedDraft = restoreOpen ? styleOverlay.getDraft() : null;
		if (styleOverlay != null) {
			styleOverlay.dispose();
		}
		textFields.clear();
		textFieldLineIndices.clear();

		int contentWidth = scaled(ModPanelLayout.screenContentWidth(width));
		int fieldX = ModPanelLayout.centeredX(width, contentWidth);
		int rowHeight = scaled(UiLayoutHelper.buttonHeight(font));
		int rowGap = scaled(ModPanelLayout.ROW_GAP);
		int sectionGap = scaled(ModPanelLayout.SECTION_GAP);
		int lineCount = lines.size();
		clampPage();
		int pageStart = pageStart();
		int visibleLineCount = visibleLineCount();
		int contentHeight = contentHeight(visibleLineCount, rowHeight, rowGap, sectionGap);
		int contentTop = ModPanelLayout.centeredContentTop(height, contentHeight);
		int currentY = contentTop;

		addRenderableWidget(new HologramHeightSlider(
			fieldX,
			currentY,
			contentWidth,
			rowHeight,
			Component.translatable("screen.serverutilities.hologram_options.group_height"),
			groupHeightOffset,
			() -> groupHeightOffset,
			value -> groupHeightOffset = value,
			() -> {}
		));
		currentY += rowHeight + rowGap;

		addRenderableWidget(Button.builder(seeThroughButtonLabel(), press -> toggleSeeThroughWalls())
			.bounds(fieldX, currentY, contentWidth, rowHeight)
			.build());
		currentY += rowHeight + rowGap;

		currentY = addPageNavigation(fieldX, currentY, contentWidth, rowHeight, rowGap);

		int boxHeight = lineBoxHeight(rowHeight, rowGap);
		for (int index = 0; index < visibleLineCount; index++) {
			int lineIndex = pageStart + index;
			HologramLineStack.Line line = lines.get(lineIndex);
			addRenderableOnly(new LineBackingWidget(
				fieldX,
				currentY,
				contentWidth,
				boxHeight,
				Component.translatable("screen.serverutilities.hologram_options.line_label", lineIndex + 1)
			));
			int innerX = fieldX + LINE_BOX_PADDING;
			int innerWidth = contentWidth - LINE_BOX_PADDING * 2;
			int lineY = currentY + LINE_BOX_PADDING + LINE_TITLE_HEIGHT + rowGap;

			EditBox field = createLineField(innerX, lineY, innerWidth, rowHeight, lineIndex, line);
			addRenderableWidget(field);
			textFields.add(field);
			textFieldLineIndices.add(lineIndex);

			addRenderableWidget(Button.builder(Component.literal("S"), press -> openStyleEditor(lineIndex, field, innerX, innerWidth))
				.bounds(innerX + innerWidth - STYLE_BUTTON_WIDTH - REMOVE_BUTTON_WIDTH - LINE_TEXT_BUTTON_GAP, lineY, STYLE_BUTTON_WIDTH, rowHeight)
				.build());

			Button removeButton = Button.builder(Component.literal("X"), press -> removeLine(lineIndex))
				.bounds(innerX + innerWidth - REMOVE_BUTTON_WIDTH, lineY, REMOVE_BUTTON_WIDTH, rowHeight)
				.build();
			removeButton.active = lineCount > 1;
			addRenderableWidget(removeButton);

			if (lineIndex == activeLineIndex) {
				setInitialFocus(field);
			}

			lineY += rowHeight + rowGap;
			addRenderableWidget(new FloatScaleSlider(
				innerX,
				lineY,
				innerWidth,
				rowHeight,
				Component.translatable("screen.serverutilities.hologram_options.hologram_scale"),
				line.scale(),
				() -> lines.get(lineIndex).scale(),
				value -> {
					HologramLineStack.Line current = lines.get(lineIndex);
					lines.set(lineIndex, new HologramLineStack.Line(current.text(), value, current.heightOffset(), current.seeThroughWalls()));
				},
				() -> {}
			));
			lineY += rowHeight + rowGap;
			addRenderableWidget(new HologramHeightSlider(
				innerX,
				lineY,
				innerWidth,
				rowHeight,
				Component.translatable("screen.serverutilities.hologram_options.line_height"),
				line.heightOffset(),
				() -> lines.get(lineIndex).heightOffset(),
				value -> {
					HologramLineStack.Line current = lines.get(lineIndex);
					lines.set(lineIndex, new HologramLineStack.Line(current.text(), current.scale(), value, current.seeThroughWalls()));
				},
				() -> {}
			));
			currentY += boxHeight + rowGap;
		}

		restoreStyleOverlay(restoreOpen, savedDraft, fieldX, contentWidth);

		addRenderableWidget(Button.builder(Component.literal("+ Add Line"), press -> addLine())
			.bounds(fieldX, currentY, contentWidth, rowHeight)
			.build());

		currentY += rowHeight + sectionGap;
		addActionButtons(fieldX, currentY, contentWidth, rowHeight, rowGap);
	}

	private EditBox createLineField(int fieldX, int currentY, int contentWidth, int rowHeight, int index, HologramLineStack.Line line) {
		EditBox field = new EditBox(font, fieldX, currentY, contentWidth - LINE_ACTION_AREA_WIDTH, rowHeight, Component.translatable("screen.serverutilities.hologram_options.text"));
		field.setMaxLength(256);
		field.setValue(TextFormats.parse(line.text()).text());
		field.setResponder(plain -> {
			HologramLineStack.Line current = lines.get(index);
			lines.set(index, new HologramLineStack.Line(TextFormats.parse(current.text()).withText(plain).serialize(), current.scale(), current.heightOffset(), current.seeThroughWalls()));
		});
		return field;
	}

	private Component seeThroughButtonLabel() {
		return Component.translatable(seeThroughWalls
			? "screen.serverutilities.hologram_options.see_through_on"
			: "screen.serverutilities.hologram_options.see_through_off");
	}

	private void toggleSeeThroughWalls() {
		seeThroughWalls = !seeThroughWalls;
		for (int i = 0; i < lines.size(); i++) {
			HologramLineStack.Line line = lines.get(i);
			lines.set(i, new HologramLineStack.Line(line.text(), line.scale(), line.heightOffset(), seeThroughWalls));
		}
		rebuildLineWidgets();
	}

	private int addPageNavigation(int fieldX, int currentY, int contentWidth, int rowHeight, int rowGap) {
		int maxPage = maxPage();
		int arrowWidth = rowHeight;
		int statusWidth = Math.max(0, contentWidth - arrowWidth * 2 - rowGap * 2);
		Button previous = Button.builder(Component.literal("<"), press -> changePage(-1))
			.bounds(fieldX, currentY, arrowWidth, rowHeight)
			.build();
		previous.active = page > 0;
		addRenderableWidget(previous);
		addRenderableOnly(new PageStatusWidget(
			fieldX + arrowWidth + rowGap,
			currentY,
			statusWidth,
			rowHeight,
			Component.translatable("screen.serverutilities.hologram_options.page_status", page + 1, maxPage + 1)
		));
		Button next = Button.builder(Component.literal(">"), press -> changePage(1))
			.bounds(fieldX + arrowWidth + rowGap + statusWidth + rowGap, currentY, arrowWidth, rowHeight)
			.build();
		next.active = page < maxPage;
		addRenderableWidget(next);
		return currentY + rowHeight + rowGap;
	}

	private void openStyleEditor(int index, EditBox field, int fieldX, int contentWidth) {
		activeLineIndex = index;
		HologramLineStack.Line current = lines.get(index);
		lines.set(index, new HologramLineStack.Line(TextFormats.parse(current.text()).withText(field.getValue()).serialize(), current.scale(), current.heightOffset(), current.seeThroughWalls()));
		int[] position = TextStylePanelPositions.besideField(this, fieldX, contentWidth, field.getY());
		if (styleOverlay != null) {
			styleOverlay.dispose();
		}
		styleOverlay = new TextStyleOverlay(
			this,
			field::getValue,
			TextStyleTarget.editBox(field, serialized -> {
				HologramLineStack.Line styled = lines.get(index);
				lines.set(index, new HologramLineStack.Line(serialized, styled.scale(), styled.heightOffset(), styled.seeThroughWalls()));
			}),
			() -> TextStylePanelPositions.besideField(this, fieldX, contentWidth, field.getY())
		);
		styleOverlay.toggle(lines.get(index).text(), position[0], position[1]);
	}

	private void restoreStyleOverlay(boolean restoreOpen, StyledText savedDraft, int fieldX, int contentWidth) {
		int fieldIndex = textFieldLineIndices.indexOf(activeLineIndex);
		if (!restoreOpen || savedDraft == null || fieldIndex < 0) {
			return;
		}
		EditBox activeField = textFields.get(fieldIndex);
		styleOverlay = new TextStyleOverlay(
			this,
			activeField::getValue,
			TextStyleTarget.editBox(activeField, serialized -> {
				HologramLineStack.Line current = lines.get(activeLineIndex);
				lines.set(activeLineIndex, new HologramLineStack.Line(serialized, current.scale(), current.heightOffset(), current.seeThroughWalls()));
			}),
			() -> TextStylePanelPositions.besideField(this, fieldX, contentWidth, activeField.getY())
		);
		styleOverlay.openWithDraft(savedDraft);
	}

	private void addLine() {
		if (lines.size() >= HologramLineStack.MAX_LINES) {
			return;
		}
		lines.add(new HologramLineStack.Line("", HologramScale.DEFAULT, 0.0F, seeThroughWalls));
		activeLineIndex = lines.size() - 1;
		page = pageForLine(activeLineIndex);
		if (styleOverlay != null) {
			styleOverlay.dispose();
		}
		rebuildLineWidgets();
	}

	private void removeLine(int index) {
		if (lines.size() <= 1) {
			return;
		}
		lines.remove(index);
		activeLineIndex = Math.max(0, Math.min(activeLineIndex, lines.size() - 1));
		clampPage();
		if (styleOverlay != null) {
			styleOverlay.dispose();
		}
		rebuildLineWidgets();
	}

	private void changePage(int delta) {
		int next = Math.max(0, Math.min(maxPage(), page + delta));
		if (next == page) {
			return;
		}
		page = next;
		activeLineIndex = Math.min(activeLineIndex, lines.size() - 1);
		if (styleOverlay != null) {
			styleOverlay.dispose();
		}
		rebuildLineWidgets();
	}

	private void rebuildLineWidgets() {
		clearWidgets();
		init();
	}

	private static int scaled(int value) {
		if (value == 0) {
			return 0;
		}
		return Math.max(1, Math.round(value * MENU_SCALE));
	}

	private int contentHeight(int visibleLineCount, int rowHeight, int rowGap, int sectionGap) {
		int controls = ModPanelLayout.stackHeight(5, rowHeight, rowGap);
		int linesHeight = visibleLineCount == 0 ? 0 : visibleLineCount * lineBoxHeight(rowHeight, rowGap) + (visibleLineCount - 1) * rowGap;
		return controls + linesHeight + sectionGap + rowHeight;
	}

	private int lineBoxHeight(int rowHeight, int rowGap) {
		return LINE_BOX_PADDING * 2 + LINE_TITLE_HEIGHT + rowGap + rowHeight * 3 + rowGap * 2;
	}

	private int visibleLineCount() {
		return Math.max(0, Math.min(LINES_PER_PAGE, lines.size() - pageStart()));
	}

	private int pageStart() {
		return page * LINES_PER_PAGE;
	}

	private int maxPage() {
		return Math.max(0, (lines.size() - 1) / LINES_PER_PAGE);
	}

	private int pageForLine(int lineIndex) {
		return Math.max(0, lineIndex / LINES_PER_PAGE);
	}

	private void clampPage() {
		page = Math.max(0, Math.min(page, maxPage()));
	}

	private List<HologramLineStack.Line> splitGroupHeight(List<HologramLineStack.Line> source) {
		if (source.isEmpty()) {
			groupHeightOffset = 0.0F;
			return source;
		}
		float total = 0.0F;
		for (HologramLineStack.Line line : source) {
			total += line.heightOffset();
		}
		groupHeightOffset = HologramLineStack.clampHeightOffset(total / source.size());
		List<HologramLineStack.Line> relative = new ArrayList<>(source.size());
		for (HologramLineStack.Line line : source) {
			relative.add(new HologramLineStack.Line(
				line.text(),
				line.scale(),
				HologramLineStack.clampHeightOffset(line.heightOffset() - groupHeightOffset),
				line.seeThroughWalls()
			));
		}
		return relative;
	}

	private void addActionButtons(int fieldX, int currentY, int contentWidth, int rowHeight, int rowGap) {
		int third = ModPanelLayout.columnWidth(contentWidth, 3, rowGap);
		addRenderableWidget(Button.builder(Component.translatable("screen.serverutilities.hologram_options.save"), press -> save())
			.bounds(fieldX, currentY, third, rowHeight)
			.build());
		addRenderableWidget(Button.builder(Component.translatable("screen.serverutilities.hologram_options.delete"), press -> {
			ClientPlayNetworking.send(new ModPackets.HologramEditPayload(entityId, "delete", ""));
			onClose();
		}).bounds(fieldX + third + rowGap, currentY, third, rowHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), press -> onClose())
			.bounds(fieldX + (third + rowGap) * 2, currentY, third, rowHeight)
			.build());
	}

	private void save() {
		for (int i = 0; i < textFields.size(); i++) {
			int lineIndex = textFieldLineIndices.get(i);
			HologramLineStack.Line current = lines.get(lineIndex);
			lines.set(lineIndex, new HologramLineStack.Line(
				TextFormats.parse(current.text()).withText(textFields.get(i).getValue()).serialize(),
				current.scale(),
				current.heightOffset(),
				current.seeThroughWalls()
			));
		}
		List<HologramLineStack.Line> savedLines = lines.stream()
			.map(line -> new HologramLineStack.Line(line.text(), line.scale(), line.heightOffset() + groupHeightOffset, line.seeThroughWalls()))
			.toList();
		ClientPlayNetworking.send(new ModPackets.HologramEditPayload(entityId, "update", HologramLineStack.serialize(savedLines)));
		onClose();
	}

	@Override
	public void removed() {
		if (styleOverlay != null) {
			styleOverlay.close();
		}
		super.removed();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int rowHeight = scaled(UiLayoutHelper.buttonHeight(font));
		int rowGap = scaled(ModPanelLayout.ROW_GAP);
		int sectionGap = scaled(ModPanelLayout.SECTION_GAP);
		int contentHeight = contentHeight(visibleLineCount(), rowHeight, rowGap, sectionGap);
		int contentTop = ModPanelLayout.centeredContentTop(height, contentHeight);

		UiScaleText.drawCentered(graphics, font, title, width / 2, ModPanelLayout.titleY(contentTop), 0xFFFFFF);
		UiScaleText.drawCentered(
			graphics,
			font,
			Component.translatable("screen.serverutilities.hologram_options.hint"),
			width / 2,
			ModPanelLayout.hintY(contentTop),
			0xA0A0A0
		);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		extractTransparentBackground(graphics);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private final class LineBackingWidget extends AbstractWidget {
		LineBackingWidget(int x, int y, int width, int height, Component label) {
			super(x, y, width, height, label);
			active = false;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			int x = getX();
			int y = getY();
			int right = x + width;
			int bottom = y + height;
			graphics.fill(x, y, right, bottom, 0xE0181818);
			graphics.fill(x, y, right, y + 1, 0xFF707070);
			graphics.fill(x, bottom - 1, right, bottom, 0xFF202020);
			graphics.fill(x, y, x + 1, bottom, 0xFF707070);
			graphics.fill(right - 1, y, right, bottom, 0xFF202020);
			UiScaleText.draw(graphics, font, getMessage(), x + LINE_BOX_PADDING, y + 3, 0xFFFFFF);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
		}
	}

	private final class PageStatusWidget extends AbstractWidget {
		PageStatusWidget(int x, int y, int width, int height, Component label) {
			super(x, y, width, height, label);
			active = false;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			UiScaleText.drawCentered(graphics, font, getMessage(), getX() + width / 2, getY() + (height - font.lineHeight) / 2, 0xFFFFFF);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
		}
	}
}
