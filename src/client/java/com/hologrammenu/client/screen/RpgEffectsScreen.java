package com.hologrammenu.client.screen;

import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.UiLayoutHelper;
import com.hologrammenu.client.screen.widget.UiScale;
import com.hologrammenu.client.screen.widget.UiScaleText;
import com.hologrammenu.client.screen.widget.IconPlaceholderButton;
import com.hologrammenu.rpg.RpgCustomEffectStore;
import com.hologrammenu.rpg.RpgEffectCatalog;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class RpgEffectsScreen extends Screen {
	private static final int CONTENT_WIDTH = UiScale.s(360);
	private static final int FIELD_HEIGHT = UiScale.s(18);
	private static final int INDEX_WIDTH = UiScale.s(92);
	private static final int LABEL_WIDTH = UiScale.s(64);
	private static final int CUSTOM_EFFECTS_PER_PAGE = 8;

	private final Screen parent;
	private SandboxTab activeTab = SandboxTab.BUILDER;
	private String selectedPresetId = RpgEffectCatalog.first().id();
	private String selectedCustomId = "";
	private String draftId = "";
	private String draftName = "Custom Effect";
	private String draftCategory = "Gear";
	private String draftDescription = "Custom RPG effect.";
	private String draftColorCode = "&d";
	private int draftLevel = 1;
	private int draftMaxLevel = 10;
	private int draftBasePower = 1;
	private int draftPowerPerLevel = 1;
	private int customPage;
	private EditBox nameField;
	private EditBox categoryField;
	private EditBox descriptionField;
	private EditBox colorField;

	public RpgEffectsScreen(Screen parent) {
		super(Component.translatable("screen.hologrammenu.rpg_effects.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int contentWidth = Math.min(CONTENT_WIDTH, this.width - ModPanelLayout.SCREEN_MARGIN);
		int left = ModPanelLayout.centeredX(this.width, contentWidth);
		int top = ModPanelLayout.centeredContentTop(this.height, UiScale.s(214));
		int buttonHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int third = ModPanelLayout.columnWidth(contentWidth, 3, rowGap);

		addTabButton(left, top, third, buttonHeight, SandboxTab.PRESETS, "screen.hologrammenu.rpg_effects.tab.presets");
		addTabButton(left + third + rowGap, top, third, buttonHeight, SandboxTab.BUILDER, "screen.hologrammenu.rpg_effects.tab.builder");
		addTabButton(left + (third + rowGap) * 2, top, third, buttonHeight, SandboxTab.CREATED, "screen.hologrammenu.rpg_effects.tab.created");

		int bodyTop = top + buttonHeight + ModPanelLayout.SECTION_GAP + UiScale.s(12);
		if (activeTab == SandboxTab.PRESETS) {
			buildPresetTab(left, bodyTop, contentWidth);
		} else if (activeTab == SandboxTab.CREATED) {
			buildCreatedTab(left, bodyTop, contentWidth);
		} else {
			buildBuilderTab(left, bodyTop, contentWidth);
		}
	}

	private void addTabButton(int x, int y, int width, int height, SandboxTab tab, String labelKey) {
		Button button = Button.builder(Component.translatable(labelKey), press -> {
			activeTab = tab;
			refresh();
		}).bounds(x, y, width, height).build();
		if (activeTab == tab) {
			ModUiSelectionState.markSelected(button);
		}
		addRenderableWidget(button);
	}

	private void buildPresetTab(int left, int top, int contentWidth) {
		int buttonHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int half = ModPanelLayout.columnWidth(contentWidth, 2, rowGap);
		List<RpgEffectCatalog.Entry> effects = RpgEffectCatalog.effects();
		for (int index = 0; index < effects.size(); index++) {
			RpgEffectCatalog.Entry effect = effects.get(index);
			int col = index % 2;
			int row = index / 2;
			Button button = IconPlaceholderButton.create(
				left + col * (half + rowGap),
				top + row * (buttonHeight + rowGap),
				half,
				buttonHeight,
				Component.literal(effect.category() + ": " + effect.name()),
				press -> {
					selectedPresetId = effect.id();
					loadPreset(effect);
					refresh();
				});
			if (effect.id().equals(selectedPresetId)) {
				ModUiSelectionState.markSelected(button);
			}
			addRenderableWidget(button);
		}

		int actionY = top + 4 * (buttonHeight + rowGap) + ModPanelLayout.SECTION_GAP;
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_effects.load_builder"), press -> {
			loadPreset(RpgEffectCatalog.byId(selectedPresetId));
			activeTab = SandboxTab.BUILDER;
			refresh();
		}).bounds(left, actionY, half, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), press -> onClose())
			.bounds(left + half + rowGap, actionY, half, buttonHeight).build());
	}

	private void buildCreatedTab(int left, int top, int contentWidth) {
		int buttonHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int half = ModPanelLayout.columnWidth(contentWidth, 2, rowGap);
		List<RpgCustomEffectStore.CustomEffect> effects = RpgCustomEffectStore.effects();
		int maxPage = Math.max(0, (effects.size() - 1) / CUSTOM_EFFECTS_PER_PAGE);
		customPage = Math.max(0, Math.min(maxPage, customPage));
		int start = customPage * CUSTOM_EFFECTS_PER_PAGE;
		int end = Math.min(effects.size(), start + CUSTOM_EFFECTS_PER_PAGE);
		for (int index = start; index < end; index++) {
			RpgCustomEffectStore.CustomEffect effect = effects.get(index);
			int local = index - start;
			int col = local % 2;
			int row = local / 2;
			Button button = IconPlaceholderButton.create(
				left + col * (half + rowGap),
				top + row * (buttonHeight + rowGap),
				half,
				buttonHeight,
				Component.literal(effect.category() + ": " + effect.name()),
				press -> {
					selectedCustomId = effect.id();
					loadCustom(effect);
					refresh();
				});
			if (effect.id().equals(selectedCustomId)) {
				ModUiSelectionState.markSelected(button);
			}
			addRenderableWidget(button);
		}

		int pageY = top + 4 * (buttonHeight + rowGap) + ModPanelLayout.SECTION_GAP;
		int third = ModPanelLayout.columnWidth(contentWidth, 3, rowGap);
		addRenderableWidget(Button.builder(Component.literal("<"), press -> {
			customPage = Math.max(0, customPage - 1);
			refresh();
		}).bounds(left, pageY, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_effects.page", customPage + 1, maxPage + 1), press -> {
			customPage = customPage >= maxPage ? 0 : customPage + 1;
			refresh();
		}).bounds(left + third + rowGap, pageY, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.literal(">"), press -> {
			customPage = Math.min(maxPage, customPage + 1);
			refresh();
		}).bounds(left + (third + rowGap) * 2, pageY, third, buttonHeight).build());

		int actionY = pageY + buttonHeight + rowGap;
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_effects.edit"), press -> {
			RpgCustomEffectStore.find(selectedCustomId).ifPresent(this::loadCustom);
			activeTab = SandboxTab.BUILDER;
			refresh();
		}).bounds(left, actionY, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_effects.delete"), press -> {
			RpgCustomEffectStore.remove(selectedCustomId);
			selectedCustomId = "";
			refresh();
		}).bounds(left + third + rowGap, actionY, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), press -> onClose())
			.bounds(left + (third + rowGap) * 2, actionY, third, buttonHeight).build());
	}

	private void buildBuilderTab(int left, int top, int contentWidth) {
		int rowGap = ModPanelLayout.ROW_GAP;
		int buttonHeight = UiLayoutHelper.buttonHeight(this.font);
		int formLeft = left + INDEX_WIDTH + rowGap;
		int formWidth = contentWidth - INDEX_WIDTH - rowGap;
		int fieldLeft = formLeft + LABEL_WIDTH + rowGap;
		int fieldWidth = formWidth - LABEL_WIDTH - rowGap;
		int y = top;
		nameField = addField(fieldLeft, y, fieldWidth, "screen.hologrammenu.rpg_effects.name", draftName, value -> draftName = value);
		y += FIELD_HEIGHT + rowGap + UiScale.s(10);
		categoryField = addField(fieldLeft, y, fieldWidth, "screen.hologrammenu.rpg_effects.category", draftCategory, value -> draftCategory = value);
		y += FIELD_HEIGHT + rowGap + UiScale.s(10);
		descriptionField = addField(fieldLeft, y, fieldWidth, "screen.hologrammenu.rpg_effects.description", draftDescription, value -> draftDescription = value);
		y += FIELD_HEIGHT + rowGap + UiScale.s(10);
		colorField = addField(fieldLeft, y, fieldWidth, "screen.hologrammenu.rpg_effects.color", draftColorCode, value -> draftColorCode = value);
		y += FIELD_HEIGHT + ModPanelLayout.SECTION_GAP + UiScale.s(12);

		int quarter = ModPanelLayout.columnWidth(formWidth, 4, rowGap);
		addAdjustButton(formLeft, y, quarter, buttonHeight, "screen.hologrammenu.rpg_effects.level", draftLevel, -1, ValueTarget.LEVEL);
		addAdjustButton(formLeft + quarter + rowGap, y, quarter, buttonHeight, "screen.hologrammenu.rpg_effects.max_level", draftMaxLevel, 1, ValueTarget.MAX_LEVEL);
		addAdjustButton(formLeft + (quarter + rowGap) * 2, y, quarter, buttonHeight, "screen.hologrammenu.rpg_effects.base", draftBasePower, 1, ValueTarget.BASE);
		addAdjustButton(formLeft + (quarter + rowGap) * 3, y, quarter, buttonHeight, "screen.hologrammenu.rpg_effects.scale", draftPowerPerLevel, 1, ValueTarget.SCALE);
		y += buttonHeight + rowGap;

		int third = ModPanelLayout.columnWidth(formWidth, 3, rowGap);
		addRenderableWidget(Button.builder(Component.literal("-"), press -> {
			draftLevel = RpgCustomEffectStore.clampLevel(draftLevel - 1);
			refresh();
		}).bounds(formLeft, y, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_effects.preview_level", draftLevel), press -> {
			draftLevel = Math.min(draftMaxLevel, RpgCustomEffectStore.clampLevel(draftLevel + 1));
			refresh();
		}).bounds(formLeft + third + rowGap, y, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.literal("+"), press -> {
			draftLevel = Math.min(draftMaxLevel, RpgCustomEffectStore.clampLevel(draftLevel + 1));
			refresh();
		}).bounds(formLeft + (third + rowGap) * 2, y, third, buttonHeight).build());
		y += buttonHeight + ModPanelLayout.SECTION_GAP;

		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_effects.save"), press -> saveDraft())
			.bounds(formLeft, y, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_effects.new"), press -> {
			clearDraft();
			refresh();
		}).bounds(formLeft + third + rowGap, y, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), press -> onClose())
			.bounds(formLeft + (third + rowGap) * 2, y, third, buttonHeight).build());
	}

	private EditBox addField(int x, int y, int width, String labelKey, String value, java.util.function.Consumer<String> responder) {
		EditBox field = new EditBox(this.font, x, y, width, FIELD_HEIGHT, Component.translatable(labelKey));
		field.setMaxLength(80);
		field.setValue(value);
		field.setResponder(responder);
		addRenderableWidget(field);
		return field;
	}

	private void addAdjustButton(int x, int y, int width, int height, String labelKey, int value, int step, ValueTarget target) {
		addRenderableWidget(Button.builder(Component.translatable(labelKey, value), press -> {
			adjust(target, step);
			refresh();
		}).bounds(x, y, width, height).build());
	}

	private void adjust(ValueTarget target, int delta) {
		if (target == ValueTarget.LEVEL) {
			draftLevel = Math.min(draftMaxLevel, RpgCustomEffectStore.clampLevel(draftLevel + delta));
		} else if (target == ValueTarget.MAX_LEVEL) {
			draftMaxLevel = RpgCustomEffectStore.clampLevel(draftMaxLevel + delta);
			draftLevel = Math.min(draftLevel, draftMaxLevel);
		} else if (target == ValueTarget.BASE) {
			draftBasePower = Math.max(0, draftBasePower + delta);
		} else {
			draftPowerPerLevel = Math.max(0, draftPowerPerLevel + delta);
		}
	}

	private void saveDraft() {
		RpgCustomEffectStore.CustomEffect saved = RpgCustomEffectStore.addOrUpdate(
			draftId,
			draftName,
			draftCategory,
			draftDescription,
			draftColorCode,
			draftMaxLevel,
			draftBasePower,
			draftPowerPerLevel
		);
		loadCustom(saved);
		activeTab = SandboxTab.CREATED;
		refresh();
	}

	private void loadPreset(RpgEffectCatalog.Entry effect) {
		draftId = "";
		draftName = effect.name();
		draftCategory = effect.category();
		draftDescription = effect.description();
		draftColorCode = effect.colorCode();
		draftMaxLevel = RpgEffectCatalog.MAX_LEVEL;
		draftLevel = RpgEffectCatalog.clampLevel(draftLevel);
		draftBasePower = 1;
		draftPowerPerLevel = 1;
	}

	private void loadCustom(RpgCustomEffectStore.CustomEffect effect) {
		draftId = effect.id();
		selectedCustomId = effect.id();
		draftName = effect.name();
		draftCategory = effect.category();
		draftDescription = effect.description();
		draftColorCode = effect.colorCode();
		draftMaxLevel = effect.maxLevel();
		draftLevel = Math.min(draftLevel, draftMaxLevel);
		draftBasePower = effect.basePower();
		draftPowerPerLevel = effect.powerPerLevel();
	}

	private void clearDraft() {
		draftId = "";
		draftName = "Custom Effect";
		draftCategory = "Gear";
		draftDescription = "Custom RPG effect.";
		draftColorCode = "&d";
		draftLevel = 1;
		draftMaxLevel = 10;
		draftBasePower = 1;
		draftPowerPerLevel = 1;
	}

	private RpgCustomEffectStore.CustomEffect draftEffect() {
		return new RpgCustomEffectStore.CustomEffect(
			"preview",
			draftName,
			draftCategory,
			draftDescription,
			draftColorCode,
			draftMaxLevel,
			draftBasePower,
			draftPowerPerLevel
		);
	}

	private void refresh() {
		clearWidgets();
		init();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int contentWidth = Math.min(CONTENT_WIDTH, this.width - ModPanelLayout.SCREEN_MARGIN);
		int left = ModPanelLayout.centeredX(this.width, contentWidth);
		int top = ModPanelLayout.centeredContentTop(this.height, UiScale.s(214));
		UiScaleText.drawCentered(graphics, this.font, this.title, this.width / 2, ModPanelLayout.titleY(top), 0xFFFFFF);
		UiScaleText.drawCentered(
			graphics,
			this.font,
			Component.translatable("screen.hologrammenu.rpg_effects.hint"),
			this.width / 2,
			ModPanelLayout.hintY(top),
			0xA0A0A0
		);

		int labelTop = top + UiLayoutHelper.buttonHeight(this.font) + ModPanelLayout.SECTION_GAP;
		UiScaleText.draw(graphics, this.font, tabLabel(), left, labelTop, 0xA0A0A0);
		if (activeTab == SandboxTab.BUILDER) {
			drawColorCodeIndex(graphics, left, labelTop + UiScale.s(12));
			drawBuilderLabels(graphics, left + INDEX_WIDTH + ModPanelLayout.ROW_GAP, labelTop + UiScale.s(12));
		}
		RpgCustomEffectStore.CustomEffect preview = draftEffect();
		UiScaleText.drawCentered(
			graphics,
			this.font,
			Component.literal(preview.preview(draftLevel)),
			this.width / 2,
			top + UiScale.s(206),
			0xFFFFFF
		);
	}

	private Component tabLabel() {
		return switch (activeTab) {
			case PRESETS -> Component.translatable("screen.hologrammenu.rpg_effects.presets_label");
			case CREATED -> Component.translatable("screen.hologrammenu.rpg_effects.created_label");
			case BUILDER -> Component.translatable("screen.hologrammenu.rpg_effects.builder_label");
		};
	}

	private void drawBuilderLabels(GuiGraphicsExtractor graphics, int left, int top) {
		int y = top - UiScale.s(9);
		UiScaleText.draw(graphics, this.font, Component.translatable("screen.hologrammenu.rpg_effects.name"), left, y, 0xA0A0A0);
		y += FIELD_HEIGHT + ModPanelLayout.ROW_GAP + UiScale.s(10);
		UiScaleText.draw(graphics, this.font, Component.translatable("screen.hologrammenu.rpg_effects.category"), left, y, 0xA0A0A0);
		y += FIELD_HEIGHT + ModPanelLayout.ROW_GAP + UiScale.s(10);
		UiScaleText.draw(graphics, this.font, Component.translatable("screen.hologrammenu.rpg_effects.description"), left, y, 0xA0A0A0);
		y += FIELD_HEIGHT + ModPanelLayout.ROW_GAP + UiScale.s(10);
		UiScaleText.draw(graphics, this.font, Component.translatable("screen.hologrammenu.rpg_effects.color"), left, y, 0xA0A0A0);
	}

	private void drawColorCodeIndex(GuiGraphicsExtractor graphics, int left, int top) {
		UiScaleText.draw(graphics, this.font, Component.translatable("screen.hologrammenu.color_index.title"), left, top - UiScale.s(9), 0xA0A0A0);
		int y = top + UiScale.s(3);
		for (String entry : COLOR_CODE_INDEX) {
			UiScaleText.draw(graphics, this.font, entry, left, y, 0xCFCFCF, false);
			y += UiScale.s(8);
		}
	}

	private static final String[] COLOR_CODE_INDEX = {
		"&0 Black", "&1 Dark Blue", "&2 Dark Green", "&3 Dark Aqua",
		"&4 Dark Red", "&5 Purple", "&6 Gold", "&7 Gray",
		"&8 Dark Gray", "&9 Blue", "&a Green", "&b Aqua",
		"&c Red", "&d Pink", "&e Yellow", "&f White"
	};

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractTransparentBackground(graphics);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private enum SandboxTab {
		PRESETS,
		BUILDER,
		CREATED
	}

	private enum ValueTarget {
		LEVEL,
		MAX_LEVEL,
		BASE,
		SCALE
	}
}
