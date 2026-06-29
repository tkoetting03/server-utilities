package com.hologrammenu.client.screen.widget;

import com.hologrammenu.particle.ParticlePresetEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class ParticlePresetGridWidget extends AbstractWidget {
	private static final int SLOT_SIZE = HeadPresetPickerPanelWidget.slotSize();
	private static final int COLS = HeadPresetPickerPanelWidget.COLS;

	private final Supplier<List<ParticlePresetEntry>> visibleEntries;
	private final IntSupplier scrollRow;
	private final IntSupplier selectedIndex;
	private final IntConsumer onSelectIndex;

	public ParticlePresetGridWidget(
		int x,
		int y,
		int width,
		int height,
		Supplier<List<ParticlePresetEntry>> visibleEntries,
		IntSupplier scrollRow,
		IntSupplier selectedIndex,
		IntConsumer onSelectIndex
	) {
		super(x, y, width, height, Component.empty());
		this.visibleEntries = visibleEntries;
		this.scrollRow = scrollRow;
		this.selectedIndex = selectedIndex;
		this.onSelectIndex = onSelectIndex;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		List<ParticlePresetEntry> entries = visibleEntries.get();
		int startRow = scrollRow.getAsInt();
		int selected = selectedIndex.getAsInt();
		int visibleRows = height / SLOT_SIZE;
		var font = Minecraft.getInstance().font;

		for (int row = 0; row < visibleRows; row++) {
			for (int col = 0; col < COLS; col++) {
				int index = (startRow + row) * COLS + col;
				if (index >= entries.size()) {
					continue;
				}
				ParticlePresetEntry entry = entries.get(index);
				int slotX = getX() + col * SLOT_SIZE;
				int slotY = getY() + row * SLOT_SIZE;
				boolean isSelected = index == selected;
				boolean hovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
				int background = isSelected ? 0xFF3D7A3D : hovered ? 0xFF6A6A6A : 0xFF8B8B8B;
				graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, background);
				if (isSelected) {
					graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + 1, 0xFF9FFF9F);
					graphics.fill(slotX, slotY + SLOT_SIZE - 1, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0xFF2A5A2A);
					graphics.fill(slotX, slotY, slotX + 1, slotY + SLOT_SIZE, 0xFF9FFF9F);
					graphics.fill(slotX + SLOT_SIZE - 1, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0xFF2A5A2A);
				}
				int swatchSize = SLOT_SIZE - 8;
				int swatchX = slotX + 4;
				int swatchY = slotY + 4;
				graphics.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, entry.previewColor() | 0xFF000000);
				graphics.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + 1, 0xFF2A2A2A);
				graphics.fill(swatchX, swatchY + swatchSize - 1, swatchX + swatchSize, swatchY + swatchSize, 0xFF1A1A1A);
				String label = entry.name();
				if (font.width(label) > SLOT_SIZE - 4) {
					label = font.plainSubstrByWidth(label, SLOT_SIZE - 8) + "...";
				}
				UiScaleText.drawCentered(graphics, font, Component.literal(label), slotX + SLOT_SIZE / 2, slotY + SLOT_SIZE - font.lineHeight - 2, 0xFFFFFF);
			}
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return;
		}
		List<ParticlePresetEntry> entries = visibleEntries.get();
		int localX = (int) event.x() - getX();
		int localY = (int) event.y() - getY();
		if (localX < 0 || localY < 0 || localX >= width || localY >= height) {
			return;
		}
		int col = localX / SLOT_SIZE;
		int row = localY / SLOT_SIZE;
		if (col < 0 || col >= COLS) {
			return;
		}
		int index = (scrollRow.getAsInt() + row) * COLS + col;
		if (index >= 0 && index < entries.size()) {
			onSelectIndex.accept(index);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
