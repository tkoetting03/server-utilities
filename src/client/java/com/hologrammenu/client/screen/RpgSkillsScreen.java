package com.hologrammenu.client.screen;

import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.UiLayoutHelper;
import com.hologrammenu.client.screen.widget.UiScale;
import com.hologrammenu.client.screen.widget.UiScaleText;
import com.hologrammenu.client.screen.widget.IconPlaceholderButton;
import com.hologrammenu.rpg.RpgSkillStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class RpgSkillsScreen extends Screen {
	private static final int CONTENT_WIDTH = UiScale.s(360);
	private static final int FIELD_HEIGHT = UiScale.s(18);
	private static final int INDEX_WIDTH = UiScale.s(92);
	private static final int LABEL_WIDTH = UiScale.s(64);
	private static final int SKILLS_PER_PAGE = 8;

	private final Screen parent;
	private SkillTab activeTab = SkillTab.SKILLS;
	private String selectedSkillId = RpgSkillStore.first().id();
	private int page;
	private String draftId = RpgSkillStore.first().id();
	private String draftName = RpgSkillStore.first().name();
	private String draftDescription = RpgSkillStore.first().description();
	private String draftEffect = RpgSkillStore.first().effect();
	private String draftBonus = RpgSkillStore.first().bonus();
	private String draftColorCode = RpgSkillStore.first().colorCode();
	private int draftLevel = 1;
	private int draftMaxLevel = RpgSkillStore.first().maxLevel();
	private int draftBaseBonus = RpgSkillStore.first().baseBonus();
	private int draftBonusPerLevel = RpgSkillStore.first().bonusPerLevel();

	public RpgSkillsScreen(Screen parent) {
		super(Component.translatable("screen.hologrammenu.rpg_skills.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int contentWidth = Math.min(CONTENT_WIDTH, this.width - ModPanelLayout.SCREEN_MARGIN);
		int left = ModPanelLayout.centeredX(this.width, contentWidth);
		int top = ModPanelLayout.centeredContentTop(this.height, UiScale.s(224));
		int buttonHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int half = ModPanelLayout.columnWidth(contentWidth, 2, rowGap);

		addTabButton(left, top, half, buttonHeight, SkillTab.SKILLS, "screen.hologrammenu.rpg_skills.tab.skills");
		addTabButton(left + half + rowGap, top, half, buttonHeight, SkillTab.BUILDER, "screen.hologrammenu.rpg_skills.tab.builder");

		int bodyTop = top + buttonHeight + ModPanelLayout.SECTION_GAP + UiScale.s(12);
		if (activeTab == SkillTab.SKILLS) {
			buildSkillsTab(left, bodyTop, contentWidth);
		} else {
			buildBuilderTab(left, bodyTop, contentWidth);
		}
	}

	private void addTabButton(int x, int y, int width, int height, SkillTab tab, String labelKey) {
		Button button = Button.builder(Component.translatable(labelKey), press -> {
			activeTab = tab;
			refresh();
		}).bounds(x, y, width, height).build();
		if (activeTab == tab) {
			ModUiSelectionState.markSelected(button);
		}
		addRenderableWidget(button);
	}

	private void buildSkillsTab(int left, int top, int contentWidth) {
		int buttonHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int half = ModPanelLayout.columnWidth(contentWidth, 2, rowGap);
		List<RpgSkillStore.Skill> skills = RpgSkillStore.skills();
		int maxPage = Math.max(0, (skills.size() - 1) / SKILLS_PER_PAGE);
		page = Math.max(0, Math.min(maxPage, page));
		int start = page * SKILLS_PER_PAGE;
		int end = Math.min(skills.size(), start + SKILLS_PER_PAGE);
		for (int index = start; index < end; index++) {
			RpgSkillStore.Skill skill = skills.get(index);
			int local = index - start;
			int col = local % 2;
			int row = local / 2;
			Button button = IconPlaceholderButton.create(
				left + col * (half + rowGap),
				top + row * (buttonHeight + rowGap),
				half,
				buttonHeight,
				Component.literal(skill.name()),
				press -> {
					selectedSkillId = skill.id();
					loadSkill(skill);
					refresh();
				});
			if (skill.id().equals(selectedSkillId)) {
				ModUiSelectionState.markSelected(button);
			}
			addRenderableWidget(button);
		}

		int pageY = top + 4 * (buttonHeight + rowGap) + ModPanelLayout.SECTION_GAP;
		int third = ModPanelLayout.columnWidth(contentWidth, 3, rowGap);
		addRenderableWidget(Button.builder(Component.literal("<"), press -> {
			page = Math.max(0, page - 1);
			refresh();
		}).bounds(left, pageY, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_skills.page", page + 1, maxPage + 1), press -> {
			page = page >= maxPage ? 0 : page + 1;
			refresh();
		}).bounds(left + third + rowGap, pageY, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.literal(">"), press -> {
			page = Math.min(maxPage, page + 1);
			refresh();
		}).bounds(left + (third + rowGap) * 2, pageY, third, buttonHeight).build());

		int actionY = pageY + buttonHeight + rowGap;
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_skills.edit"), press -> {
			RpgSkillStore.find(selectedSkillId).ifPresent(this::loadSkill);
			activeTab = SkillTab.BUILDER;
			refresh();
		}).bounds(left, actionY, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_skills.new"), press -> {
			clearDraft();
			activeTab = SkillTab.BUILDER;
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
		addField(fieldLeft, y, fieldWidth, "screen.hologrammenu.rpg_skills.name", draftName, value -> draftName = value);
		y += FIELD_HEIGHT + rowGap + UiScale.s(10);
		addField(fieldLeft, y, fieldWidth, "screen.hologrammenu.rpg_skills.description", draftDescription, value -> draftDescription = value);
		y += FIELD_HEIGHT + rowGap + UiScale.s(10);
		addField(fieldLeft, y, fieldWidth, "screen.hologrammenu.rpg_skills.effect", draftEffect, value -> draftEffect = value);
		y += FIELD_HEIGHT + rowGap + UiScale.s(10);
		addField(fieldLeft, y, fieldWidth, "screen.hologrammenu.rpg_skills.bonus", draftBonus, value -> draftBonus = value);
		y += FIELD_HEIGHT + rowGap + UiScale.s(10);
		addField(fieldLeft, y, fieldWidth, "screen.hologrammenu.rpg_skills.color", draftColorCode, value -> draftColorCode = value);
		y += FIELD_HEIGHT + ModPanelLayout.SECTION_GAP + UiScale.s(12);

		int quarter = ModPanelLayout.columnWidth(formWidth, 4, rowGap);
		addAdjustButton(formLeft, y, quarter, buttonHeight, "screen.hologrammenu.rpg_skills.max_level", draftMaxLevel, ValueTarget.MAX_LEVEL);
		addAdjustButton(formLeft + quarter + rowGap, y, quarter, buttonHeight, "screen.hologrammenu.rpg_skills.base", draftBaseBonus, ValueTarget.BASE);
		addAdjustButton(formLeft + (quarter + rowGap) * 2, y, quarter, buttonHeight, "screen.hologrammenu.rpg_skills.scale", draftBonusPerLevel, ValueTarget.SCALE);
		addAdjustButton(formLeft + (quarter + rowGap) * 3, y, quarter, buttonHeight, "screen.hologrammenu.rpg_skills.preview", draftLevel, ValueTarget.LEVEL);
		y += buttonHeight + rowGap;

		int third = ModPanelLayout.columnWidth(formWidth, 3, rowGap);
		addRenderableWidget(Button.builder(Component.literal("-"), press -> {
			draftLevel = RpgSkillStore.clampLevel(draftLevel - 1);
			refresh();
		}).bounds(formLeft, y, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_skills.preview_level", draftLevel), press -> {
			draftLevel = Math.min(draftMaxLevel, RpgSkillStore.clampLevel(draftLevel + 1));
			refresh();
		}).bounds(formLeft + third + rowGap, y, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.literal("+"), press -> {
			draftLevel = Math.min(draftMaxLevel, RpgSkillStore.clampLevel(draftLevel + 1));
			refresh();
		}).bounds(formLeft + (third + rowGap) * 2, y, third, buttonHeight).build());
		y += buttonHeight + ModPanelLayout.SECTION_GAP;

		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_skills.save"), press -> saveDraft())
			.bounds(formLeft, y, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.rpg_skills.delete"), press -> {
			RpgSkillStore.remove(draftId);
			loadSkill(RpgSkillStore.first());
			activeTab = SkillTab.SKILLS;
			refresh();
		}).bounds(formLeft + third + rowGap, y, third, buttonHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), press -> onClose())
			.bounds(formLeft + (third + rowGap) * 2, y, third, buttonHeight).build());
	}

	private EditBox addField(int x, int y, int width, String labelKey, String value, java.util.function.Consumer<String> responder) {
		EditBox field = new EditBox(this.font, x, y, width, FIELD_HEIGHT, Component.translatable(labelKey));
		field.setMaxLength(90);
		field.setValue(value);
		field.setResponder(responder);
		addRenderableWidget(field);
		return field;
	}

	private void addAdjustButton(int x, int y, int width, int height, String labelKey, int value, ValueTarget target) {
		addRenderableWidget(Button.builder(Component.translatable(labelKey, value), press -> {
			adjust(target, 1);
			refresh();
		}).bounds(x, y, width, height).build());
	}

	private void adjust(ValueTarget target, int delta) {
		if (target == ValueTarget.LEVEL) {
			draftLevel = Math.min(draftMaxLevel, RpgSkillStore.clampLevel(draftLevel + delta));
		} else if (target == ValueTarget.MAX_LEVEL) {
			draftMaxLevel = RpgSkillStore.clampLevel(draftMaxLevel + delta);
			draftLevel = Math.min(draftLevel, draftMaxLevel);
		} else if (target == ValueTarget.BASE) {
			draftBaseBonus = Math.max(0, draftBaseBonus + delta);
		} else {
			draftBonusPerLevel = Math.max(0, draftBonusPerLevel + delta);
		}
	}

	private void saveDraft() {
		RpgSkillStore.Skill saved = RpgSkillStore.addOrUpdate(
			draftId,
			draftName,
			draftDescription,
			draftEffect,
			draftBonus,
			draftColorCode,
			draftMaxLevel,
			draftBaseBonus,
			draftBonusPerLevel
		);
		loadSkill(saved);
		activeTab = SkillTab.SKILLS;
		refresh();
	}

	private void loadSkill(RpgSkillStore.Skill skill) {
		selectedSkillId = skill.id();
		draftId = skill.id();
		draftName = skill.name();
		draftDescription = skill.description();
		draftEffect = skill.effect();
		draftBonus = skill.bonus();
		draftColorCode = skill.colorCode();
		draftMaxLevel = skill.maxLevel();
		draftLevel = Math.min(draftLevel, draftMaxLevel);
		draftBaseBonus = skill.baseBonus();
		draftBonusPerLevel = skill.bonusPerLevel();
	}

	private void clearDraft() {
		draftId = "";
		draftName = "Custom Skill";
		draftDescription = "Custom skill.";
		draftEffect = "Passive";
		draftBonus = "Bonus";
		draftColorCode = "&f";
		draftLevel = 1;
		draftMaxLevel = 100;
		draftBaseBonus = 1;
		draftBonusPerLevel = 1;
	}

	private RpgSkillStore.Skill draftSkill() {
		return new RpgSkillStore.Skill(
			"preview",
			draftName,
			draftDescription,
			draftEffect,
			draftBonus,
			draftColorCode,
			draftMaxLevel,
			draftBaseBonus,
			draftBonusPerLevel
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
		int top = ModPanelLayout.centeredContentTop(this.height, UiScale.s(224));
		UiScaleText.drawCentered(graphics, this.font, this.title, this.width / 2, ModPanelLayout.titleY(top), 0xFFFFFF);
		UiScaleText.drawCentered(
			graphics,
			this.font,
			Component.translatable("screen.hologrammenu.rpg_skills.hint"),
			this.width / 2,
			ModPanelLayout.hintY(top),
			0xA0A0A0
		);
		int labelY = top + UiLayoutHelper.buttonHeight(this.font) + ModPanelLayout.SECTION_GAP;
		UiScaleText.draw(graphics, this.font, activeTab == SkillTab.SKILLS
			? Component.translatable("screen.hologrammenu.rpg_skills.skills_label")
			: Component.translatable("screen.hologrammenu.rpg_skills.builder_label"), left, labelY, 0xA0A0A0);
		if (activeTab == SkillTab.BUILDER) {
			drawColorCodeIndex(graphics, left, labelY + UiScale.s(12));
			drawBuilderLabels(graphics, left + INDEX_WIDTH + ModPanelLayout.ROW_GAP, labelY + UiScale.s(12));
		}
		UiScaleText.drawCentered(
			graphics,
			this.font,
			Component.literal(draftSkill().preview(draftLevel)),
			this.width / 2,
			top + UiScale.s(216),
			0xFFFFFF
		);
	}

	private void drawBuilderLabels(GuiGraphicsExtractor graphics, int left, int top) {
		int y = top - UiScale.s(9);
		UiScaleText.draw(graphics, this.font, Component.translatable("screen.hologrammenu.rpg_skills.name"), left, y, 0xA0A0A0);
		y += FIELD_HEIGHT + ModPanelLayout.ROW_GAP + UiScale.s(10);
		UiScaleText.draw(graphics, this.font, Component.translatable("screen.hologrammenu.rpg_skills.description"), left, y, 0xA0A0A0);
		y += FIELD_HEIGHT + ModPanelLayout.ROW_GAP + UiScale.s(10);
		UiScaleText.draw(graphics, this.font, Component.translatable("screen.hologrammenu.rpg_skills.effect"), left, y, 0xA0A0A0);
		y += FIELD_HEIGHT + ModPanelLayout.ROW_GAP + UiScale.s(10);
		UiScaleText.draw(graphics, this.font, Component.translatable("screen.hologrammenu.rpg_skills.bonus"), left, y, 0xA0A0A0);
		y += FIELD_HEIGHT + ModPanelLayout.ROW_GAP + UiScale.s(10);
		UiScaleText.draw(graphics, this.font, Component.translatable("screen.hologrammenu.rpg_skills.color"), left, y, 0xA0A0A0);
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

	private enum SkillTab {
		SKILLS,
		BUILDER
	}

	private enum ValueTarget {
		LEVEL,
		MAX_LEVEL,
		BASE,
		SCALE
	}
}
