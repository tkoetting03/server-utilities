package com.hologrammenu.client.screen;

import com.hologrammenu.client.HotkeyActions;
import com.hologrammenu.client.config.ClientConfig;
import com.hologrammenu.client.config.ClientSettings;
import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.UiLayoutHelper;
import com.hologrammenu.client.screen.widget.UiScale;
import com.hologrammenu.client.screen.widget.UiScaleText;
import com.hologrammenu.client.screen.widget.VanillaIconButton;
import com.hologrammenu.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.BooleanSupplier;

public class HotkeyHubScreen extends Screen {
	private static final int GROUP_COUNT = 4;
	private static final int GROUP_LABEL_GAP = UiScale.s(2);
	private static final int HEADER_HEIGHT = UiScale.s(28);

	public HotkeyHubScreen() {
		super(Component.translatable("screen.hologrammenu.hotkey_hub.title"));
	}

	@Override
	protected void init() {
		int buttonHeight = UiLayoutHelper.buttonHeight(this.font);
		int buttonWidth = hubButtonWidth();
		int rowGap = ModPanelLayout.ROW_GAP;
		int sectionGap = ModPanelLayout.SECTION_GAP;
		int panelWidth = panelWidth(buttonWidth);
		int panelHeight = panelHeight(buttonHeight, rowGap, sectionGap);
		int panelX = ModPanelLayout.centeredX(this.width, panelWidth);
		int panelY = Math.max(UiScale.s(12), (this.height - panelHeight) / 2);
		int x = panelX + ModPanelLayout.PANEL_PADDING;
		int y = panelY + ModPanelLayout.PANEL_PADDING + HEADER_HEIGHT;

		y = addGroupLabel(y);
		addRenderableWidget(createToggleButton(
			x, y, buttonWidth, buttonHeight,
			"screen.hologrammenu.hotkey_hub.npc_placement",
			() -> ClientSettings.npcPlacementModeEnabled,
			() -> HotkeyActions.toggleNpcPlacement(this.minecraft),
			new ItemStack(Items.VILLAGER_SPAWN_EGG)
		));
		y += buttonHeight + rowGap;

		addRenderableWidget(createToggleButton(
			x, y, buttonWidth, buttonHeight,
			"screen.hologrammenu.hotkey_hub.npc_edit",
			() -> ClientSettings.npcEditModeEnabled,
			() -> HotkeyActions.toggleNpcEdit(this.minecraft),
			new ItemStack(Items.NAME_TAG)
		));
		y += buttonHeight + sectionGap;

		y = addGroupLabel(y);
		addRenderableWidget(createToggleButton(
			x, y, buttonWidth, buttonHeight,
			"screen.hologrammenu.hotkey_hub.hologram_placement",
			() -> ClientSettings.placementModeEnabled,
			() -> HotkeyActions.toggleHologramPlacement(this.minecraft),
			new ItemStack(Items.GLOW_INK_SAC)
		));
		y += buttonHeight + rowGap;

		addRenderableWidget(createToggleButton(
			x, y, buttonWidth, buttonHeight,
			"screen.hologrammenu.hotkey_hub.hologram_edit",
			() -> ClientSettings.hologramEditModeEnabled,
			() -> HotkeyActions.toggleHologramEdit(this.minecraft),
			new ItemStack(Items.ARMOR_STAND)
		));
		y += buttonHeight + sectionGap;

		y = addGroupLabel(y);
		addRenderableWidget(VanillaIconButton.create(
			x, y, buttonWidth, buttonHeight,
			Component.translatable("screen.hologrammenu.item_styler.title"),
			new ItemStack(Items.ANVIL),
			press -> {
				this.minecraft.setScreen(null);
				ClientPlayNetworking.send(new ModPackets.ItemStylerOpenPayload());
			}
		));
		y += buttonHeight + rowGap;

		addRenderableWidget(createToggleButton(
			x, y, buttonWidth, buttonHeight,
			"screen.hologrammenu.hotkey_hub.style_widget",
			() -> ClientSettings.styleWidgetEnabled,
			() -> ClientConfig.setStyleWidgetEnabled(!ClientSettings.styleWidgetEnabled),
			new ItemStack(Items.NAME_TAG)
		));
		y += buttonHeight + sectionGap;

		y = addGroupLabel(y);
		addRenderableWidget(createToggleButton(
			x, y, buttonWidth, buttonHeight,
			"screen.hologrammenu.hotkey_hub.storage_placement",
			() -> ClientSettings.storagePlacementModeEnabled,
			() -> HotkeyActions.toggleStoragePlacement(this.minecraft),
			new ItemStack(Items.CHEST)
		));
		y += buttonHeight + rowGap;

		addRenderableWidget(createToggleButton(
			x, y, buttonWidth, buttonHeight,
			"screen.hologrammenu.hotkey_hub.container_editor_widget",
			() -> ClientSettings.containerEditorWidgetEnabled,
			() -> ClientConfig.setContainerEditorWidgetEnabled(!ClientSettings.containerEditorWidgetEnabled),
			new ItemStack(Items.WRITABLE_BOOK)
		));
		y += buttonHeight + sectionGap;

		addRenderableWidget(VanillaIconButton.create(
			x, y, buttonWidth, buttonHeight,
			Component.translatable("gui.done"),
			new ItemStack(Items.LIME_DYE),
			press -> onClose()
		));
	}

