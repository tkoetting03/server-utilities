package com.hologrammenu.client.screen;

import com.hologrammenu.client.screen.widget.FloatScaleSlider;
import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.UiLayoutHelper;
import com.hologrammenu.client.screen.widget.UiScaleText;
import com.hologrammenu.hologram.HologramLineStack;
import com.hologrammenu.hologram.HologramScale;
import com.hologrammenu.network.ModPackets;
import com.hologrammenu.text.StyledText;
import com.hologrammenu.text.TextFormats;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HologramOptionsScreen extends Screen {
	private final int entityId;
	private final List<HologramLineStack.Line> lines = new ArrayList<>();
	private final List<EditBox> textFields = new ArrayList<>();
	private TextStyleOverlay styleOverlay;
	private int activeLineIndex = 0;

	public HologramOptionsScreen(int entityId, List<HologramLineStack.Line> lines) {
		super(Component.translatable("screen.hologrammenu.hologram_options.title"));
		this.entityId = entityId;
		this.lines.addAll(HologramLineStack.normalize(lines));
	}

	@Override
	protected void init() {
		boolean restoreOpen = styleOverlay != null && styleOverlay.isOpen();
		var savedDraft = restoreOpen ? styleOverlay.getDraft() : null;
		if (styleOverlay != null) {
			styleOverlay.dispose();
		}
		textFields.clear();

		int contentWidth = ModPanelLayout.screenContentWidth(width);
		int fieldX = ModPanelLayout.centeredX(width, contentWidth);
		int rowHeight = UiLayoutHelper.buttonHeight(font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int sectionGap = ModPanelLayout.SECTION_GAP;
		int lineCount = lines.size();
		int contentHeight = ModPanelLayout.stackHeight(lineCount * 2 + 2, rowHeight, rowGap) + sectionGap + rowHeight;
		int contentTop = ModPanelLayout.centeredContentTop(height, contentHeight);
		int currentY = contentTop;

		for (int index = 0; index < lineCount; index++) {
			int lineIndex = index;
			HologramLineStack.Line line = lines.get(lineIndex);
			EditBox field = createLineField(fieldX, currentY, contentWidth, rowHeight, lineIndex, line);
			addRenderableWidget(field);
			textFields.add(field);

			addRenderableWidget(Button.builder(Component.literal("S"), press -> openStyleEditor(lineIndex, field, fieldX, contentWidth))
				.bounds(fieldX + contentWidth - 40, currentY, 20, rowHeight)
				.build());

			Button removeButton = Button.builder(Component.literal("X"), press -> removeLine(lineIndex))
				.bounds(fieldX + contentWidth - 15, currentY, 15, rowHeight)
				.build();
			removeButton.active = lineCount > 1;
			addRenderableWidget(removeButton);

			if (index == activeLineIndex) {
				setInitialFocus(field);
			}

			currentY += rowHeight + rowGap;
			addRenderableWidget(new FloatScaleSlider(
				fieldX,
				currentY,
				contentWidth,
				Component.translatable("screen.hologrammenu.hologram_options.hologram_scale"),
				line.scale(),
				() -> lines.get(lineIndex).scale(),
				value -> {
					HologramLineStack.Line current = lines.get(lineIndex);
					lines.set(lineIndex, new HologramLineStack.Line(current.text(), value));
				},
				() -> {}
			));
			currentY += rowHeight + rowGap;
		}

		restoreStyleOverlay(restoreOpen, savedDraft, fieldX, contentWidth);

		addRenderableWidget(Button.builder(Component.literal("+ Add Line"), press -> addLine())
			.bounds(fieldX, currentY, contentWidth, rowHeight)
			.build());

		currentY += rowHeight + sectionGap;
		addActionButtons(fieldX, currentY, contentWidth, rowHeight, rowGap);
	}

	private EditBox createLineField(int fieldX, int currentY, int contentWidth, int rowHeight, int index, HologramLineStack.Line line) {
		EditBox field = new EditBox(font, fieldX, currentY, contentWidth - 45, rowHeight, Component.translatable("screen.hologrammenu.hologram_options.text"));
		field.setMaxLength(256);
		field.setValue(TextFormats.parse(line.text()).text());
		field.setResponder(plain -> {
			HologramLineStack.Line current = lines.get(index);
			lines.set(index, new HologramLineStack.Line(TextFormats.parse(current.text()).withText(plain).serialize(), current.scale()));
		});
		return field;
	}

	private void openStyleEditor(int index, EditBox field, int fieldX, int contentWidth) {
		activeLineIndex = index;
		HologramLineStack.Line current = lines.get(index);
		lines.set(index, new HologramLineStack.Line(TextFormats.parse(current.text()).withText(field.getValue()).serialize(), current.scale()));
		int[] position = TextStylePanelPositions.besideField(this, fieldX, contentWidth, field.getY());
		if (styleOverlay != null) {
			styleOverlay.dispose();
		}
		styleOverlay = new TextStyleOverlay(
			this,
			field::getValue,
			TextStyleTarget.editBox(field, serialized -> {
				HologramLineStack.Line styled = lines.get(index);
				lines.set(index, new HologramLineStack.Line(serialized, styled.scale()));
			}),
			() -> TextStylePanelPositions.besideField(this, fieldX, contentWidth, field.getY())
		);
		styleOverlay.toggle(lines.get(index).text(), position[0], position[1]);
	}

	private void restoreStyleOverlay(boolean restoreOpen, StyledText savedDraft, int fieldX, int contentWidth) {
		if (!restoreOpen || savedDraft == null || activeLineIndex >= textFields.size()) {
			return;
		}
		EditBox activeField = textFields.get(activeLineIndex);
		styleOverlay = new TextStyleOverlay(
			this,
			activeField::getValue,
			TextStyleTarget.editBox(activeField, serialized -> {
				HologramLineStack.Line current = lines.get(activeLineIndex);
				lines.set(activeLineIndex, new HologramLineStack.Line(serialized, current.scale()));
			}),
			() -> TextStylePanelPositions.besideField(this, fieldX, contentWidth, activeField.getY())
		);
		styleOverlay.openWithDraft(savedDraft);
	}

	private void addLine() {
		if (lines.size() >= HologramLineStack.MAX_LINES) {
			return;
		}
		lines.add(new HologramLineStack.Line("", HologramScale.DEFAULT));
		activeLineIndex = lines.size() - 1;
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
		if (styleOverlay != null) {
			styleOverlay.dispose();
		}
		rebuildLineWidgets();
	}

	private void rebuildLineWidgets() {
		clearWidgets();
		init();
	}

	private void addActionButtons(int fieldX, int currentY, int contentWidth, int rowHeight, int rowGap) {
		int third = ModPanelLayout.columnWidth(contentWidth, 3, rowGap);
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.hologram_options.save"), press -> save())
			.bounds(fieldX, currentY, third, rowHeight)
			.build());
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.hologram_options.delete"), press -> {
			ClientPlayNetworking.send(new ModPackets.HologramEditPayload(entityId, "delete", ""));
			onClose();
		}).bounds(fieldX + third + rowGap, currentY, third, rowHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), press -> onClose())
			.bounds(fieldX + (third + rowGap) * 2, currentY, third, rowHeight)
			.build());
	}

	private void save() {
		for (int i = 0; i < textFields.size(); i++) {
			HologramLineStack.Line current = lines.get(i);
			lines.set(i, new HologramLineStack.Line(TextFormats.parse(current.text()).withText(textFields.get(i).getValue()).serialize(), current.scale()));
		}
		ClientPlayNetworking.send(new ModPackets.HologramEditPayload(entityId, "update", HologramLineStack.serialize(lines)));
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
		int rowHeight = UiLayoutHelper.buttonHeight(font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int sectionGap = ModPanelLayout.SECTION_GAP;
		int contentHeight = ModPanelLayout.stackHeight(lines.size() * 2 + 2, rowHeight, rowGap) + sectionGap + rowHeight;
		int contentTop = ModPanelLayout.centeredContentTop(height, contentHeight);

		UiScaleText.drawCentered(graphics, font, title, width / 2, ModPanelLayout.titleY(contentTop), 0xFFFFFF);
		UiScaleText.drawCentered(
			graphics,
			font,
			Component.translatable("screen.hologrammenu.hologram_options.hint"),
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
}
