package com.hologrammenu.client.screen;

import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.UiLayoutHelper;
import com.hologrammenu.client.screen.widget.UiScaleText;
import com.hologrammenu.client.storage.StorageMenuClientPermissions;
import com.hologrammenu.head.HeadPresetEntry;
import com.hologrammenu.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PlayerHeadToolScreen extends Screen {
	private final Screen parent;
	private EditBox playerField;
	private Button headPresetsButton;
	private HeadPresetPickerOverlay headPresetPickerOverlay;
	private String selectedHeadDatabaseId = "";
	private String selectedHeadDatabaseBase64 = "";
	private boolean updatingPresetField;

	public PlayerHeadToolScreen(Screen parent) {
		super(Component.translatable("screen.hologrammenu.player_head_tool.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		boolean restorePicker = headPresetPickerOverlay != null && headPresetPickerOverlay.isOpen();
		if (headPresetPickerOverlay != null && restorePicker) {
			headPresetPickerOverlay.close();
		}

		int contentWidth = ModPanelLayout.screenContentWidth(this.width);
		int fieldX = ModPanelLayout.centeredX(this.width, contentWidth);
		int rowHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int sectionGap = ModPanelLayout.SECTION_GAP;
		int contentHeight = rowHeight + rowGap + rowHeight + sectionGap + rowHeight;
		int contentTop = ModPanelLayout.centeredContentTop(this.height, contentHeight);
		int y = contentTop;

		playerField = new EditBox(this.font, fieldX, y, contentWidth, rowHeight, Component.translatable("screen.hologrammenu.player_head_tool.player_name"));
		playerField.setMaxLength(64);
		playerField.setHint(Component.translatable("screen.hologrammenu.player_head_tool.player_name_hint"));
		playerField.setResponder(value -> {
			if (!updatingPresetField) {
				selectedHeadDatabaseId = "";
				selectedHeadDatabaseBase64 = "";
			}
		});
		addRenderableWidget(playerField);
		setInitialFocus(playerField);
		y += rowHeight + rowGap;

		headPresetsButton = Button.builder(Component.translatable("screen.hologrammenu.head_presets.button"), press -> toggleHeadPresets())
			.bounds(fieldX, y, contentWidth, rowHeight).build();
		addRenderableWidget(headPresetsButton);
		y += rowHeight + sectionGap;

		int half = ModPanelLayout.columnWidth(contentWidth, 2, rowGap);
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.shop.confirm"), press -> giveHead())
			.bounds(fieldX, y, half, rowHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), press -> onDone())
			.bounds(fieldX + half + rowGap, y, half, rowHeight).build());

		ensureHeadPresetPickerOverlay();
		if (restorePicker) {
			headPresetPickerOverlay.open();
		}
	}

	private void toggleHeadPresets() {
		ensureHeadPresetPickerOverlay();
		headPresetPickerOverlay.toggle();
	}

	private void ensureHeadPresetPickerOverlay() {
		if (headPresetPickerOverlay != null) {
			return;
		}
		headPresetPickerOverlay = new HeadPresetPickerOverlay(
			this,
			this::applyHeadPreset,
			this::headPresetPanelPosition
		);
	}

	private void applyHeadPreset(HeadPresetEntry entry) {
		selectedHeadDatabaseId = entry.id();
		selectedHeadDatabaseBase64 = entry.base64();
		if (playerField != null) {
			updatingPresetField = true;
			try {
				playerField.setValue(entry.name());
			} finally {
				updatingPresetField = false;
			}
		}
	}

	private int[] headPresetPanelPosition() {
		int anchorY = headPresetsButton != null
			? headPresetsButton.getY()
			: playerField != null ? playerField.getY() : 0;
		int fieldX = ModPanelLayout.centeredX(this.width, ModPanelLayout.screenContentWidth(this.width));
		int contentWidth = ModPanelLayout.screenContentWidth(this.width);
		return TextStylePanelPositions.besideField(this, fieldX, contentWidth, anchorY);
	}

	private void giveHead() {
		if (!StorageMenuClientPermissions.canEdit()) {
			return;
		}
		String name = playerField.getValue().trim();
		if (name.isEmpty() && selectedHeadDatabaseId.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new ModPackets.GivePlayerHeadPayload(name, selectedHeadDatabaseId, selectedHeadDatabaseBase64));
		onDone();
	}

	private void onDone() {
		if (headPresetPickerOverlay != null) {
			headPresetPickerOverlay.close();
		}
		if (this.minecraft != null) {
			this.minecraft.setScreen(parent);
		}
	}

	@Override
	public void onClose() {
		onDone();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (headPresetPickerOverlay != null && headPresetPickerOverlay.isOpen() && headPresetPickerOverlay.mouseScrolled(scrollY)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int rowHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int sectionGap = ModPanelLayout.SECTION_GAP;
		int contentHeight = rowHeight + rowGap + rowHeight + sectionGap + rowHeight;
		int contentTop = ModPanelLayout.centeredContentTop(this.height, contentHeight);
		UiScaleText.drawCentered(graphics, this.font, this.title, this.width / 2, ModPanelLayout.titleY(contentTop), 0xFFFFFF);
		UiScaleText.drawCentered(
			graphics,
			this.font,
			Component.translatable("screen.hologrammenu.player_head_tool.hint"),
			this.width / 2,
			ModPanelLayout.hintY(contentTop),
			0xA0A0A0
		);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractTransparentBackground(graphics);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