	private int hubButtonWidth() {
		return Math.min(
			ModPanelLayout.screenContentWidth(this.width),
			maxIconButtonWidth(
				toggleLabel("screen.hologrammenu.hotkey_hub.hologram_placement", true),
				toggleLabel("screen.hologrammenu.hotkey_hub.hologram_placement", false),
				toggleLabel("screen.hologrammenu.hotkey_hub.storage_placement", true),
				toggleLabel("screen.hologrammenu.hotkey_hub.storage_placement", false),
				toggleLabel("screen.hologrammenu.hotkey_hub.npc_placement", true),
				toggleLabel("screen.hologrammenu.hotkey_hub.npc_placement", false),
				toggleLabel("screen.hologrammenu.hotkey_hub.npc_edit", true),
				toggleLabel("screen.hologrammenu.hotkey_hub.npc_edit", false),
				toggleLabel("screen.hologrammenu.hotkey_hub.hologram_edit", true),
				toggleLabel("screen.hologrammenu.hotkey_hub.hologram_edit", false),
				Component.translatable("screen.hologrammenu.item_styler.title"),
				toggleLabel("screen.hologrammenu.hotkey_hub.style_widget", true),
				toggleLabel("screen.hologrammenu.hotkey_hub.style_widget", false),
				toggleLabel("screen.hologrammenu.hotkey_hub.container_editor_widget", true),
				toggleLabel("screen.hologrammenu.hotkey_hub.container_editor_widget", false),
				Component.translatable("gui.done")
			)
		);
	}

	private Button createToggleButton(
		int x,
		int y,
		int width,
		int height,
		String labelKey,
		BooleanSupplier enabled,
		Runnable onToggle,
		ItemStack icon
	) {
		Button button = VanillaIconButton.create(x, y, width, height, toggleLabel(labelKey, enabled.getAsBoolean()), icon, press -> {
			onToggle.run();
			press.setMessage(toggleLabel(labelKey, enabled.getAsBoolean()));
			updateToggleOutline(press, enabled.getAsBoolean());
		});
		updateToggleOutline(button, enabled.getAsBoolean());
		return button;
	}

	private static void updateToggleOutline(Button button, boolean enabled) {
		if (enabled) {
			ModUiSelectionState.markSelected(button);
		} else {
			ModUiSelectionState.unmarkSelected(button);
		}
	}

	private static Component toggleLabel(String labelKey, boolean enabled) {
		return Component.translatable(
			enabled ? "screen.hologrammenu.hotkey_hub.toggle_on" : "screen.hologrammenu.hotkey_hub.toggle_off",
			Component.translatable(labelKey)
		);
	}

	private int maxIconButtonWidth(Component... labels) {
		int max = 0;
		for (Component label : labels) {
			max = Math.max(max, UiLayoutHelper.iconButtonWidth(this.font, label));
		}
		return max;
	}

	private int panelWidth(int buttonWidth) {
		return buttonWidth + ModPanelLayout.PANEL_PADDING * 2;
	}

