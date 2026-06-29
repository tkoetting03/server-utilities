package com.hologrammenu.client.screen;

import com.hologrammenu.client.HotkeyActions;
import com.hologrammenu.client.config.ClientSettings;
import com.hologrammenu.client.screen.ModUiSelectionState;
import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.UiLayoutHelper;
import com.hologrammenu.client.screen.widget.UiScaleText;
import com.hologrammenu.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class HotkeyHubScreen extends Screen {
	private static final int CONTENT_ROW_COUNT = 9;

	public HotkeyHubScreen() {
		super(Component.translatable("screen.hologrammenu.hotkey_hub.title"));
	}

	@Override
	protected void init() {
		int buttonHeight = UiLayoutHelper.buttonHeight(this.font);
		int buttonWidth = hubButtonWidth();
		int rowGap = ModPanelLayout.ROW_GAP;
		int sectionGap = ModPanelLayout.SECTION_GAP;
		int contentHeight = ModPanelLayout.stackHeight(CONTENT_ROW_COUNT, buttonHeight, rowGap)
			+ sectionGap
			+ buttonHeight;
		int x = ModPanelLayout.centeredX(this.width, buttonWidth);
		int y = ModPanelLayout.centeredContentTop(this.height, contentHeight);

		addRenderableWidget(createToggleButton(
			x, y, buttonWidth, buttonHeight,
			"screen.hologrammenu.hotkey_hub.hologram_placement",
			() -> ClientSettings.placementModeEnabled,
			() -> HotkeyActions.toggleHologramPlacement(this.minecraft)
		));
		y += buttonHeight + rowGap;

		addRenderableWidget(createToggleButton(
			x, y, buttonWidth, buttonHeight,
			"screen.hologrammenu.hotkey_hub.storage_placement",
			() -> ClientSettings.storagePlacementModeEnabled,
			() -> HotkeyActions.toggleStoragePlacement(this.minecraft)
		));
		y += buttonHeight + rowGap;

		addRenderableWidget(createToggleButton(
			x, y, buttonWidth, buttonHeight,
			"screen.hologrammenu.hotkey_hub.npc_placement",
			() -> ClientSettings.npcPlacementModeEnabled,
			() -> HotkeyActions.toggleNpcPlacement(this.minecraft)
		));
		y += buttonHeight + rowGap;

		addRenderableWidget(createToggleButton(
			x, y, buttonWidth, buttonHeight,
			"screen.hologrammenu.hotkey_hub.npc_edit",
			() -> ClientSettings.npcEditModeEnabled,
			() -> HotkeyActions.toggleNpcEdit(this.minecraft)
		));
		y += buttonHeight + rowGap;

		addRenderableWidget(createToggleButton(
			x, y, buttonWidth, buttonHeight,
			"screen.hologrammenu.hotkey_hub.hologram_edit",
			() -> ClientSettings.hologramEditModeEnabled,
			() -> HotkeyActions.toggleHologramEdit(this.minecraft)
		));
		y += buttonHeight + rowGap;

		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.hotkey_hub.player_head_tool"), press ->
			this.minecraft.setScreen(new PlayerHeadToolScreen(this))
		).bounds(x, y, buttonWidth, buttonHeight).build());
		y += buttonHeight + rowGap;

		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.item_styler.title"), press -> {
			this.minecraft.setScreen(null);
			ClientPlayNetworking.send(new ModPackets.ItemStylerOpenPayload());
		}).bounds(x, y, buttonWidth, buttonHeight).build());
		y += buttonHeight + rowGap;

		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.hotkey_hub.rpg_effects"), press ->
			this.minecraft.setScreen(new RpgEffectsScreen(this))
		).bounds(x, y, buttonWidth, buttonHeight).build());
		y += buttonHeight + rowGap;

		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.hotkey_hub.rpg_skills"), press ->
			this.minecraft.setScreen(new RpgSkillsScreen(this))
		).bounds(x, y, buttonWidth, buttonHeight).build());
		y += buttonHeight + sectionGap;

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), press -> onClose())
			.bounds(x, y, buttonWidth, buttonHeight).build());
	}

	private int hubButtonWidth() {
		return Math.min(
			ModPanelLayout.screenContentWidth(this.width),
			UiLayoutHelper.maxButtonWidth(
				this.font,
				Component.translatable("screen.hologrammenu.hotkey_hub.player_head_tool"),
				toggleLabel("screen.hologrammenu.hotkey_hub.hologram_placement", true),
				toggleLabel("screen.hologrammenu.hotkey_hub.storage_placement", true),
				toggleLabel("screen.hologrammenu.hotkey_hub.npc_placement", true),
				toggleLabel("screen.hologrammenu.hotkey_hub.npc_edit", true),
				toggleLabel("screen.hologrammenu.hotkey_hub.hologram_edit", true),
				Component.translatable("screen.hologrammenu.item_styler.title"),
				Component.translatable("screen.hologrammenu.hotkey_hub.rpg_effects"),
				Component.translatable("screen.hologrammenu.hotkey_hub.rpg_skills"),
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
		Runnable onToggle
	) {
		Button button = Button.builder(toggleLabel(labelKey, enabled.getAsBoolean()), press -> {
			onToggle.run();
			press.setMessage(toggleLabel(labelKey, enabled.getAsBoolean()));
			updateToggleOutline(press, enabled.getAsBoolean());
		}).bounds(x, y, width, height).build();
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

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int buttonHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int sectionGap = ModPanelLayout.SECTION_GAP;
		int contentHeight = ModPanelLayout.stackHeight(CONTENT_ROW_COUNT, buttonHeight, rowGap) + sectionGap + buttonHeight;
		int contentTop = ModPanelLayout.centeredContentTop(this.height, contentHeight);
		UiScaleText.drawCentered(graphics, this.font, this.title, this.width / 2, ModPanelLayout.titleY(contentTop), 0xFFFFFF);
		UiScaleText.drawCentered(
			graphics,
			this.font,
			Component.translatable("screen.hologrammenu.hotkey_hub.hint"),
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