	private int panelHeight(int buttonHeight, int rowGap, int sectionGap) {
		int buttonRows = 9;
		int groupButtonGaps = rowGap * 4;
		return ModPanelLayout.PANEL_PADDING * 2
			+ HEADER_HEIGHT
			+ GROUP_COUNT * groupLabelHeight()
			+ GROUP_COUNT * GROUP_LABEL_GAP
			+ buttonRows * buttonHeight
			+ groupButtonGaps
			+ GROUP_COUNT * sectionGap;
	}

	private int groupLabelHeight() {
		return UiScaleText.lineHeight(this.font) + UiScale.s(2);
	}

	private int addGroupLabel(int y) {
		return y + groupLabelHeight() + GROUP_LABEL_GAP;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int buttonHeight = UiLayoutHelper.buttonHeight(this.font);
		int buttonWidth = hubButtonWidth();
		int rowGap = ModPanelLayout.ROW_GAP;
		int sectionGap = ModPanelLayout.SECTION_GAP;
		int panelWidth = panelWidth(buttonWidth);
		int panelHeight = panelHeight(buttonHeight, rowGap, sectionGap);
		int panelX = ModPanelLayout.centeredX(this.width, panelWidth);
		int panelY = Math.max(UiScale.s(12), (this.height - panelHeight) / 2);
		drawPanel(graphics, panelX, panelY, panelWidth, panelHeight);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int contentLeft = panelX + ModPanelLayout.PANEL_PADDING;
		int contentWidth = buttonWidth;
		int y = panelY + ModPanelLayout.PANEL_PADDING;
		UiScaleText.drawCentered(graphics, this.font, this.title, panelX + panelWidth / 2, y + UiScale.s(2), 0xFFFFFF);
		UiScaleText.drawCentered(
			graphics,
			this.font,
			Component.translatable("screen.hologrammenu.hotkey_hub.hint"),
			panelX + panelWidth / 2,
			y + UiScale.s(14),
			0xA0A0A0
		);
		y += HEADER_HEIGHT;
		y = drawGroupLabel(graphics, contentLeft, contentWidth, y, Component.translatable("screen.hologrammenu.hotkey_hub.group.npc"));
		y += buttonHeight + rowGap + buttonHeight + sectionGap;
		y = drawGroupLabel(graphics, contentLeft, contentWidth, y, Component.translatable("screen.hologrammenu.hotkey_hub.group.hologram"));
		y += buttonHeight + rowGap + buttonHeight + sectionGap;
		y = drawGroupLabel(graphics, contentLeft, contentWidth, y, Component.translatable("screen.hologrammenu.hotkey_hub.group.style"));
		y += buttonHeight + rowGap + buttonHeight + sectionGap;
		drawGroupLabel(graphics, contentLeft, contentWidth, y, Component.translatable("screen.hologrammenu.hotkey_hub.group.container"));
	}

	private void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		int right = x + width;
		int bottom = y + height;
		graphics.fill(x, y, right, bottom, 0xF0181818);
		graphics.fill(x, y, right, y + 1, 0xFF6A6A6A);
		graphics.fill(x, bottom - 1, right, bottom, 0xFF2A2A2A);
		graphics.fill(x, y, x + 1, bottom, 0xFF6A6A6A);
		graphics.fill(right - 1, y, right, bottom, 0xFF2A2A2A);
	}

	private int drawGroupLabel(GuiGraphicsExtractor graphics, int x, int width, int y, Component label) {
		int lineY = y + groupLabelHeight() - UiScale.s(1);
		int labelWidth = UiScaleText.width(this.font, label);
		int labelCenter = x + width / 2;
		int labelLeft = labelCenter - labelWidth / 2;
		int labelRight = labelLeft + labelWidth;
		int lineGap = UiScale.s(5);
		graphics.fill(x, lineY, Math.max(x, labelLeft - lineGap), lineY + 1, 0xFF4A4A4A);
		graphics.fill(Math.min(x + width, labelRight + lineGap), lineY, x + width, lineY + 1, 0xFF4A4A4A);
		UiScaleText.drawCentered(graphics, this.font, label, labelCenter, y, 0xFFFFFF);
		return y + groupLabelHeight() + GROUP_LABEL_GAP;
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
