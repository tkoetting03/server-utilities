package com.hologrammenu.client.screen;

import com.hologrammenu.client.mixin.accessor.EditBoxAccessor;
import com.hologrammenu.client.mixin.accessor.MultiLineEditBoxAccessor;
import com.hologrammenu.client.mixin.accessor.MultilineTextFieldAccessor;
import com.hologrammenu.client.mixin.accessor.ScreenInvoker;
import com.hologrammenu.client.screen.ModUiSelectionState;
import com.hologrammenu.client.screen.widget.ClassicColorSwatchButton;
import com.hologrammenu.client.screen.widget.DraggableTitleBarWidget;
import com.hologrammenu.client.screen.widget.GradientPreviewWidget;
import com.hologrammenu.client.screen.widget.LabeledFieldLayout;
import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.PartSelectEditBox;
import com.hologrammenu.client.screen.widget.RgbColorPickerWidget;
import com.hologrammenu.client.screen.widget.TextStylePanelLayout;
import com.hologrammenu.client.screen.widget.TextStylePanelWidget;
import com.hologrammenu.client.screen.widget.UiLayoutHelper;
import com.hologrammenu.client.screen.widget.UiScale;
import com.hologrammenu.client.screen.widget.VanillaIconButton;
import com.hologrammenu.client.screen.widget.AnvilEditorMetrics;
import com.hologrammenu.client.screen.widget.AnvilEditorPanelWidget;
import com.hologrammenu.network.ModPackets;
import com.hologrammenu.rpg.RpgCustomEffectStore;
import com.hologrammenu.rpg.RpgEffectCatalog;
import com.hologrammenu.storage.StorageMenuItemLore;
import com.hologrammenu.text.StyledSpan;
import com.hologrammenu.text.StyledText;
import com.hologrammenu.text.TextFormats;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class TextStyleOverlay {
	private static final int MAX_PARTS = 6;
	private static final int BUTTON_PAD = 0;
	private static final int DEFAULT_LORE_BASE_COLOR = 0xA0A0A0;
	private static final int DEFAULT_LORE_ACCENT_COLOR = 0x55FFFF;
	private static final StyledText.Effect[] ALL_EFFECTS = {
		StyledText.Effect.BOLD,
		StyledText.Effect.ITALIC,
		StyledText.Effect.UNDERLINE,
		StyledText.Effect.OBFUSCATED,
		StyledText.Effect.STRIKETHROUGH
	};

	private static final Map<Screen, TextStyleOverlay> ACTIVE = new WeakHashMap<>();

	private final Screen parent;
	private final Supplier<String> plainTextSupplier;
	private final TextStyleTarget target;
	private final Supplier<int[]> panelPositionSupplier;
	private final Supplier<ItemStack> anvilStackSupplier;
	private final EditorEffectsMode effectsMode;
	private final ScreenInvoker screenInvoker;
	private final List<GuiEventListener> widgets = new ArrayList<>();
	private final List<TextPart> parts = new ArrayList<>();
	private final List<EditBox> partFields = new ArrayList<>();
	private final List<String> loreLines = new ArrayList<>();
	private final List<EditBox> loreLineFields = new ArrayList<>();
	private MultiLineEditBox loreParagraphField;
	private StyledText loreParagraphDraft = StyledText.EMPTY;

	private StyledText draft = StyledText.EMPTY;
	private RgbColorPickerWidget colorPicker;
	private int gradientStartColor = 0xFFFFFF;
	private int gradientEndColor = 0x55FFFF;
	private GradientTarget gradientTarget = GradientTarget.START;
	private Button solidModeButton;
	private Button gradientModeButton;
	private Button gradientStartButton;
	private Button gradientEndButton;
	private GradientPreviewWidget gradientPreview;
	private ColorEditMode colorEditMode = ColorEditMode.SOLID;
	private final Map<StyledText.Effect, Button> effectButtons = new EnumMap<>(StyledText.Effect.class);
	private final Set<AbstractWidget> selectionWidgets = Collections.newSetFromMap(new IdentityHashMap<>());
	private int selectedPartIndex;
	private boolean open;
	private Runnable onClose;
	private DraggablePanelGroup dragGroup;
	private AnvilEditorTab anvilActiveTab = AnvilEditorTab.STYLE;
	private AnvilStyleFocus anvilStyleFocus = AnvilStyleFocus.RENAME;
	private int selectedLoreLineIndex;
	private LoreDynamicStyle loreDynamicStyle = LoreDynamicStyle.CLEAN;
	private int loreBaseColor = DEFAULT_LORE_BASE_COLOR;
	private int loreAccentColor = DEFAULT_LORE_ACCENT_COLOR;
	private int loreWrapWidth = 32;
	private boolean loreColorTableOpen;
	private SelectionRange loreColorSelection = SelectionRange.NONE;
	private final EnumSet<StyledText.Effect> loreParagraphEffects = EnumSet.noneOf(StyledText.Effect.class);
	private Button anvilStyleTabButton;
	private Button anvilLoreTabButton;
	private Button anvilEffectsTabButton;
	private final Map<String, Button> rpgEffectButtons = new java.util.HashMap<>();
	private final Map<String, Button> vanillaEnchantButtons = new java.util.HashMap<>();
	private String selectedRpgEffectId = RpgEffectCatalog.first().id();
	private int selectedRpgEffectLevel = RpgEffectCatalog.MIN_LEVEL;
	private EffectsSubTab effectsSubTab = EffectsSubTab.RPG;
	private String selectedVanillaEnchantId = "minecraft:protection";
	private int selectedVanillaEnchantLevel = 1;
	private int vanillaEnchantPage;
	private EditBox vanillaEnchantLevelField;
	private int swordCritChance = 10;
	private int swordCooldownReduction = 0;
	private int swordLifeSteal = 0;
	private int swordBleedChance = 0;
	private int swordArmorPierce = 0;
	private int swordExecuteDamage = 0;
	private int armorHealthBonus = 0;
	private int armorDefenseBonus = 0;
	private int armorRegenBonus = 0;

	private static final int ENCHANTS_PER_PAGE = 8;
	private static final SwordAspect[] SWORD_ASPECTS = {
		new SwordAspect("Critical Chance", "&e", "+%s%% Crit", 0, 100, 5),
		new SwordAspect("Cooldown", "&b", "-%ss Cooldown", 0, 40, 2),
		new SwordAspect("Parasitic Regen", "&4", "%s%% Life Steal", 0, 50, 5),
		new SwordAspect("Bleed Chance", "&c", "%s%% Bleed", 0, 100, 5),
		new SwordAspect("Armor Pierce", "&6", "%s%% Pierce", 0, 75, 5),
		new SwordAspect("Execute Damage", "&d", "+%s%% Execute", 0, 100, 5)
	};
	private static final ArmorAspect[] ARMOR_ASPECTS = {
		new ArmorAspect("Max Health", "&c", "+%s Health", 0, 40, 2),
		new ArmorAspect("Armor Defense", "&9", "+%s Defense", 0, 100, 5),
		new ArmorAspect("Health Regen", "&a", "+%s Regen", 0, 50, 5)
	};
	private static final List<VanillaEnchantOption> VANILLA_ENCHANTS = List.of(
		new VanillaEnchantOption("minecraft:protection", "Protection"),
		new VanillaEnchantOption("minecraft:fire_protection", "Fire Prot"),
		new VanillaEnchantOption("minecraft:feather_falling", "Feather Fall"),
		new VanillaEnchantOption("minecraft:blast_protection", "Blast Prot"),
		new VanillaEnchantOption("minecraft:projectile_protection", "Proj Prot"),
		new VanillaEnchantOption("minecraft:respiration", "Respiration"),
		new VanillaEnchantOption("minecraft:aqua_affinity", "Aqua Affinity"),
		new VanillaEnchantOption("minecraft:thorns", "Thorns"),
		new VanillaEnchantOption("minecraft:depth_strider", "Depth Strider"),
		new VanillaEnchantOption("minecraft:frost_walker", "Frost Walker"),
		new VanillaEnchantOption("minecraft:binding_curse", "Binding"),
		new VanillaEnchantOption("minecraft:soul_speed", "Soul Speed"),
		new VanillaEnchantOption("minecraft:swift_sneak", "Swift Sneak"),
		new VanillaEnchantOption("minecraft:sharpness", "Sharpness"),
		new VanillaEnchantOption("minecraft:smite", "Smite"),
		new VanillaEnchantOption("minecraft:bane_of_arthropods", "Bane"),
		new VanillaEnchantOption("minecraft:knockback", "Knockback"),
		new VanillaEnchantOption("minecraft:fire_aspect", "Fire Aspect"),
		new VanillaEnchantOption("minecraft:looting", "Looting"),
		new VanillaEnchantOption("minecraft:sweeping_edge", "Sweeping"),
		new VanillaEnchantOption("minecraft:efficiency", "Efficiency"),
		new VanillaEnchantOption("minecraft:silk_touch", "Silk Touch"),
		new VanillaEnchantOption("minecraft:unbreaking", "Unbreaking"),
		new VanillaEnchantOption("minecraft:fortune", "Fortune"),
		new VanillaEnchantOption("minecraft:power", "Power"),
		new VanillaEnchantOption("minecraft:punch", "Punch"),
		new VanillaEnchantOption("minecraft:flame", "Flame"),
		new VanillaEnchantOption("minecraft:infinity", "Infinity"),
		new VanillaEnchantOption("minecraft:luck_of_the_sea", "Sea Luck"),
		new VanillaEnchantOption("minecraft:lure", "Lure"),
		new VanillaEnchantOption("minecraft:loyalty", "Loyalty"),
		new VanillaEnchantOption("minecraft:impaling", "Impaling"),
		new VanillaEnchantOption("minecraft:riptide", "Riptide"),
		new VanillaEnchantOption("minecraft:channeling", "Channeling"),
		new VanillaEnchantOption("minecraft:multishot", "Multishot"),
		new VanillaEnchantOption("minecraft:quick_charge", "Quick Charge"),
		new VanillaEnchantOption("minecraft:piercing", "Piercing"),
		new VanillaEnchantOption("minecraft:density", "Density"),
		new VanillaEnchantOption("minecraft:breach", "Breach"),
		new VanillaEnchantOption("minecraft:wind_burst", "Wind Burst"),
		new VanillaEnchantOption("minecraft:lunge", "Lunge"),
		new VanillaEnchantOption("minecraft:mending", "Mending"),
		new VanillaEnchantOption("minecraft:vanishing_curse", "Vanishing")
	);

	public TextStyleOverlay(
		Screen parent,
		Supplier<String> plainTextSupplier,
		TextStyleTarget target,
		Supplier<int[]> panelPositionSupplier
	) {
		this(parent, plainTextSupplier, target, panelPositionSupplier, null, EditorEffectsMode.NONE);
	}

	public static TextStyleOverlay forAnvil(
		Screen parent,
		Supplier<String> plainTextSupplier,
		TextStyleTarget target,
		Supplier<int[]> panelPositionSupplier,
		Supplier<ItemStack> stackSupplier
	) {
		return new TextStyleOverlay(parent, plainTextSupplier, target, panelPositionSupplier, stackSupplier, EditorEffectsMode.ALL);
	}

	public static TextStyleOverlay forItemStyler(
		Screen parent,
		Supplier<String> plainTextSupplier,
		TextStyleTarget target,
		Supplier<int[]> panelPositionSupplier,
		Supplier<ItemStack> stackSupplier
	) {
		return new TextStyleOverlay(parent, plainTextSupplier, target, panelPositionSupplier, stackSupplier, EditorEffectsMode.ENCHANTS_ONLY);
	}

	public TextStyleOverlay(
		Screen parent,
		Supplier<String> plainTextSupplier,
		TextStyleTarget target,
		Supplier<int[]> panelPositionSupplier,
		Supplier<ItemStack> anvilStackSupplier
	) {
		this(parent, plainTextSupplier, target, panelPositionSupplier, anvilStackSupplier, anvilStackSupplier != null ? EditorEffectsMode.ALL : EditorEffectsMode.NONE);
	}

	private TextStyleOverlay(
		Screen parent,
		Supplier<String> plainTextSupplier,
		TextStyleTarget target,
		Supplier<int[]> panelPositionSupplier,
		Supplier<ItemStack> anvilStackSupplier,
		EditorEffectsMode effectsMode
	) {
		this.parent = parent;
		this.plainTextSupplier = plainTextSupplier;
		this.target = target;
		this.panelPositionSupplier = panelPositionSupplier;
		this.anvilStackSupplier = anvilStackSupplier;
		this.effectsMode = effectsMode;
		this.screenInvoker = (ScreenInvoker) parent;
	}

	public boolean isAnvilMode() {
		return anvilStackSupplier != null;
	}

	public AnvilEditorTab getAnvilActiveTab() {
		return anvilActiveTab;
	}

	public static TextStyleOverlay getActive(Screen screen) {
		return ACTIVE.get(screen);
	}

	public static void onScreenRemoved(Screen screen) {
		TextStyleOverlay overlay = ACTIVE.remove(screen);
		if (overlay != null) {
			overlay.close();
		}
	}

	public boolean isOpen() {
		return open;
	}

	public boolean isOverlayEditBox(EditBox editBox) {
		return partFields.contains(editBox) || loreLineFields.contains(editBox);
	}

	public boolean isOverlayTextInput(GuiEventListener listener) {
		if (listener instanceof EditBox editBox) {
			return isOverlayEditBox(editBox);
		}
		return listener == loreParagraphField;
	}

	public StyledText getDraft() {
		return currentDraft();
	}

	public String getPlainText() {
		syncPartsFromFields();
		StringBuilder builder = new StringBuilder();
		for (TextPart part : parts) {
			builder.append(part.text);
		}
		return builder.toString();
	}

	public void dispose() {
		if (open) {
			tearDownWidgets();
			open = false;
			ACTIVE.remove(parent);
		}
	}

	public void toggle(String storedSerialized, int panelX, int panelY) {
		if (open) {
			close();
		} else {
			open(storedSerialized, panelX, panelY);
		}
	}

	public void toggleAnvilTab(String storedSerialized, int panelX, int panelY, AnvilEditorTab tab) {
		if (!isAnvilMode()) {
			toggle(storedSerialized, panelX, panelY);
			return;
		}
		if (open && anvilActiveTab == tab) {
			close();
			return;
		}
		if (!open) {
			open(storedSerialized, panelX, panelY);
		}
		switchAnvilTab(tab);
	}

	public void open(String storedSerialized, int panelX, int panelY) {
		if (open) {
			return;
		}

		if (isAnvilMode()) {
			anvilActiveTab = AnvilEditorTab.STYLE;
			anvilStyleFocus = AnvilStyleFocus.RENAME;
			loreLines.clear();
		}
		draft = TextFormats.parse(target.readSerialized(storedSerialized, plainTextSupplier));
		loadPartsFromDraft(draft);
		open = true;
		ACTIVE.put(parent, this);
		buildWidgets(panelX, panelY);
	}

	public void openWithDraft(StyledText savedDraft) {
		openWithDraft(savedDraft, AnvilEditorTab.STYLE);
	}

	public void openWithDraft(StyledText savedDraft, AnvilEditorTab restoredTab) {
		if (open) {
			return;
		}

		if (isAnvilMode()) {
			anvilActiveTab = restoredTab == AnvilEditorTab.EFFECTS && effectsMode == EditorEffectsMode.NONE ? AnvilEditorTab.STYLE : restoredTab;
			anvilStyleFocus = AnvilStyleFocus.RENAME;
		}
		draft = savedDraft.withText(stylePlainTextSupplier().get());
		loadPartsFromDraft(draft);
		open = true;
		ACTIVE.put(parent, this);
		int[] position = panelPositionSupplier.get();
		buildWidgets(position[0], position[1]);
	}

	public void setOnClose(Runnable onClose) {
		this.onClose = onClose;
	}

	public void relayout() {
		if (!open) {
			return;
		}

		if (anvilActiveTab == AnvilEditorTab.STYLE) {
			syncPartsFromFields();
		} else if (anvilActiveTab == AnvilEditorTab.LORE) {
			syncLoreLinesFromFields();
		}
		int[] position = panelPositionSupplier.get();
		tearDownWidgets();
		buildWidgets(position[0], position[1]);
	}

	private Supplier<String> stylePlainTextSupplier() {
		if (isAnvilMode() && anvilStyleFocus == AnvilStyleFocus.LORE_LINE) {
			return () -> TextFormats.parse(currentSelectedLoreSerialized()).text();
		}
		return plainTextSupplier;
	}

	private int currentPanelHeight() {
		int partCount = parts.size();
		boolean gradientExpanded = colorEditMode == ColorEditMode.GRADIENT;
		if (!isAnvilMode()) {
			return TextStylePanelWidget.panelHeight(partCount, 0, gradientExpanded);
		}
		int lineCount = Math.max(1, Math.min(loreLines.size(), StorageMenuItemLore.MAX_LINES));
		return AnvilEditorPanelWidget.panelHeight(
			anvilActiveTab,
			partCount,
			lineCount,
			gradientExpanded,
			anvilActiveTab == AnvilEditorTab.LORE && loreColorTableOpen
		);
	}

	private void loadPartsFromDraft(StyledText value) {
		parts.clear();
		for (StyledSpan span : value.spans()) {
			if (!span.text().isEmpty() || parts.isEmpty()) {
				parts.add(TextPart.fromSpan(span, value.effects()));
			}
		}
		if (parts.isEmpty()) {
			parts.add(TextPart.empty());
		}
		selectedPartIndex = 0;
		syncColorModeFromSelectedPart();
	}

	private void buildWidgets(int panelX, int panelY) {
		int partCount = parts.size();
		int lineCount = Math.max(1, Math.min(loreLines.size(), StorageMenuItemLore.MAX_LINES));
		int panelHeight = currentPanelHeight();
		int defaultX = panelX;
		int defaultY = panelY;
		dragGroup = new DraggablePanelGroup(parent, isAnvilMode() ? "anvil_editor" : "text_style");
		int[] position = dragGroup.resolvePosition(defaultX, defaultY, TextStylePanelWidget.PANEL_WIDTH, panelHeight);
		panelX = position[0];
		panelY = position[1];

		if (isAnvilMode()) {
			var panelWidget = new AnvilEditorPanelWidget(
				panelX,
				panelY,
				anvilActiveTab,
				partCount,
				lineCount,
				colorEditMode == ColorEditMode.GRADIENT,
				anvilActiveTab == AnvilEditorTab.LORE && loreColorTableOpen
			);
			screenInvoker.hologrammenu$addRenderableOnly(panelWidget);
			widgets.add(panelWidget);
			dragGroup.track(panelWidget);
			attachAnvilTabs(panelX, panelY);
			if (anvilActiveTab == AnvilEditorTab.STYLE) {
				buildStyleWidgets(panelX, panelY, AnvilEditorMetrics.tabRowHeight(), partCount);
			} else if (anvilActiveTab == AnvilEditorTab.LORE) {
				buildLoreWidgets(panelX, panelY, lineCount);
			} else {
					buildEffectsWidgets(panelX, panelY);
			}
		} else {
			var panelWidget = new TextStylePanelWidget(panelX, panelY, partCount, 0, colorEditMode == ColorEditMode.GRADIENT);
			screenInvoker.hologrammenu$addRenderableOnly(panelWidget);
			widgets.add(panelWidget);
			dragGroup.track(panelWidget);
			buildStyleWidgets(panelX, panelY, 0, partCount);
		}

		DraggableTitleBarWidget titleBar = dragGroup.createTitleBar(
			isAnvilMode()
				? Component.translatable("screen.hologrammenu.anvil.editor_title")
				: Component.translatable("screen.hologrammenu.text_style.title"),
			TextStylePanelWidget.PANEL_WIDTH,
			ModPanelLayout.TITLE_BAR_HEIGHT
		);
		attach(titleBar, true);
		if (anvilActiveTab == AnvilEditorTab.STYLE) {
			refreshSelectionOutlines();
		} else if (anvilActiveTab == AnvilEditorTab.LORE) {
			refreshLoreSelectionOutlines();
		} else {
			refreshRpgEffectSelectionOutlines();
		}
		updateAnvilTabSelection();
	}

	private void buildStyleWidgets(int panelX, int panelY, int contentTopOffset, int partCount) {
		boolean gradientExpanded = colorEditMode == ColorEditMode.GRADIENT;
		TextStylePanelLayout.Metrics layout = TextStylePanelLayout.metrics(partCount, contentTopOffset, gradientExpanded);
		int left = panelX + TextStylePanelLayout.CONTENT_LEFT;
		int contentWidth = TextStylePanelLayout.CONTENT_WIDTH;
		int gap = layout.buttonRowGap();
		int buttonHeight = layout.buttonHeight();
		buildPartFields(panelX, panelY, partCount, contentTopOffset, layout);

		int gridLeft = layout.classicGridLeft(panelX);
		int classicTop = panelY + layout.classicTop(partCount);
		int swatch = layout.classicSwatch();
		int swatchGap = layout.classicGap();
		int column = 0;
		int row = 0;

		for (TextFormats.ColorOption color : TextFormats.COLORS.values()) {
			int x = gridLeft + column * (swatch + swatchGap);
			int y = classicTop + row * (swatch + swatchGap);
			attach(new ClassicColorSwatchButton(x, y, swatch, color.previewColor(), () -> {
				int rgb = color.previewColor();
				if (colorPicker != null) {
					colorPicker.setColor(rgb);
				}
				if (colorEditMode == ColorEditMode.GRADIENT) {
					setActiveGradientColor(rgb);
				} else {
					applySolidColorToSelectedPart(rgb);
				}
			}));

			column++;
			if (column >= TextStylePanelLayout.CLASSIC_COLUMNS) {
				column = 0;
				row++;
			}
		}

		int customTop = panelY + layout.customTop(partCount);
		syncGradientColorsFromSelectedPart();
		int pickerLeft = layout.pickerLeft(panelX);
		colorPicker = new RgbColorPickerWidget(
			pickerLeft,
			customTop,
			layout.pickerWidth(),
			activePickerColor(),
			this::onPickerColorChanged,
			TextStylePanelLayout.PICKER_SCALE
		);
		attach(colorPicker);

		int modeTop = panelY + layout.colorModeTop(partCount);
		var modePlacements = UiLayoutHelper.layoutEqualRow(left, contentWidth, gap, 2);
		solidModeButton = Button.builder(TextStylePanelLayout.solidModeLabel(), press -> {
			selectColorMode(ColorEditMode.SOLID);
			releaseButtonFocus(press);
		})
			.bounds(modePlacements.get(0).x(), modeTop, modePlacements.get(0).width(), buttonHeight)
			.build();
		gradientModeButton = Button.builder(TextStylePanelLayout.gradientModeLabel(), press -> {
			selectColorMode(ColorEditMode.GRADIENT);
			releaseButtonFocus(press);
		})
			.bounds(modePlacements.get(1).x(), modeTop, modePlacements.get(1).width(), buttonHeight)
			.build();
		attach(solidModeButton);
		attach(gradientModeButton);

		if (gradientExpanded) {
			int previewTop = panelY + layout.gradientPreviewTop(partCount);
			gradientPreview = new GradientPreviewWidget(
				left,
				previewTop,
				contentWidth,
				layout.gradientPreviewHeight(),
				() -> gradientStartColor,
				() -> gradientEndColor
			);
			attach(gradientPreview, false);

			int targetTop = panelY + layout.gradientTargetTop(partCount);
			var gradientPlacements = UiLayoutHelper.layoutEqualRow(left, contentWidth, gap, 2);
			gradientStartButton = Button.builder(Component.translatable("screen.hologrammenu.text_style.gradient_start"), press -> {
					setGradientTarget(GradientTarget.START);
					releaseButtonFocus(press);
				})
				.bounds(
					gradientPlacements.get(0).x(),
					targetTop,
					gradientPlacements.get(0).width(),
					buttonHeight
				)
				.build();
			gradientEndButton = Button.builder(Component.translatable("screen.hologrammenu.text_style.gradient_end"), press -> {
					setGradientTarget(GradientTarget.END);
					releaseButtonFocus(press);
				})
				.bounds(
					gradientPlacements.get(1).x(),
					targetTop,
					gradientPlacements.get(1).width(),
					buttonHeight
				)
				.build();
			attach(gradientStartButton);
			attach(gradientEndButton);
		} else {
			gradientPreview = null;
			gradientStartButton = null;
			gradientEndButton = null;
		}
		updateColorModeButtons();
		updateGradientTargetButtons();

		int effectY = panelY + layout.effectsTop(partCount);
		int effectHeight = layout.effectButtonHeight();
		int effectGap = layout.effectGridGap();
		effectButtons.clear();
		boolean firstEffectRow = true;
		for (Component[] rowLabels : layout.effectRows()) {
			if (firstEffectRow) {
				attachPrimaryEffectButtonRow(left, effectY, contentWidth, effectHeight, effectGap, rowLabels);
				firstEffectRow = false;
			} else {
				attachEffectButtonRow(left, effectY, contentWidth, effectHeight, effectGap, rowLabels);
			}
			effectY += effectHeight + effectGap;
		}

		int footerY = panelY + layout.footerTop(partCount);
		for (Component[] rowLabels : layout.footerRows()) {
			attachEqualButtonRow(left, footerY, contentWidth, buttonHeight, gap, rowLabels, footerHandlersFor(rowLabels));
			footerY += buttonHeight + gap;
		}
	}

	private void attachEqualButtonRow(
		int left,
		int y,
		int contentWidth,
		int height,
		int gap,
		Component[] labels,
		Runnable[] handlers
	) {
		var placements = UiLayoutHelper.layoutEqualRow(left, contentWidth, gap, labels.length);
		for (int index = 0; index < labels.length; index++) {
			UiLayoutHelper.ButtonPlacement placement = placements.get(index);
			int handlerIndex = index;
			attach(Button.builder(labels[index], press -> {
				handlers[handlerIndex].run();
				releaseButtonFocus(press);
			})
				.bounds(placement.x(), y, placement.width(), height)
				.build());
		}
	}

	private void releaseButtonFocus(Button button) {
		button.setFocused(false);
		if (parent.getFocused() == button) {
			parent.setFocused(null);
		}
	}

	private void selectColorMode(ColorEditMode mode) {
		if (colorEditMode == mode) {
			if (mode == ColorEditMode.GRADIENT) {
				applyGradientToSelectedPart(gradientStartColor, gradientEndColor);
			} else if (colorPicker != null) {
				applySolidColorToSelectedPart(colorPicker.getColor());
			}
			return;
		}
		colorEditMode = mode;
		if (mode == ColorEditMode.SOLID) {
			if (colorPicker != null) {
				applySolidColorToSelectedPart(colorPicker.getColor());
			}
		} else {
			syncGradientColorsFromSelectedPart();
			gradientTarget = GradientTarget.START;
			applyGradientToSelectedPart(gradientStartColor, gradientEndColor);
		}
		relayout();
	}

	private void syncColorModeFromSelectedPart() {
		colorEditMode = selectedPart().gradientEnd.isPresent() ? ColorEditMode.GRADIENT : ColorEditMode.SOLID;
	}

	private void updateColorModeButtons() {
		if (solidModeButton == null || gradientModeButton == null) {
			return;
		}
		solidModeButton.active = true;
		gradientModeButton.active = true;
		refreshSelectionOutlines();
	}

	private int activePickerColor() {
		if (colorEditMode == ColorEditMode.GRADIENT) {
			return activeGradientColor();
		}
		TextPart part = selectedPart();
		if (part.color.isPresent()) {
			return part.color.getAsInt();
		}
		return colorPicker != null ? colorPicker.getColor() : 0xFFFFFF;
	}

	private Runnable[] footerHandlersFor(Component[] labels) {
		Runnable[] handlers = new Runnable[labels.length];
		for (int index = 0; index < labels.length; index++) {
			handlers[index] = switch (labelKey(labels[index])) {
				case "screen.hologrammenu.text_style.reset" -> this::resetParts;
				case "gui.done" -> this::close;
				default -> throw new IllegalArgumentException("Unknown footer label: " + labels[index]);
			};
		}
		return handlers;
	}

	private void attachPrimaryEffectButtonRow(
		int left,
		int y,
		int contentWidth,
		int height,
		int gap,
		Component[] labels
	) {
		if (labels.length != 3) {
			attachEffectButtonRow(left, y, contentWidth, height, gap, labels);
			return;
		}

		var font = Minecraft.getInstance().font;
		Component boldLabel = labels[0];
		Component underlineLabel = labels[1];
		Component italicLabel = labels[2];
		int underlineWidth = UiLayoutHelper.buttonWidth(font, underlineLabel, UiScale.s(10));
		int sideWidth = Math.max(
			UiLayoutHelper.buttonWidth(font, boldLabel, UiScale.s(4)),
			(contentWidth - underlineWidth - gap * 2) / 2
		);
		underlineWidth = contentWidth - sideWidth * 2 - gap * 2;

		int x = left;
		attachEffectButton(boldLabel, x, y, sideWidth, height);
		x += sideWidth + gap;
		attachEffectButton(underlineLabel, x, y, underlineWidth, height);
		x += underlineWidth + gap;
		attachEffectButton(italicLabel, x, y, contentWidth - (x - left), height);
	}

	private void attachEffectButton(Component label, int x, int y, int width, int height) {
		StyledText.Effect effect = effectForLabel(label);
		Button effectButton = Button.builder(label, press -> {
			toggleEffectOnSelectedPart(effect);
			releaseButtonFocus(press);
		})
			.bounds(x, y, width, height)
			.build();
		effectButtons.put(effect, effectButton);
		ModUiSelectionState.registerEffectButton(effectButton);
		attach(effectButton);
	}

	private void attachEffectButtonRow(
		int left,
		int y,
		int contentWidth,
		int height,
		int gap,
		Component[] labels
	) {
		var placements = UiLayoutHelper.layoutEqualRow(left, contentWidth, gap, labels.length);
		for (int index = 0; index < labels.length; index++) {
			UiLayoutHelper.ButtonPlacement placement = placements.get(index);
			attachEffectButton(labels[index], placement.x(), y, placement.width(), height);
		}
	}

	private StyledText.Effect effectForLabel(Component label) {
		String key = labelKey(label);
		for (TextFormats.EffectOption option : TextFormats.EFFECTS.values()) {
			if (key.equals("screen.hologrammenu.text_style.effect." + option.id())) {
				return option.effect();
			}
		}
		throw new IllegalArgumentException("Unknown effect label: " + label);
	}

	private static String labelKey(Component component) {
		if (component.getContents() instanceof TranslatableContents contents) {
			return contents.getKey();
		}
		return component.getString();
	}

	private void attachAnvilTabs(int panelX, int panelY) {
		int left = panelX + ModPanelLayout.PANEL_PADDING;
		int contentWidth = ModPanelLayout.CONTENT_WIDTH;
		int tabY = panelY + ModPanelLayout.CONTENT_TOP;
		int tabGap = ModPanelLayout.ROW_GAP;
		int tabHeight = AnvilEditorMetrics.tabButtonHeight();
		int tabCount = effectsMode == EditorEffectsMode.NONE ? 2 : 3;
		int tabWidth = ModPanelLayout.columnWidth(contentWidth, tabCount, tabGap);
		anvilStyleTabButton = Button.builder(Component.translatable("screen.hologrammenu.anvil.tab.style"), press -> {
				switchAnvilTab(AnvilEditorTab.STYLE);
				releaseButtonFocus(press);
			})
			.bounds(left, tabY, tabWidth, tabHeight)
			.build();
		anvilLoreTabButton = Button.builder(Component.translatable("screen.hologrammenu.anvil.tab.lore"), press -> {
				switchAnvilTab(AnvilEditorTab.LORE);
				releaseButtonFocus(press);
			})
			.bounds(left + tabWidth + tabGap, tabY, tabWidth, tabHeight)
			.build();
		attach(anvilStyleTabButton);
		attach(anvilLoreTabButton);
		if (effectsMode != EditorEffectsMode.NONE) {
			String labelKey = effectsMode == EditorEffectsMode.ENCHANTS_ONLY
				? "screen.hologrammenu.anvil.effects_tab.enchants"
				: "screen.hologrammenu.anvil.tab.effects";
			anvilEffectsTabButton = Button.builder(Component.translatable(labelKey), press -> {
					switchAnvilTab(AnvilEditorTab.EFFECTS);
					releaseButtonFocus(press);
				})
				.bounds(left + (tabWidth + tabGap) * 2, tabY, tabWidth, tabHeight)
				.build();
			attach(anvilEffectsTabButton);
		} else {
			anvilEffectsTabButton = null;
		}
	}

	private void switchAnvilTab(AnvilEditorTab tab) {
		if (!isAnvilMode() || anvilActiveTab == tab) {
			return;
		}
		if (tab == AnvilEditorTab.EFFECTS && effectsMode == EditorEffectsMode.NONE) {
			return;
		}
		if (anvilActiveTab == AnvilEditorTab.STYLE) {
			syncPartsFromFields();
			if (anvilStyleFocus == AnvilStyleFocus.LORE_LINE) {
				updateSelectedLoreLine(currentDraft().serialize());
			} else {
				applyDraftToTarget();
			}
		} else if (anvilActiveTab == AnvilEditorTab.LORE) {
			syncLoreLinesFromFields();
			applyLore();
		}
		if (tab == AnvilEditorTab.LORE || tab == AnvilEditorTab.EFFECTS) {
			ItemStack stack = anvilStackSupplier.get();
			if (stack.isEmpty()) {
				showAnvilMessage(Component.translatable(tab == AnvilEditorTab.LORE
					? "screen.hologrammenu.anvil.lore_no_item"
					: "screen.hologrammenu.anvil.effects_no_item"));
				return;
			}
			if (loreLines.isEmpty()) {
				loadLoreFromStack(stack);
			}
		}
		if (tab == AnvilEditorTab.STYLE) {
			anvilStyleFocus = AnvilStyleFocus.RENAME;
			draft = TextFormats.parse(target.readSerialized("", plainTextSupplier));
			loadPartsFromDraft(draft);
		}
		anvilActiveTab = tab;
		relayout();
	}

	private void updateAnvilTabSelection() {
		if (!isAnvilMode() || anvilStyleTabButton == null || anvilLoreTabButton == null) {
			return;
		}
		ModUiSelectionState.unmarkSelected(anvilStyleTabButton);
		ModUiSelectionState.unmarkSelected(anvilLoreTabButton);
		if (anvilEffectsTabButton != null) {
			ModUiSelectionState.unmarkSelected(anvilEffectsTabButton);
		}
		switch (anvilActiveTab) {
			case STYLE -> ModUiSelectionState.markSelected(anvilStyleTabButton);
			case LORE -> ModUiSelectionState.markSelected(anvilLoreTabButton);
			case EFFECTS -> {
				if (anvilEffectsTabButton != null) {
					ModUiSelectionState.markSelected(anvilEffectsTabButton);
				}
			}
		}
	}

	private void buildLoreWidgets(int panelX, int panelY, int lineCount) {
		int fieldX = panelX + ModPanelLayout.PANEL_PADDING;
		int fieldWidth = ModPanelLayout.CONTENT_WIDTH;
		int buttonH = UiLayoutHelper.defaultButtonHeight();
		int rowGap = ModPanelLayout.ROW_GAP;
		int y = panelY + AnvilEditorMetrics.tabContentTop() + ModPanelLayout.SECTION_LABEL_GAP;

		loreLineFields.clear();
		ensureLoreParagraphDraft();
		int paragraphHeight = AnvilEditorMetrics.loreParagraphHeight(lineCount);
		loreParagraphField = MultiLineEditBox.builder()
			.setX(fieldX)
			.setY(y)
			.setPlaceholder(Component.translatable("screen.hologrammenu.anvil.lore_paragraph_hint"))
			.build(
				Minecraft.getInstance().font,
				fieldWidth,
				paragraphHeight,
				Component.translatable("screen.hologrammenu.anvil.lore_paragraph")
			);
		loreParagraphField.setCharacterLimit(720);
		loreParagraphField.setValue(loreParagraphDraft.text(), true);
		loreParagraphField.setValueListener(value -> {
			loreParagraphDraft = loreParagraphDraft.withText(value);
			rebuildParagraphLoreFromDraft();
			refreshLoreSelectionOutlines();
		});
		attach(loreParagraphField);
		y += paragraphHeight + rowGap;

		int half = ModPanelLayout.columnWidth(fieldWidth, 2, rowGap);
		if (loreColorTableOpen) {
			buildLoreColorTable(fieldX, y, fieldWidth, buttonH, rowGap);
			return;
		}

		attach(iconButton(fieldX, y, fieldWidth, buttonH, Component.translatable("screen.hologrammenu.anvil.lore_color_gradient"), new ItemStack(Items.MAGENTA_DYE), press -> {
			loreColorSelection = loreParagraphSelection();
			if (!loreColorSelection.hasSelection()) {
				loreColorSelection = new SelectionRange(0, loreParagraphDraft.text().length());
			}
			colorEditMode = ColorEditMode.SOLID;
			loreColorTableOpen = true;
			relayout();
			releaseButtonFocus(press);
		}));
		y += buttonH + rowGap;

		attach(iconButton(fieldX, y, fieldWidth, buttonH, Component.translatable("screen.hologrammenu.anvil.lore_wrap", loreWrapWidth), new ItemStack(Items.PAPER), press -> {
			loreWrapWidth = loreWrapWidth >= 42 ? 24 : loreWrapWidth + 6;
			rebuildParagraphLoreFromDraft();
			relayout();
			applyLore();
		}));
		y += buttonH + ModPanelLayout.SECTION_GAP;

		StyledText.Effect[] paragraphEffects = {
			StyledText.Effect.BOLD,
			StyledText.Effect.ITALIC,
			StyledText.Effect.UNDERLINE,
			StyledText.Effect.STRIKETHROUGH
		};
		for (int index = 0; index < paragraphEffects.length; index++) {
			StyledText.Effect effect = paragraphEffects[index];
			int column = index % 2;
			int row = index / 2;
			Button button = Button.builder(Component.translatable("screen.hologrammenu.text_style.effect." + effect.name().toLowerCase(java.util.Locale.ROOT)), press -> {
				toggleLoreEffectOnSelection(effect);
				relayout();
				applyLore();
			}).bounds(fieldX + column * (half + rowGap), y + row * (buttonH + rowGap), half, buttonH).build();
			attach(button);
			if (loreParagraphEffects.contains(effect)) {
				markSelectionWidget(button);
			}
		}
		y += ModPanelLayout.stackHeight(2, buttonH, rowGap) + ModPanelLayout.SECTION_GAP;

		attach(iconButton(fieldX, y, fieldWidth, buttonH, Component.translatable("screen.hologrammenu.anvil.lore_preview", generatedLoreLineCount()), new ItemStack(Items.WRITABLE_BOOK), press -> {
			selectedLoreLineIndex = Math.min(selectedLoreLineIndex + 1, Math.max(0, loreLines.size() - 1));
			refreshLoreSelectionOutlines();
		}));

		int actionRowY = y + buttonH + ModPanelLayout.SECTION_GAP;
		int footerY = panelY + AnvilEditorMetrics.loreFooterTop(lineCount);

		attach(iconButton(fieldX, actionRowY, half, buttonH, Component.translatable("screen.hologrammenu.anvil.lore_expand_style"), new ItemStack(Items.NAME_TAG), press -> openLoreLineStyle()));

		attach(iconButton(fieldX + half + rowGap, actionRowY, half, buttonH, Component.translatable("screen.hologrammenu.anvil.lore_regenerate"), new ItemStack(Items.CRAFTING_TABLE), press -> {
			rebuildParagraphLoreFromDraft();
			applyLore();
		}));

		attach(iconButton(fieldX, footerY, half, buttonH, Component.translatable("screen.hologrammenu.anvil.lore_reset"), new ItemStack(Items.BARRIER), press -> resetLore()));

		attach(iconButton(fieldX + half + rowGap, footerY, half, buttonH, Component.translatable("gui.done"), new ItemStack(Items.EMERALD), press -> close()));
	}

	private void buildLoreColorTable(int fieldX, int y, int fieldWidth, int buttonH, int rowGap) {
		int swatch = (fieldWidth - rowGap * (TextFormats.COLORS.size() / 2 - 1)) / (TextFormats.COLORS.size() / 2);
		int index = 0;
		for (TextFormats.ColorOption color : TextFormats.COLORS.values()) {
			int column = index % (TextFormats.COLORS.size() / 2);
			int row = index / (TextFormats.COLORS.size() / 2);
			int x = fieldX + column * (swatch + rowGap);
			int swatchY = y + row * (swatch + rowGap);
			attach(new ClassicColorSwatchButton(x, swatchY, swatch, color.previewColor(), () -> {
				setLorePickerColor(color.previewColor());
				applyLoreColorChoice(true);
				applyLore();
			}));
			index++;
		}
		y += 2 * swatch + rowGap + ModPanelLayout.SECTION_GAP;

		colorPicker = new RgbColorPickerWidget(
			fieldX,
			y,
			fieldWidth,
			colorEditMode == ColorEditMode.GRADIENT ? activeLoreGradientColor() : loreBaseColor,
			this::setLorePickerColor,
			TextStylePanelLayout.PICKER_SCALE
		);
		attach(colorPicker);
		y += colorPicker.getHeight() + rowGap;

		int half = ModPanelLayout.columnWidth(fieldWidth, 2, rowGap);
		solidModeButton = iconButton(fieldX, y, half, buttonH, TextStylePanelLayout.solidModeLabel(), new ItemStack(Items.LIME_DYE), press -> {
			colorEditMode = ColorEditMode.SOLID;
			relayout();
			releaseButtonFocus(press);
		});
		gradientModeButton = iconButton(fieldX + half + rowGap, y, half, buttonH, TextStylePanelLayout.gradientModeLabel(), new ItemStack(Items.AMETHYST_SHARD), press -> {
			colorEditMode = ColorEditMode.GRADIENT;
			gradientTarget = GradientTarget.START;
			relayout();
			releaseButtonFocus(press);
		});
		attach(solidModeButton);
		attach(gradientModeButton);
		markSelectionWidget(colorEditMode == ColorEditMode.SOLID ? solidModeButton : gradientModeButton);
		y += buttonH + rowGap;

		if (colorEditMode == ColorEditMode.GRADIENT) {
			gradientPreview = new GradientPreviewWidget(
				fieldX,
				y,
				fieldWidth,
				TextStylePanelLayout.metrics(1, 0, true).gradientPreviewHeight(),
				() -> loreAccentColor,
				() -> loreBaseColor
			);
			attach(gradientPreview, false);
			y += gradientPreview.getHeight() + rowGap;

			gradientStartButton = iconButton(fieldX, y, half, buttonH, Component.translatable("screen.hologrammenu.text_style.gradient_start"), new ItemStack(Items.GLOW_INK_SAC), press -> {
				gradientTarget = GradientTarget.START;
				if (colorPicker != null) {
					colorPicker.setColor(loreAccentColor);
				}
				relayout();
				releaseButtonFocus(press);
			});
			gradientEndButton = iconButton(fieldX + half + rowGap, y, half, buttonH, Component.translatable("screen.hologrammenu.text_style.gradient_end"), new ItemStack(Items.INK_SAC), press -> {
				gradientTarget = GradientTarget.END;
				if (colorPicker != null) {
					colorPicker.setColor(loreBaseColor);
				}
				relayout();
				releaseButtonFocus(press);
			});
			attach(gradientStartButton);
			attach(gradientEndButton);
			markSelectionWidget(gradientTarget == GradientTarget.START ? gradientStartButton : gradientEndButton);
			y += buttonH + rowGap;
		}

		attach(iconButton(fieldX, y, half, buttonH, Component.translatable("screen.hologrammenu.anvil.effects_apply"), new ItemStack(Items.EMERALD), press -> {
			applyLoreColorChoice(true);
			loreColorTableOpen = false;
			relayout();
			applyLore();
			releaseButtonFocus(press);
		}));
		attach(iconButton(fieldX + half + rowGap, y, half, buttonH, Component.translatable("gui.back"), new ItemStack(Items.ARROW), press -> {
			loreColorTableOpen = false;
			relayout();
			releaseButtonFocus(press);
		}));
	}

	private String loreParagraphText() {
		if (loreParagraphField != null) {
			return loreParagraphField.getValue();
		}
		if (!loreParagraphDraft.text().isBlank()) {
			return loreParagraphDraft.text();
		}
		if (loreLines.isEmpty()) {
			return "";
		}
		List<String> plainLines = new ArrayList<>();
		for (String line : loreLines) {
			if (isGeneratedEffectLoreLine(line)) {
				continue;
			}
			String plain = TextFormats.parse(line).text();
			if (!plain.isBlank()) {
				plainLines.add(plain);
			}
		}
		return String.join("\n", plainLines);
	}

	private void ensureLoreParagraphDraft() {
		if (!loreParagraphDraft.text().isBlank() || loreLines.isEmpty()) {
			return;
		}
		loreParagraphDraft = loreLinesToParagraphDraft();
	}

	private StyledText loreLinesToParagraphDraft() {
		List<StyledSpan> spans = new ArrayList<>();
		for (String line : loreLines) {
			if (isGeneratedEffectLoreLine(line)) {
				continue;
			}
			StyledText styled = TextFormats.parse(line);
			if (styled.text().isBlank()) {
				continue;
			}
			if (!spans.isEmpty()) {
				spans.add(StyledSpan.plain("\n"));
			}
			EnumSet<StyledText.Effect> effects = styled.effects();
			for (StyledSpan span : styled.spans()) {
				spans.add(effects.isEmpty() || !span.effects().isEmpty() ? span : span.withEffects(effects));
			}
		}
		return new StyledText(EnumSet.noneOf(StyledText.Effect.class), spans.isEmpty() ? List.of(StyledSpan.plain("")) : spans);
	}

	private void rebuildParagraphLoreFromDraft() {
		List<String> generatedLines = loreLines.stream()
			.filter(this::isGeneratedEffectLoreLine)
			.toList();
		List<StyledText> wrapped = wrapLoreParagraph(loreParagraphDraft, loreWrapWidth);
		loreLines.clear();
		for (int index = 0; index < wrapped.size() && index < StorageMenuItemLore.MAX_LINES; index++) {
			loreLines.add(styleLoreLine(wrapped.get(index), index, wrapped.size()));
		}
		if (loreLines.isEmpty()) {
			loreLines.add("");
		}
		for (String generatedLine : generatedLines) {
			if (loreLines.size() >= StorageMenuItemLore.MAX_LINES) {
				break;
			}
			loreLines.add(generatedLine);
		}
		selectedLoreLineIndex = Math.min(selectedLoreLineIndex, Math.max(0, loreLines.size() - 1));
	}

	private List<StyledText> wrapLoreParagraph(StyledText paragraph, int wrapWidth) {
		String text = paragraph.text();
		if (text == null || text.isEmpty()) {
			return List.of(StyledText.EMPTY);
		}
		List<StyledText> wrappedLines = new ArrayList<>();
		int lineStart = 0;
		int index = 0;
		while (index <= text.length()) {
			if (index == text.length() || text.charAt(index) == '\n' || text.charAt(index) == '\r') {
				wrappedLines.addAll(wrapLoreHardLine(paragraph, lineStart, index, wrapWidth));
				if (wrappedLines.size() >= StorageMenuItemLore.MAX_LINES || index == text.length()) {
					break;
				}
				if (text.charAt(index) == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
					index++;
				}
				lineStart = index + 1;
			}
			index++;
		}
		if (wrappedLines.isEmpty()) {
			return List.of(StyledText.EMPTY);
		}
		return wrappedLines.size() > StorageMenuItemLore.MAX_LINES
			? wrappedLines.subList(0, StorageMenuItemLore.MAX_LINES)
			: wrappedLines;
	}

	private List<StyledText> wrapLoreHardLine(StyledText paragraph, int start, int end, int wrapWidth) {
		String text = paragraph.text();
		if (start >= end || text.substring(start, end).isBlank()) {
			return List.of(StyledText.EMPTY);
		}
		List<WordRange> words = paragraphWords(text, start, end);
		List<List<WordRange>> lines = new ArrayList<>();
		List<WordRange> current = new ArrayList<>();
		int currentLength = 0;
		for (WordRange word : words) {
			int wordLength = word.end() - word.start();
			if (!current.isEmpty() && currentLength + 1 + wordLength > wrapWidth) {
				lines.add(current);
				if (lines.size() >= StorageMenuItemLore.MAX_LINES) {
					break;
				}
				current = new ArrayList<>();
				currentLength = 0;
			}
			current.add(word);
			currentLength += (currentLength == 0 ? 0 : 1) + wordLength;
		}
		if (!current.isEmpty() && lines.size() < StorageMenuItemLore.MAX_LINES) {
			lines.add(current);
		}
		if (lines.isEmpty()) {
			return List.of(StyledText.EMPTY);
		}
		return lines.stream().map(line -> styledLineFromWords(paragraph, line)).toList();
	}

	private List<WordRange> paragraphWords(String text, int startIndex, int endIndex) {
		List<WordRange> words = new ArrayList<>();
		int index = Math.max(0, startIndex);
		int limit = Math.max(index, Math.min(text.length(), endIndex));
		while (index < limit) {
			while (index < limit && Character.isWhitespace(text.charAt(index))) {
				index++;
			}
			int start = index;
			while (index < limit && !Character.isWhitespace(text.charAt(index))) {
				index++;
			}
			if (index > start) {
				words.add(new WordRange(start, index));
			}
		}
		return words;
	}

	private StyledText styledLineFromWords(StyledText source, List<WordRange> words) {
		List<StyledSpan> spans = new ArrayList<>();
		for (WordRange word : words) {
			if (!spans.isEmpty()) {
				spans.add(StyledSpan.plain(" "));
			}
			spans.addAll(source.slice(word.start(), word.end()).spans());
		}
		return new StyledText(EnumSet.noneOf(StyledText.Effect.class), spans);
	}

	private String styleLoreLine(StyledText line, int index, int totalLines) {
		EnumSet<StyledText.Effect> effects = loreParagraphEffects.isEmpty()
			? EnumSet.noneOf(StyledText.Effect.class)
			: EnumSet.copyOf(loreParagraphEffects);
		if (loreDynamicStyle == LoreDynamicStyle.RARITY && index == 0) {
			effects.add(StyledText.Effect.BOLD);
		}
		String text = line.text();
		if (loreDynamicStyle == LoreDynamicStyle.GRADIENT) {
			return line.spans().size() == 1 && line.color().isEmpty()
				? new StyledText(effects, List.of(StyledSpan.gradient(loreAccentColor, loreBaseColor, text))).serialize()
				: new StyledText(effects, line.spans()).serialize();
		}
		if (loreDynamicStyle == LoreDynamicStyle.ACCENT && index == 0) {
			return line.color().isEmpty()
				? StyledText.of(OptionalInt.of(loreAccentColor), effects, text).serialize()
				: new StyledText(effects, line.spans()).serialize();
		}
		if (loreDynamicStyle == LoreDynamicStyle.RARITY) {
			int color = index == 0
				? loreAccentColor
				: blendRgb(loreBaseColor, loreAccentColor, totalLines <= 1 ? 0.0F : index / (float) (totalLines - 1));
			return line.color().isEmpty()
				? StyledText.of(OptionalInt.of(color), effects, text).serialize()
				: new StyledText(effects, line.spans()).serialize();
		}
		return line.color().isEmpty()
			? StyledText.of(OptionalInt.of(loreBaseColor), effects, text).serialize()
			: new StyledText(effects, line.spans()).serialize();
	}

	private int generatedLoreLineCount() {
		return (int) loreLines.stream()
			.filter(line -> !isGeneratedEffectLoreLine(line))
			.filter(line -> !TextFormats.normalize(line).isBlank())
			.count();
	}

	private boolean isGeneratedEffectLoreLine(String line) {
		if (RpgEffectCatalog.isEffectLore(line)) {
			return true;
		}
		for (RpgCustomEffectStore.CustomEffect effect : RpgCustomEffectStore.effects()) {
			if (RpgCustomEffectStore.isEffectLoreFor(line, effect.name())) {
				return true;
			}
		}
		for (SwordAspect aspect : SWORD_ASPECTS) {
			if (RpgCustomEffectStore.isEffectLoreFor(line, aspect.name())) {
				return true;
			}
		}
		for (ArmorAspect aspect : ARMOR_ASPECTS) {
			if (RpgCustomEffectStore.isEffectLoreFor(line, aspect.name())) {
				return true;
			}
		}
		return false;
	}

	private void applyLoreColorToSelection(int color) {
		SelectionRange selection = loreParagraphSelection();
		if (selection.hasSelection()) {
			loreParagraphDraft = loreParagraphDraft.applySolidColor(selection.start(), selection.end(), color);
		} else {
			loreParagraphDraft = loreParagraphDraft.withColor(color);
		}
		rebuildParagraphLoreFromDraft();
	}

	private void applyLoreColorChoice(boolean useStoredSelection) {
		SelectionRange selection = useStoredSelection ? loreColorSelection : loreParagraphSelection();
		if (!selection.hasSelection()) {
			selection = new SelectionRange(0, loreParagraphDraft.text().length());
		}
		if (!selection.hasSelection()) {
			return;
		}
		if (colorEditMode == ColorEditMode.GRADIENT) {
			loreParagraphDraft = loreParagraphDraft.applyGradient(selection.start(), selection.end(), loreAccentColor, loreBaseColor);
		} else {
			loreParagraphDraft = loreParagraphDraft.applySolidColor(selection.start(), selection.end(), loreBaseColor);
		}
		rebuildParagraphLoreFromDraft();
	}

	private void setLorePickerColor(int rgb) {
		int sanitized = rgb & 0xFFFFFF;
		if (colorEditMode == ColorEditMode.GRADIENT && gradientTarget == GradientTarget.START) {
			loreAccentColor = sanitized;
		} else {
			loreBaseColor = sanitized;
		}
		if (colorPicker != null && colorPicker.getColor() != sanitized) {
			colorPicker.setColor(sanitized);
		}
	}

	private int activeLoreGradientColor() {
		return gradientTarget == GradientTarget.START ? loreAccentColor : loreBaseColor;
	}

	private void applyLoreDynamicStyleToSelection() {
		SelectionRange selection = loreParagraphSelection();
		if (!selection.hasSelection()) {
			rebuildParagraphLoreFromDraft();
			return;
		}
		if (loreDynamicStyle == LoreDynamicStyle.GRADIENT) {
			loreParagraphDraft = loreParagraphDraft.applyGradient(selection.start(), selection.end(), loreAccentColor, loreBaseColor);
		} else if (loreDynamicStyle == LoreDynamicStyle.ACCENT || loreDynamicStyle == LoreDynamicStyle.RARITY) {
			loreParagraphDraft = loreParagraphDraft.applySolidColor(selection.start(), selection.end(), loreAccentColor);
		} else {
			loreParagraphDraft = loreParagraphDraft.withText(loreParagraphDraft.text());
		}
		rebuildParagraphLoreFromDraft();
	}

	private void toggleLoreEffectOnSelection(StyledText.Effect effect) {
		SelectionRange selection = loreParagraphSelection();
		if (selection.hasSelection()) {
			loreParagraphDraft = loreParagraphDraft.toggleEffect(selection.start(), selection.end(), effect);
		} else if (loreParagraphEffects.contains(effect)) {
			loreParagraphEffects.remove(effect);
		} else {
			loreParagraphEffects.add(effect);
		}
		rebuildParagraphLoreFromDraft();
	}

	private SelectionRange loreParagraphSelection() {
		if (loreParagraphField == null) {
			return SelectionRange.NONE;
		}
		MultilineTextField textField = ((MultiLineEditBoxAccessor) loreParagraphField).hologrammenu$getTextField();
		MultilineTextFieldAccessor accessor = (MultilineTextFieldAccessor) textField;
		int start = Math.min(accessor.hologrammenu$getCursor(), accessor.hologrammenu$getSelectCursor());
		int end = Math.max(accessor.hologrammenu$getCursor(), accessor.hologrammenu$getSelectCursor());
		return new SelectionRange(start, end);
	}

	private int nextLoreBaseColor() {
		int[] colors = {0xA0A0A0, 0xFFFFFF, 0x55FF55, 0x55FFFF, 0xFFAA00, 0xFF55FF};
		for (int index = 0; index < colors.length; index++) {
			if (colors[index] == loreBaseColor) {
				return colors[(index + 1) % colors.length];
			}
		}
		return colors[0];
	}

	private static int blendRgb(int start, int end, float amount) {
		amount = Math.max(0.0F, Math.min(1.0F, amount));
		int sr = (start >> 16) & 0xFF;
		int sg = (start >> 8) & 0xFF;
		int sb = start & 0xFF;
		int er = (end >> 16) & 0xFF;
		int eg = (end >> 8) & 0xFF;
		int eb = end & 0xFF;
		int red = Math.round(sr + (er - sr) * amount);
		int green = Math.round(sg + (eg - sg) * amount);
		int blue = Math.round(sb + (eb - sb) * amount);
		return (red << 16) | (green << 8) | blue;
	}

	private void loadLoreFromStack(ItemStack stack) {
		loreLines.clear();
		List<String> existing = readInitialLore(stack);
		if (existing.isEmpty()) {
			loreLines.add("");
		} else {
			loreLines.addAll(existing);
		}
		loreParagraphDraft = loreLinesToParagraphDraft();
		selectedLoreLineIndex = 0;
	}

	private List<String> readInitialLore(ItemStack inputStack) {
		List<String> fromInput = StorageMenuItemLore.readLoreLines(inputStack);
		if (!fromInput.isEmpty()) {
			return fromInput;
		}
		if (parent instanceof AnvilScreen anvilScreen) {
			ItemStack result = anvilScreen.getMenu().getSlot(2).getItem();
			if (!result.isEmpty()) {
				return StorageMenuItemLore.readLoreLines(result);
			}
		}
		return List.of();
	}

	private void selectLoreLine(int lineIndex) {
		selectedLoreLineIndex = lineIndex;
		refreshLoreSelectionOutlines();
	}

	private void refreshLoreSelectionOutlines() {
		clearSelectionOutlines();
		if (selectedLoreLineIndex >= 0 && selectedLoreLineIndex < loreLineFields.size()) {
			markSelectionWidget(loreLineFields.get(selectedLoreLineIndex));
		}
	}

	private void addLoreLine() {
		syncLoreLinesFromFields();
		if (loreLines.size() >= StorageMenuItemLore.MAX_LINES) {
			return;
		}
		loreLines.add("");
		selectedLoreLineIndex = loreLines.size() - 1;
		relayout();
		applyLore();
	}

	private void removeSelectedLoreLine() {
		syncLoreLinesFromFields();
		if (loreLines.size() <= 1) {
			loreLines.set(0, "");
			relayout();
			applyLore();
			return;
		}
		loreLines.remove(selectedLoreLineIndex);
		selectedLoreLineIndex = Math.min(selectedLoreLineIndex, loreLines.size() - 1);
		relayout();
		applyLore();
	}

	private void openLoreLineStyle() {
		syncLoreLinesFromFields();
		selectedLoreLineIndex = focusedLoreLineIndex();
		anvilStyleFocus = AnvilStyleFocus.LORE_LINE;
		draft = TextFormats.parse(currentSelectedLoreSerialized());
		loadPartsFromDraft(draft);
		anvilActiveTab = AnvilEditorTab.STYLE;
		relayout();
	}

	private void applyLore() {
		syncLoreLinesFromFields();
		List<String> payload = loreLines.stream()
			.map(TextFormats::normalize)
			.filter(line -> !line.isBlank())
			.limit(StorageMenuItemLore.MAX_LINES)
			.toList();
		ClientPlayNetworking.send(new ModPackets.AnvilSetLorePayload(payload));
	}

	private void resetLore() {
		loreLines.clear();
		loreLines.add("");
		loreParagraphDraft = StyledText.EMPTY;
		loreParagraphEffects.clear();
		loreBaseColor = DEFAULT_LORE_BASE_COLOR;
		loreAccentColor = DEFAULT_LORE_ACCENT_COLOR;
		loreColorSelection = SelectionRange.NONE;
		loreColorTableOpen = false;
		colorEditMode = ColorEditMode.SOLID;
		gradientTarget = GradientTarget.START;
		loreDynamicStyle = LoreDynamicStyle.CLEAN;
		selectedLoreLineIndex = 0;
		relayout();
		ClientPlayNetworking.send(new ModPackets.AnvilSetLorePayload(List.of()));
	}

	private void buildEffectsWidgets(int panelX, int panelY) {
		if (effectsMode == EditorEffectsMode.ENCHANTS_ONLY) {
			effectsSubTab = EffectsSubTab.ENCHANTS;
			buildVanillaEnchantWidgets(panelX, panelY);
			return;
		}
		int left = panelX + ModPanelLayout.PANEL_PADDING;
		int contentWidth = ModPanelLayout.CONTENT_WIDTH;
		int buttonH = UiLayoutHelper.defaultButtonHeight();
		int rowGap = ModPanelLayout.ROW_GAP;
		int subTabWidth = ModPanelLayout.columnWidth(contentWidth, 4, rowGap);
		int subTabY = panelY + AnvilEditorMetrics.tabContentTop() + ModPanelLayout.SECTION_LABEL_GAP;
		Button rpgTabButton = Button.builder(Component.translatable("screen.hologrammenu.anvil.effects_tab.rpg"), press -> {
				effectsSubTab = EffectsSubTab.RPG;
				relayout();
			})
			.bounds(left, subTabY, subTabWidth, buttonH)
			.build();
		Button enchantTabButton = Button.builder(Component.translatable("screen.hologrammenu.anvil.effects_tab.enchants"), press -> {
				effectsSubTab = EffectsSubTab.ENCHANTS;
				relayout();
			})
			.bounds(left + subTabWidth + rowGap, subTabY, subTabWidth, buttonH)
			.build();
		Button swordTabButton = Button.builder(Component.translatable("screen.hologrammenu.anvil.effects_tab.sword"), press -> {
				effectsSubTab = EffectsSubTab.SWORD;
				relayout();
			})
			.bounds(left + (subTabWidth + rowGap) * 2, subTabY, subTabWidth, buttonH)
			.build();
		Button armorTabButton = Button.builder(Component.translatable("screen.hologrammenu.anvil.effects_tab.armor"), press -> {
				effectsSubTab = EffectsSubTab.ARMOR;
				relayout();
			})
			.bounds(left + (subTabWidth + rowGap) * 3, subTabY, subTabWidth, buttonH)
			.build();
		attach(rpgTabButton);
		attach(enchantTabButton);
		attach(swordTabButton);
		attach(armorTabButton);
		if (effectsSubTab == EffectsSubTab.RPG) {
			markSelectionWidget(rpgTabButton);
			buildRpgEffectWidgets(panelX, panelY);
		} else if (effectsSubTab == EffectsSubTab.ENCHANTS) {
			markSelectionWidget(enchantTabButton);
			buildVanillaEnchantWidgets(panelX, panelY);
		} else if (effectsSubTab == EffectsSubTab.SWORD) {
			markSelectionWidget(swordTabButton);
			buildSwordAspectWidgets(panelX, panelY);
		} else {
			markSelectionWidget(armorTabButton);
			buildArmorAspectWidgets(panelX, panelY);
		}
	}

	private void buildRpgEffectWidgets(int panelX, int panelY) {
		int left = panelX + ModPanelLayout.PANEL_PADDING;
		int contentWidth = ModPanelLayout.CONTENT_WIDTH;
		int buttonH = UiLayoutHelper.defaultButtonHeight();
		int rowGap = ModPanelLayout.ROW_GAP;
		int half = ModPanelLayout.columnWidth(contentWidth, 2, rowGap);
		int y = panelY + AnvilEditorMetrics.effectsGridTop();

		rpgEffectButtons.clear();
		List<RpgEffectOption> effects = rpgEffectOptions();
		for (int index = 0; index < effects.size(); index++) {
			RpgEffectOption effect = effects.get(index);
			int col = index % 2;
			int row = index / 2;
			Button button = iconButton(
				left + col * (half + rowGap),
				y + row * (buttonH + rowGap),
				half,
				buttonH,
				Component.literal(effect.name()),
				new ItemStack(Items.EXPERIENCE_BOTTLE),
				press -> {
					selectedRpgEffectId = effect.id();
					refreshRpgEffectSelectionOutlines();
					releaseButtonFocus(press);
				});
			rpgEffectButtons.put(effect.id(), button);
			attach(button);
		}

		int levelY = panelY + AnvilEditorMetrics.effectsLevelRowTop();
		int third = ModPanelLayout.columnWidth(contentWidth, 3, rowGap);
		attach(Button.builder(Component.literal("-"), press -> {
			selectedRpgEffectLevel = clampSelectedRpgLevel(selectedRpgEffectLevel - 1);
			relayout();
		}).bounds(left, levelY, third, buttonH).build());
		attach(Button.builder(Component.translatable("screen.hologrammenu.rpg_effects.level", selectedRpgEffectLevel), press -> {
			selectedRpgEffectLevel = clampSelectedRpgLevel(selectedRpgEffectLevel + 1);
			relayout();
		}).bounds(left + third + rowGap, levelY, third, buttonH).build());
		attach(Button.builder(Component.literal("+"), press -> {
			selectedRpgEffectLevel = clampSelectedRpgLevel(selectedRpgEffectLevel + 1);
			relayout();
		}).bounds(left + (third + rowGap) * 2, levelY, third, buttonH).build());

		int actionY = panelY + AnvilEditorMetrics.effectsActionRowTop();
		attach(iconButton(left, actionY, half, buttonH, Component.translatable("screen.hologrammenu.anvil.effects_apply"), new ItemStack(Items.EMERALD), press -> applySelectedRpgEffect()));
		attach(iconButton(left + half + rowGap, actionY, half, buttonH, Component.translatable("screen.hologrammenu.anvil.effects_remove"), new ItemStack(Items.BARRIER), press -> removeSelectedRpgEffect()));

		int footerY = panelY + AnvilEditorMetrics.effectsFooterTop();
		attach(iconButton(left, footerY, contentWidth, buttonH, Component.translatable("gui.done"), new ItemStack(Items.EMERALD), press -> close()));
		refreshRpgEffectSelectionOutlines();
	}

	private void buildVanillaEnchantWidgets(int panelX, int panelY) {
		int left = panelX + ModPanelLayout.PANEL_PADDING;
		int contentWidth = ModPanelLayout.CONTENT_WIDTH;
		int buttonH = UiLayoutHelper.defaultButtonHeight();
		int rowGap = ModPanelLayout.ROW_GAP;
		int half = ModPanelLayout.columnWidth(contentWidth, 2, rowGap);
		int y = panelY + AnvilEditorMetrics.effectsGridTop();
		vanillaEnchantButtons.clear();
		int maxPage = maxVanillaEnchantPage();
		vanillaEnchantPage = Math.max(0, Math.min(maxPage, vanillaEnchantPage));
		int start = vanillaEnchantPage * ENCHANTS_PER_PAGE;
		int end = Math.min(VANILLA_ENCHANTS.size(), start + ENCHANTS_PER_PAGE);
		for (int index = start; index < end; index++) {
			VanillaEnchantOption option = VANILLA_ENCHANTS.get(index);
			int local = index - start;
			int col = local % 2;
			int row = local / 2;
			Button button = iconButton(left + col * (half + rowGap), y + row * (buttonH + rowGap), half, buttonH, Component.literal(option.label()), new ItemStack(Items.ENCHANTED_BOOK), press -> {
				selectedVanillaEnchantId = option.id();
				refreshVanillaEnchantSelectionOutlines();
				releaseButtonFocus(press);
			});
			vanillaEnchantButtons.put(option.id(), button);
			attach(button);
		}

		int pageY = panelY + AnvilEditorMetrics.effectsPageRowTop();
		int third = ModPanelLayout.columnWidth(contentWidth, 3, rowGap);
		attach(Button.builder(Component.literal("<"), press -> {
			vanillaEnchantPage = Math.max(0, vanillaEnchantPage - 1);
			relayout();
		}).bounds(left, pageY, third, buttonH).build());
		attach(Button.builder(Component.translatable("screen.hologrammenu.anvil.enchant_page", vanillaEnchantPage + 1, maxPage + 1), press -> {
			vanillaEnchantPage = vanillaEnchantPage >= maxPage ? 0 : vanillaEnchantPage + 1;
			relayout();
		}).bounds(left + third + rowGap, pageY, third, buttonH).build());
		attach(Button.builder(Component.literal(">"), press -> {
			vanillaEnchantPage = Math.min(maxPage, vanillaEnchantPage + 1);
			relayout();
		}).bounds(left + (third + rowGap) * 2, pageY, third, buttonH).build());

		int levelY = panelY + AnvilEditorMetrics.effectsEnchantLevelRowTop();
		attach(Button.builder(Component.literal("-"), press -> adjustVanillaEnchantLevel(-1))
			.bounds(left, levelY, third, buttonH).build());
		vanillaEnchantLevelField = new EditBox(
			Minecraft.getInstance().font,
			left + third + rowGap,
			levelY,
			third,
			buttonH,
			Component.translatable("screen.hologrammenu.rpg_effects.level", selectedVanillaEnchantLevel)
		);
		vanillaEnchantLevelField.setMaxLength(3);
		vanillaEnchantLevelField.setValue(Integer.toString(selectedVanillaEnchantLevel));
		vanillaEnchantLevelField.setResponder(value -> selectedVanillaEnchantLevel = parseEnchantLevel(value));
		attach(vanillaEnchantLevelField);
		attach(Button.builder(Component.literal("+"), press -> adjustVanillaEnchantLevel(1))
			.bounds(left + (third + rowGap) * 2, levelY, third, buttonH).build());

		int actionY = panelY + AnvilEditorMetrics.effectsEnchantActionRowTop();
		attach(iconButton(left, actionY, half, buttonH, Component.translatable("screen.hologrammenu.anvil.effects_apply"), new ItemStack(Items.EMERALD), press -> applySelectedVanillaEnchant()));
		attach(iconButton(left + half + rowGap, actionY, half, buttonH, Component.translatable("screen.hologrammenu.anvil.effects_remove"), new ItemStack(Items.BARRIER), press -> removeSelectedVanillaEnchant()));

		int footerY = panelY + AnvilEditorMetrics.effectsEnchantFooterTop();
		attach(iconButton(left, footerY, contentWidth, buttonH, Component.translatable("gui.done"), new ItemStack(Items.EMERALD), press -> close()));
		refreshVanillaEnchantSelectionOutlines();
	}

	private void buildSwordAspectWidgets(int panelX, int panelY) {
		int left = panelX + ModPanelLayout.PANEL_PADDING;
		int contentWidth = ModPanelLayout.CONTENT_WIDTH;
		int buttonH = UiLayoutHelper.defaultButtonHeight();
		int rowGap = ModPanelLayout.ROW_GAP;
		int half = ModPanelLayout.columnWidth(contentWidth, 2, rowGap);
		int y = panelY + AnvilEditorMetrics.effectsGridTop();
		for (int index = 0; index < SWORD_ASPECTS.length; index++) {
			SwordAspect aspect = SWORD_ASPECTS[index];
			int col = index % 2;
			int row = index / 2;
			attach(iconButton(
				left + col * (half + rowGap),
				y + row * (buttonH + rowGap),
				half,
				buttonH,
				Component.literal(aspect.name() + ": " + aspectValueLabel(aspect)),
				new ItemStack(Items.IRON_SWORD),
				press -> {
					adjustSwordAspect(aspect, aspect.step());
					relayout();
				}));
		}

		int levelY = panelY + AnvilEditorMetrics.effectsLevelRowTop();
		int third = ModPanelLayout.columnWidth(contentWidth, 3, rowGap);
		attach(iconButton(left, levelY, third, buttonH, Component.translatable("screen.hologrammenu.anvil.sword_aspects_reset"), new ItemStack(Items.BARRIER), press -> {
			resetSwordAspects();
			relayout();
		}));
		attach(Button.builder(Component.translatable("screen.hologrammenu.anvil.sword_aspects_minus"), press -> {
			adjustAllSwordAspects(-5);
			relayout();
		}).bounds(left + third + rowGap, levelY, third, buttonH).build());
		attach(Button.builder(Component.translatable("screen.hologrammenu.anvil.sword_aspects_plus"), press -> {
			adjustAllSwordAspects(5);
			relayout();
		}).bounds(left + (third + rowGap) * 2, levelY, third, buttonH).build());

		int actionY = panelY + AnvilEditorMetrics.effectsActionRowTop();
		attach(iconButton(left, actionY, half, buttonH, Component.translatable("screen.hologrammenu.anvil.effects_apply"), new ItemStack(Items.EMERALD), press -> applySwordAspects()));
		attach(iconButton(left + half + rowGap, actionY, half, buttonH, Component.translatable("screen.hologrammenu.anvil.effects_remove"), new ItemStack(Items.BARRIER), press -> removeSwordAspects()));

		int footerY = panelY + AnvilEditorMetrics.effectsFooterTop();
		attach(iconButton(left, footerY, contentWidth, buttonH, Component.translatable("gui.done"), new ItemStack(Items.EMERALD), press -> close()));
	}

	private void buildArmorAspectWidgets(int panelX, int panelY) {
		int left = panelX + ModPanelLayout.PANEL_PADDING;
		int contentWidth = ModPanelLayout.CONTENT_WIDTH;
		int buttonH = UiLayoutHelper.defaultButtonHeight();
		int rowGap = ModPanelLayout.ROW_GAP;
		int third = ModPanelLayout.columnWidth(contentWidth, 3, rowGap);
		int y = panelY + AnvilEditorMetrics.effectsGridTop();
		for (int index = 0; index < ARMOR_ASPECTS.length; index++) {
			ArmorAspect aspect = ARMOR_ASPECTS[index];
			attach(iconButton(
				left + index * (third + rowGap),
				y,
				third,
				buttonH,
				Component.literal(aspect.name() + ": " + armorAspectValueLabel(aspect)),
				new ItemStack(Items.IRON_CHESTPLATE),
				press -> {
					adjustArmorAspect(aspect, aspect.step());
					relayout();
				}));
		}

		int levelY = panelY + AnvilEditorMetrics.effectsLevelRowTop();
		attach(iconButton(left, levelY, third, buttonH, Component.translatable("screen.hologrammenu.anvil.armor_aspects_reset"), new ItemStack(Items.BARRIER), press -> {
			resetArmorAspects();
			relayout();
		}));
		attach(Button.builder(Component.translatable("screen.hologrammenu.anvil.armor_aspects_minus"), press -> {
			adjustAllArmorAspects(-5);
			relayout();
		}).bounds(left + third + rowGap, levelY, third, buttonH).build());
		attach(Button.builder(Component.translatable("screen.hologrammenu.anvil.armor_aspects_plus"), press -> {
			adjustAllArmorAspects(5);
			relayout();
		}).bounds(left + (third + rowGap) * 2, levelY, third, buttonH).build());

		int half = ModPanelLayout.columnWidth(contentWidth, 2, rowGap);
		int actionY = panelY + AnvilEditorMetrics.effectsActionRowTop();
		attach(iconButton(left, actionY, half, buttonH, Component.translatable("screen.hologrammenu.anvil.effects_apply"), new ItemStack(Items.EMERALD), press -> applyArmorAspects()));
		attach(iconButton(left + half + rowGap, actionY, half, buttonH, Component.translatable("screen.hologrammenu.anvil.effects_remove"), new ItemStack(Items.BARRIER), press -> removeArmorAspects()));

		int footerY = panelY + AnvilEditorMetrics.effectsFooterTop();
		attach(iconButton(left, footerY, contentWidth, buttonH, Component.translatable("gui.done"), new ItemStack(Items.EMERALD), press -> close()));
	}

	private void applySelectedRpgEffect() {
		RpgCustomEffectStore.CustomEffect custom = selectedCustomRpgEffect();
		if (custom != null) {
			ensureLoreLoadedForEffects();
			String loreLine = custom.loreLine(selectedRpgEffectLevel);
			ClientPlayNetworking.send(new ModPackets.AnvilApplyCustomRpgEffectPayload(custom.name(), loreLine, false));
			replaceLocalCustomRpgEffectLine(custom.name(), loreLine);
			return;
		}
		RpgEffectCatalog.Entry selected = RpgEffectCatalog.byId(selectedRpgEffectId);
		ensureLoreLoadedForEffects();
		ClientPlayNetworking.send(new ModPackets.AnvilApplyRpgEffectPayload(selected.id(), selectedRpgEffectLevel, false));
		replaceLocalRpgEffectLine(selected, selected.loreLine(selectedRpgEffectLevel));
	}

	private void applySwordAspects() {
		ensureLoreLoadedForEffects();
		removeLocalSwordAspectLines();
		for (SwordAspect aspect : SWORD_ASPECTS) {
			int value = swordAspectValue(aspect);
			if (value <= aspect.min()) {
				continue;
			}
			String line = swordAspectLoreLine(aspect, value);
			ClientPlayNetworking.send(new ModPackets.AnvilApplyCustomRpgEffectPayload(aspect.name(), line, false));
			replaceLocalCustomRpgEffectLine(aspect.name(), line);
		}
	}

	private void removeSwordAspects() {
		ensureLoreLoadedForEffects();
		for (SwordAspect aspect : SWORD_ASPECTS) {
			ClientPlayNetworking.send(new ModPackets.AnvilApplyCustomRpgEffectPayload(aspect.name(), swordAspectLoreLine(aspect, aspect.min()), true));
		}
		removeLocalSwordAspectLines();
		if (loreLines.isEmpty()) {
			loreLines.add("");
		}
	}

	private void removeLocalSwordAspectLines() {
		for (SwordAspect aspect : SWORD_ASPECTS) {
			loreLines.removeIf(line -> RpgCustomEffectStore.isEffectLoreFor(line, aspect.name()));
		}
	}

	private String swordAspectLoreLine(SwordAspect aspect, int value) {
		return translateAmpersandCodes(aspect.colorCode() + aspect.name() + " " + aspect.valueText(value));
	}

	private String aspectValueLabel(SwordAspect aspect) {
		return aspect.valueText(swordAspectValue(aspect));
	}

	private int swordAspectValue(SwordAspect aspect) {
		return switch (aspect.name()) {
			case "Critical Chance" -> swordCritChance;
			case "Cooldown" -> swordCooldownReduction;
			case "Parasitic Regen" -> swordLifeSteal;
			case "Bleed Chance" -> swordBleedChance;
			case "Armor Pierce" -> swordArmorPierce;
			case "Execute Damage" -> swordExecuteDamage;
			default -> 0;
		};
	}

	private void adjustSwordAspect(SwordAspect aspect, int delta) {
		int value = swordAspectValue(aspect) + delta;
		if (value > aspect.max()) {
			value = aspect.min();
		}
		value = Math.max(aspect.min(), Math.min(aspect.max(), value));
		setSwordAspectValue(aspect, value);
	}

	private void setSwordAspectValue(SwordAspect aspect, int value) {
		switch (aspect.name()) {
			case "Critical Chance" -> swordCritChance = value;
			case "Cooldown" -> swordCooldownReduction = value;
			case "Parasitic Regen" -> swordLifeSteal = value;
			case "Bleed Chance" -> swordBleedChance = value;
			case "Armor Pierce" -> swordArmorPierce = value;
			case "Execute Damage" -> swordExecuteDamage = value;
			default -> {
			}
		}
	}

	private void resetSwordAspects() {
		for (SwordAspect aspect : SWORD_ASPECTS) {
			setSwordAspectValue(aspect, aspect.min());
		}
	}

	private void adjustAllSwordAspects(int delta) {
		for (SwordAspect aspect : SWORD_ASPECTS) {
			setSwordAspectValue(aspect, Math.max(aspect.min(), Math.min(aspect.max(), swordAspectValue(aspect) + delta)));
		}
	}

	private void applyArmorAspects() {
		ensureLoreLoadedForEffects();
		removeLocalArmorAspectLines();
		for (ArmorAspect aspect : ARMOR_ASPECTS) {
			int value = armorAspectValue(aspect);
			if (value <= aspect.min()) {
				continue;
			}
			String line = armorAspectLoreLine(aspect, value);
			ClientPlayNetworking.send(new ModPackets.AnvilApplyCustomRpgEffectPayload(aspect.name(), line, false));
			replaceLocalCustomRpgEffectLine(aspect.name(), line);
		}
	}

	private void removeArmorAspects() {
		ensureLoreLoadedForEffects();
		for (ArmorAspect aspect : ARMOR_ASPECTS) {
			ClientPlayNetworking.send(new ModPackets.AnvilApplyCustomRpgEffectPayload(aspect.name(), armorAspectLoreLine(aspect, aspect.min()), true));
		}
		removeLocalArmorAspectLines();
		if (loreLines.isEmpty()) {
			loreLines.add("");
		}
	}

	private void removeLocalArmorAspectLines() {
		for (ArmorAspect aspect : ARMOR_ASPECTS) {
			loreLines.removeIf(line -> RpgCustomEffectStore.isEffectLoreFor(line, aspect.name()));
		}
	}

	private String armorAspectLoreLine(ArmorAspect aspect, int value) {
		return translateAmpersandCodes(aspect.colorCode() + aspect.name() + " " + aspect.valueText(value));
	}

	private static String translateAmpersandCodes(String value) {
		StringBuilder translated = new StringBuilder(value.length());
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (character == '&' && index + 1 < value.length() && isLegacyCode(value.charAt(index + 1))) {
				translated.append('§').append(Character.toLowerCase(value.charAt(index + 1)));
				index++;
			} else {
				translated.append(character);
			}
		}
		return translated.toString();
	}

	private static boolean isLegacyCode(char value) {
		return (value >= '0' && value <= '9')
			|| (value >= 'a' && value <= 'f')
			|| (value >= 'A' && value <= 'F')
			|| "kKlLmMnNoOrR".indexOf(value) >= 0;
	}

	private String armorAspectValueLabel(ArmorAspect aspect) {
		return aspect.valueText(armorAspectValue(aspect));
	}

	private int armorAspectValue(ArmorAspect aspect) {
		return switch (aspect.name()) {
			case "Max Health" -> armorHealthBonus;
			case "Armor Defense" -> armorDefenseBonus;
			case "Health Regen" -> armorRegenBonus;
			default -> 0;
		};
	}

	private void adjustArmorAspect(ArmorAspect aspect, int delta) {
		int value = armorAspectValue(aspect) + delta;
		if (value > aspect.max()) {
			value = aspect.min();
		}
		value = Math.max(aspect.min(), Math.min(aspect.max(), value));
		setArmorAspectValue(aspect, value);
	}

	private void setArmorAspectValue(ArmorAspect aspect, int value) {
		switch (aspect.name()) {
			case "Max Health" -> armorHealthBonus = value;
			case "Armor Defense" -> armorDefenseBonus = value;
			case "Health Regen" -> armorRegenBonus = value;
			default -> {
			}
		}
	}

	private void resetArmorAspects() {
		for (ArmorAspect aspect : ARMOR_ASPECTS) {
			setArmorAspectValue(aspect, aspect.min());
		}
	}

	private void adjustAllArmorAspects(int delta) {
		for (ArmorAspect aspect : ARMOR_ASPECTS) {
			setArmorAspectValue(aspect, Math.max(aspect.min(), Math.min(aspect.max(), armorAspectValue(aspect) + delta)));
		}
	}

	private void removeSelectedRpgEffect() {
		RpgCustomEffectStore.CustomEffect custom = selectedCustomRpgEffect();
		if (custom != null) {
			ensureLoreLoadedForEffects();
			ClientPlayNetworking.send(new ModPackets.AnvilApplyCustomRpgEffectPayload(custom.name(), custom.loreLine(selectedRpgEffectLevel), true));
			loreLines.removeIf(line -> RpgCustomEffectStore.isEffectLoreFor(line, custom.name()));
			if (loreLines.isEmpty()) {
				loreLines.add("");
			}
			return;
		}
		RpgEffectCatalog.Entry selected = RpgEffectCatalog.byId(selectedRpgEffectId);
		ensureLoreLoadedForEffects();
		ClientPlayNetworking.send(new ModPackets.AnvilApplyRpgEffectPayload(selected.id(), selectedRpgEffectLevel, true));
		loreLines.removeIf(line -> RpgEffectCatalog.isEffectLoreFor(line, selected));
		if (loreLines.isEmpty()) {
			loreLines.add("");
		}
	}

	private void replaceLocalRpgEffectLine(RpgEffectCatalog.Entry selected, String line) {
		List<String> updated = new ArrayList<>();
		for (String existing : loreLines) {
			if (!RpgEffectCatalog.isEffectLoreFor(existing, selected)) {
				updated.add(existing);
			}
		}
		while (updated.size() >= StorageMenuItemLore.MAX_LINES) {
			updated.remove(updated.size() - 1);
		}
		updated.add(line);
		loreLines.clear();
		loreLines.addAll(trimLoreLines(updated));
	}

	private void replaceLocalCustomRpgEffectLine(String effectName, String line) {
		List<String> updated = new ArrayList<>();
		for (String existing : loreLines) {
			if (!RpgCustomEffectStore.isEffectLoreFor(existing, effectName)) {
				updated.add(existing);
			}
		}
		while (updated.size() >= StorageMenuItemLore.MAX_LINES) {
			updated.remove(updated.size() - 1);
		}
		updated.add(line);
		loreLines.clear();
		loreLines.addAll(trimLoreLines(updated));
	}

	private List<RpgEffectOption> rpgEffectOptions() {
		List<RpgEffectOption> options = new ArrayList<>();
		for (RpgCustomEffectStore.CustomEffect effect : RpgCustomEffectStore.effects()) {
			options.add(new RpgEffectOption(effect.id(), effect.name()));
		}
		for (RpgEffectCatalog.Entry effect : RpgEffectCatalog.effects()) {
			options.add(new RpgEffectOption(effect.id(), effect.name()));
		}
		if (options.size() > 8) {
			return options.subList(0, 8);
		}
		return options;
	}

	private RpgCustomEffectStore.CustomEffect selectedCustomRpgEffect() {
		return RpgCustomEffectStore.find(selectedRpgEffectId).orElse(null);
	}

	private int clampSelectedRpgLevel(int level) {
		RpgCustomEffectStore.CustomEffect custom = selectedCustomRpgEffect();
		if (custom != null) {
			return Math.min(custom.maxLevel(), RpgCustomEffectStore.clampLevel(level));
		}
		return RpgEffectCatalog.clampLevel(level);
	}

	private void applySelectedVanillaEnchant() {
		selectedVanillaEnchantLevel = vanillaEnchantLevel();
		ClientPlayNetworking.send(new ModPackets.AnvilApplyEnchantPayload(selectedVanillaEnchantId, selectedVanillaEnchantLevel, false));
	}

	private void removeSelectedVanillaEnchant() {
		ClientPlayNetworking.send(new ModPackets.AnvilApplyEnchantPayload(selectedVanillaEnchantId, selectedVanillaEnchantLevel, true));
	}

	private void adjustVanillaEnchantLevel(int delta) {
		selectedVanillaEnchantLevel = Math.max(1, Math.min(255, vanillaEnchantLevel() + delta));
		if (vanillaEnchantLevelField != null) {
			vanillaEnchantLevelField.setValue(Integer.toString(selectedVanillaEnchantLevel));
		}
	}

	private int vanillaEnchantLevel() {
		if (vanillaEnchantLevelField != null) {
			return parseEnchantLevel(vanillaEnchantLevelField.getValue());
		}
		return Math.max(1, Math.min(255, selectedVanillaEnchantLevel));
	}

	private static int parseEnchantLevel(String value) {
		if (value == null || value.isBlank()) {
			return 1;
		}
		try {
			return Math.max(1, Math.min(255, Integer.parseInt(value.trim())));
		} catch (NumberFormatException ignored) {
			return 1;
		}
	}

	private int maxVanillaEnchantPage() {
		return Math.max(0, (VANILLA_ENCHANTS.size() - 1) / ENCHANTS_PER_PAGE);
	}

	private void ensureLoreLoadedForEffects() {
		if (loreLines.isEmpty() && anvilStackSupplier != null) {
			ItemStack stack = anvilStackSupplier.get();
			if (!stack.isEmpty()) {
				loadLoreFromStack(stack);
			}
		}
	}

	private List<String> trimLoreLines(List<String> lines) {
		return lines.stream()
			.filter(line -> !TextFormats.normalize(line).isBlank())
			.limit(StorageMenuItemLore.MAX_LINES)
			.toList();
	}

	private void refreshRpgEffectSelectionOutlines() {
		clearSelectionOutlines();
		Button selected = rpgEffectButtons.get(selectedRpgEffectId);
		if (selected != null) {
			markSelectionWidget(selected);
		}
	}

	private void refreshVanillaEnchantSelectionOutlines() {
		clearSelectionOutlines();
		Button selected = vanillaEnchantButtons.get(selectedVanillaEnchantId);
		if (selected != null) {
			markSelectionWidget(selected);
		}
	}

	private void syncLoreLinesFromFields() {
		if (loreParagraphField != null) {
			loreParagraphDraft = loreParagraphDraft.withText(loreParagraphField.getValue());
			rebuildParagraphLoreFromDraft();
			return;
		}
		for (int index = 0; index < loreLineFields.size(); index++) {
			EditBox field = loreLineFields.get(index);
			loreLines.set(index, TextFormats.parse(loreLines.get(index)).withText(field.getValue()).serialize());
		}
	}

	private void updateSelectedLoreLine(String serialized) {
		while (loreLines.size() <= selectedLoreLineIndex) {
			loreLines.add("");
		}
		loreLines.set(selectedLoreLineIndex, serialized);
		if (selectedLoreLineIndex < loreLineFields.size()) {
			EditBox field = loreLineFields.get(selectedLoreLineIndex);
			String plain = TextFormats.parse(serialized).text();
			if (!plain.equals(field.getValue())) {
				field.setValue(plain);
			}
		}
	}

	private String currentSelectedLoreSerialized() {
		if (selectedLoreLineIndex >= 0 && selectedLoreLineIndex < loreLines.size()) {
			return loreLines.get(selectedLoreLineIndex);
		}
		return "";
	}

	private int focusedLoreLineIndex() {
		for (int index = 0; index < loreLineFields.size(); index++) {
			if (loreLineFields.get(index).isFocused()) {
				return index;
			}
		}
		return Math.min(selectedLoreLineIndex, Math.max(0, loreLines.size() - 1));
	}

	private void showAnvilMessage(Component message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			minecraft.player.sendSystemMessage(message);
		}
	}

	private void buildPartFields(int panelX, int panelY, int partCount, int contentTopOffset, TextStylePanelLayout.Metrics layout) {
		partFields.clear();
		boolean removable = partCount > 1;
		int buttonHeight = layout.buttonHeight();
		int fieldWidth = TextStylePanelLayout.partFieldWidth(removable, buttonHeight);
		int left = panelX + TextStylePanelLayout.CONTENT_LEFT;
		int removeX = left + fieldWidth + layout.buttonRowGap();

		for (int index = 0; index < partCount; index++) {
			int fieldY = panelY + layout.partTop() + index * layout.partRowHeight();
			TextPart part = parts.get(index);
			int partIndex = index;

			EditBox field = new PartSelectEditBox(
				Minecraft.getInstance().font,
				left,
				fieldY,
				fieldWidth,
				LabeledFieldLayout.FIELD_HEIGHT,
				Component.translatable("screen.hologrammenu.text_style.part", partIndex + 1),
				() -> selectPart(partIndex)
			);
			field.setMaxLength(64);
			field.setValue(part.text);
			field.setHint(Component.translatable("screen.hologrammenu.text_style.part", partIndex + 1));
			field.setResponder(value -> {
				selectedPartIndex = partIndex;
				parts.get(partIndex).text = value;
				rebuildDraftFromParts();
			});
			attach(field);
			partFields.add(field);

			if (removable) {
				attach(Button.builder(Component.literal("x"), press -> removePart(partIndex))
					.bounds(removeX, fieldY, buttonHeight, buttonHeight)
					.build());
			}
		}

		int addY = panelY + layout.partTop() + partCount * layout.partRowHeight();
		if (partCount < MAX_PARTS) {
			attach(Button.builder(Component.translatable("screen.hologrammenu.text_style.add_part"), press -> addPart())
				.bounds(left, addY, TextStylePanelLayout.CONTENT_WIDTH, buttonHeight)
				.build());
		}
	}

	private static String effectIdFor(StyledText.Effect effect) {
		for (TextFormats.EffectOption option : TextFormats.EFFECTS.values()) {
			if (option.effect() == effect) {
				return option.id();
			}
		}
		throw new IllegalArgumentException("Unknown effect: " + effect);
	}

	private void addPart() {
		if (parts.size() >= MAX_PARTS) {
			return;
		}
		syncPartsFromFields();
		parts.add(TextPart.empty());
		selectedPartIndex = parts.size() - 1;
		rebuildDraftFromParts();
		applyDraftToTarget();
		relayout();
	}

	private void removePart(int index) {
		if (parts.size() <= 1) {
			return;
		}
		syncPartsFromFields();
		parts.remove(index);
		selectedPartIndex = Math.min(selectedPartIndex, parts.size() - 1);
		rebuildDraftFromParts();
		applyDraftToTarget();
		relayout();
	}

	private void resetParts() {
		String combined = stylePlainTextSupplier().get();
		parts.clear();
		parts.add(TextPart.empty());
		parts.get(0).text = combined;
		selectedPartIndex = 0;
		rebuildDraftFromParts();
		applyDraftToTarget();
		relayout();
	}

	private TextPart selectedPart() {
		return parts.get(Math.min(selectedPartIndex, parts.size() - 1));
	}

	private void syncPartsFromFields() {
		for (int index = 0; index < partFields.size() && index < parts.size(); index++) {
			parts.get(index).text = partFields.get(index).getValue();
		}
	}

	private void syncPartField(int index) {
		if (index >= 0 && index < partFields.size() && index < parts.size()) {
			partFields.get(index).setValue(parts.get(index).text);
		}
	}

	private void rebuildDraftFromParts() {
		draft = buildFromParts();
	}

	private void applyDraftToTarget() {
		draft = currentDraft();
		if (isAnvilMode() && anvilStyleFocus == AnvilStyleFocus.LORE_LINE) {
			updateSelectedLoreLine(draft.serialize());
			return;
		}
		target.applyStyledText(draft.serialize());
	}

	private StyledText buildFromParts() {
		List<StyledSpan> spans = new ArrayList<>();
		for (TextPart part : parts) {
			if (!part.text.isEmpty()) {
				spans.add(part.toSpan());
			}
		}
		if (spans.isEmpty()) {
			spans.add(StyledSpan.plain(""));
		}
		return new StyledText(EnumSet.noneOf(StyledText.Effect.class), spans);
	}

	private StyledText currentDraft() {
		syncPartsFromFields();
		return buildFromParts();
	}

	private int[] selectedPartSelection() {
		if (selectedPartIndex < 0 || selectedPartIndex >= partFields.size()) {
			int end = selectedPart().text.length();
			return new int[] {end, end};
		}
		EditBox field = partFields.get(selectedPartIndex);
		int cursor = field.getCursorPosition();
		int highlight = ((EditBoxAccessor) field).hologrammenu$getHighlightPos();
		return new int[] {Math.min(cursor, highlight), Math.max(cursor, highlight)};
	}

	private void applySolidColorToSelectedPart(int rgb) {
		selectedPartIndex = Math.min(selectedPartIndex, parts.size() - 1);
		int[] range = selectedPartSelection();
		TextPart part = selectedPart();
		if (range[0] == range[1]) {
			part.setSolidColor(rgb);
		} else {
			StyledText mini = part.toStyledText().applySolidColor(range[0], range[1], rgb);
			part.applyFrom(mini);
		}
		syncPartField(selectedPartIndex);
		rebuildDraftFromParts();
		applyDraftToTarget();
	}

	private void applyGradientToSelectedPart(int startRgb, int endRgb) {
		selectedPartIndex = Math.min(selectedPartIndex, parts.size() - 1);
		int[] range = selectedPartSelection();
		TextPart part = selectedPart();
		if (range[0] == range[1]) {
			part.setGradient(startRgb, endRgb);
		} else {
			StyledText mini = part.toStyledText().applyGradient(range[0], range[1], startRgb, endRgb);
			part.applyFrom(mini);
		}
		syncPartField(selectedPartIndex);
		rebuildDraftFromParts();
		applyDraftToTarget();
	}

	private void toggleEffectOnSelectedPart(StyledText.Effect effect) {
		syncPartsFromFields();
		selectedPart().toggleEffect(effect);
		rebuildDraftFromParts();
		applyDraftToTarget();
		refreshSelectionOutlines();
	}

	private void selectPart(int partIndex) {
		selectedPartIndex = partIndex;
		ColorEditMode previousMode = colorEditMode;
		syncColorModeFromSelectedPart();
		syncGradientColorsFromSelectedPart();
		if (colorPicker != null) {
			colorPicker.setColor(activePickerColor());
		}
		if (previousMode != colorEditMode) {
			relayout();
		} else {
			updateColorModeButtons();
			updateGradientTargetButtons();
			refreshSelectionOutlines();
		}
	}

	private void setGradientTarget(GradientTarget target) {
		gradientTarget = target;
		if (colorPicker != null) {
			colorPicker.setColor(activeGradientColor());
		}
		updateGradientTargetButtons();
	}

	private void onPickerColorChanged(int rgb) {
		if (colorEditMode == ColorEditMode.GRADIENT) {
			setActiveGradientColor(rgb);
		} else {
			applySolidColorToSelectedPart(rgb);
		}
	}

	private void setActiveGradientColor(int rgb) {
		int sanitized = rgb & 0xFFFFFF;
		if (gradientTarget == GradientTarget.START) {
			gradientStartColor = sanitized;
		} else {
			gradientEndColor = sanitized;
		}
		if (colorEditMode == ColorEditMode.GRADIENT) {
			applyGradientToSelectedPart(gradientStartColor, gradientEndColor);
		}
	}

	private int activeGradientColor() {
		return gradientTarget == GradientTarget.START ? gradientStartColor : gradientEndColor;
	}

	private void syncGradientColorsFromSelectedPart() {
		TextPart part = selectedPart();
		if (part.gradientEnd.isPresent() && part.color.isPresent()) {
			gradientStartColor = part.color.getAsInt();
			gradientEndColor = part.gradientEnd.getAsInt();
			return;
		}
		if (part.color.isPresent()) {
			gradientStartColor = part.color.getAsInt();
		} else {
			gradientStartColor = 0xFFFFFF;
		}
		gradientEndColor = complementRgb(gradientStartColor);
	}

	private void updateGradientTargetButtons() {
		if (gradientStartButton != null) {
			gradientStartButton.active = true;
		}
		if (gradientEndButton != null) {
			gradientEndButton.active = true;
		}
		refreshSelectionOutlines();
	}

	private void refreshSelectionOutlines() {
		clearSelectionOutlines();
		markSelectionWidget(colorEditMode == ColorEditMode.SOLID ? solidModeButton : gradientModeButton);
		if (colorEditMode == ColorEditMode.GRADIENT) {
			markSelectionWidget(gradientTarget == GradientTarget.START ? gradientStartButton : gradientEndButton);
		}

		TextPart part = selectedPart();
		for (Map.Entry<StyledText.Effect, Button> entry : effectButtons.entrySet()) {
			if (part.effects.contains(entry.getKey())) {
				markSelectionWidget(entry.getValue());
			}
		}

		if (selectedPartIndex >= 0 && selectedPartIndex < partFields.size()) {
			markSelectionWidget(partFields.get(selectedPartIndex));
		}
	}

	private void markSelectionWidget(AbstractWidget widget) {
		if (widget == null) {
			return;
		}
		ModUiSelectionState.markSelected(widget);
		selectionWidgets.add(widget);
	}

	private void clearSelectionOutlines() {
		for (AbstractWidget widget : selectionWidgets) {
			ModUiSelectionState.unmarkSelected(widget);
		}
		selectionWidgets.clear();
	}

	private static int complementRgb(int rgb) {
		int red = (rgb >> 16) & 0xFF;
		int green = (rgb >> 8) & 0xFF;
		int blue = rgb & 0xFF;
		return ((255 - red) << 16) | ((255 - green) << 8) | (255 - blue);
	}

	public void close() {
		if (!open) {
			return;
		}

		if (isAnvilMode()) {
			if (anvilActiveTab == AnvilEditorTab.LORE) {
				syncLoreLinesFromFields();
				applyLore();
			} else if (anvilStyleFocus == AnvilStyleFocus.LORE_LINE) {
				syncPartsFromFields();
				updateSelectedLoreLine(currentDraft().serialize());
				applyLore();
			}
		}

		tearDownWidgets();
		open = false;
		ACTIVE.remove(parent);
		if (onClose != null) {
			onClose.run();
		}
	}

	private void tearDownWidgets() {
		clearSelectionOutlines();
		ModUiSelectionState.clear();
		effectButtons.clear();
		if (colorPicker != null) {
			colorPicker.destroy();
			colorPicker = null;
		}
		gradientStartButton = null;
		gradientEndButton = null;
		solidModeButton = null;
		gradientModeButton = null;
		gradientPreview = null;

		for (GuiEventListener widget : widgets) {
			screenInvoker.hologrammenu$removeWidget(widget);
		}
		widgets.clear();
		partFields.clear();
		loreLineFields.clear();
		loreParagraphField = null;
		anvilStyleTabButton = null;
		anvilLoreTabButton = null;
		anvilEffectsTabButton = null;
		vanillaEnchantLevelField = null;
		rpgEffectButtons.clear();
		vanillaEnchantButtons.clear();
	}

	private void attach(GuiEventListener widget) {
		attach(widget, true);
	}

	private void attach(GuiEventListener widget, boolean interactive) {
		if (interactive) {
			if (widget instanceof Button button) {
				screenInvoker.hologrammenu$addRenderableWidget(button);
			} else if (widget instanceof EditBox editBox) {
				screenInvoker.hologrammenu$addRenderableWidget(editBox);
			} else if (widget instanceof ClassicColorSwatchButton swatchButton) {
				screenInvoker.hologrammenu$addRenderableWidget(swatchButton);
			} else if (widget instanceof RgbColorPickerWidget picker) {
				screenInvoker.hologrammenu$addRenderableWidget(picker);
			} else if (widget instanceof AbstractWidget abstractWidget) {
				screenInvoker.hologrammenu$addRenderableWidget(abstractWidget);
			}
		} else if (widget instanceof net.minecraft.client.gui.components.Renderable renderable) {
			screenInvoker.hologrammenu$addRenderableOnly(renderable);
		}
		widgets.add(widget);
		if (widget instanceof AbstractWidget abstractWidget) {
			ModUiRenderContext.markIfInteractive(abstractWidget);
		}
		if (dragGroup != null && widget instanceof AbstractWidget abstractWidget && !(widget instanceof DraggableTitleBarWidget)) {
			dragGroup.track(abstractWidget);
		}
	}

	private static Button iconButton(
		int x,
		int y,
		int width,
		int height,
		Component message,
		ItemStack icon,
		Button.OnPress onPress
	) {
		return VanillaIconButton.create(x, y, width, height, message, icon, onPress);
	}

	public static int[] clampPanelPosition(Screen screen, int panelX, int panelY) {
		return clampPanelPosition(screen, panelX, panelY, 1);
	}

	public static int[] clampPanelPosition(Screen screen, int panelX, int panelY, int partCount) {
		int panelHeight = TextStylePanelWidget.panelHeight(partCount);
		int maxX = Math.max(4, screen.width - TextStylePanelWidget.PANEL_WIDTH - 4);
		int maxY = Math.max(4, screen.height - panelHeight - 4);
		return new int[] {
			Math.max(4, Math.min(panelX, maxX)),
			Math.max(4, Math.min(panelY, maxY))
		};
	}

	private static final class TextPart {
		private String text = "";
		private OptionalInt color = OptionalInt.empty();
		private OptionalInt gradientEnd = OptionalInt.empty();
		private EnumSet<StyledText.Effect> effects = EnumSet.noneOf(StyledText.Effect.class);

		private static TextPart empty() {
			return new TextPart();
		}

		private static TextPart fromSpan(StyledSpan span, EnumSet<StyledText.Effect> globalEffects) {
			TextPart part = new TextPart();
			part.text = span.text();
			part.color = span.color();
			part.gradientEnd = span.gradientEnd();
			part.effects = span.effects().isEmpty()
				? EnumSet.copyOf(globalEffects)
				: EnumSet.copyOf(span.effects());
			return part;
		}

		private StyledSpan toSpan() {
			StyledSpan span;
			if (gradientEnd.isPresent() && color.isPresent()) {
				span = StyledSpan.gradient(color.getAsInt(), gradientEnd.getAsInt(), text);
			} else if (color.isPresent()) {
				span = StyledSpan.solid(color.getAsInt(), text);
			} else {
				span = StyledSpan.plain(text);
			}
			return effects.isEmpty() ? span : span.withEffects(effects);
		}

		private StyledText toStyledText() {
			return new StyledText(EnumSet.noneOf(StyledText.Effect.class), List.of(toSpan()));
		}

		private void applyFrom(StyledText styled) {
			text = styled.text();
			if (styled.spans().isEmpty()) {
				color = OptionalInt.empty();
				gradientEnd = OptionalInt.empty();
				effects = EnumSet.noneOf(StyledText.Effect.class);
				return;
			}
			if (styled.spans().size() == 1) {
				StyledSpan span = styled.spans().get(0);
				color = span.color();
				gradientEnd = span.gradientEnd();
				effects = span.effects().isEmpty()
					? EnumSet.copyOf(styled.effects())
					: EnumSet.copyOf(span.effects());
				return;
			}
			mergeStyledSpans(styled.spans(), styled.effects());
		}

		private void mergeStyledSpans(List<StyledSpan> spans, EnumSet<StyledText.Effect> globalEffects) {
			color = OptionalInt.empty();
			gradientEnd = OptionalInt.empty();
			effects = EnumSet.noneOf(StyledText.Effect.class);
			for (StyledSpan span : spans) {
				if (!span.text().isEmpty() && effects.isEmpty()) {
					effects = span.effects().isEmpty()
						? EnumSet.copyOf(globalEffects)
						: EnumSet.copyOf(span.effects());
				}
				if (span.isGradient()) {
					color = span.color();
					gradientEnd = span.gradientEnd();
					return;
				}
				if (span.color().isPresent()) {
					color = span.color();
					gradientEnd = OptionalInt.empty();
				}
			}
		}

		private void setSolidColor(int rgb) {
			color = OptionalInt.of(rgb & 0xFFFFFF);
			gradientEnd = OptionalInt.empty();
		}

		private void setGradient(int startRgb, int endRgb) {
			color = OptionalInt.of(startRgb & 0xFFFFFF);
			gradientEnd = OptionalInt.of(endRgb & 0xFFFFFF);
		}

		private void toggleEffect(StyledText.Effect effect) {
			if (effects.contains(effect)) {
				effects.remove(effect);
			} else {
				effects.add(effect);
			}
		}
	}

	private enum EffectsSubTab {
		RPG,
		ENCHANTS,
		SWORD,
		ARMOR
	}

	private enum EditorEffectsMode {
		NONE,
		ENCHANTS_ONLY,
		ALL
	}

	private enum LoreDynamicStyle {
		CLEAN("screen.hologrammenu.anvil.lore_style.clean"),
		ACCENT("screen.hologrammenu.anvil.lore_style.accent"),
		GRADIENT("screen.hologrammenu.anvil.lore_style.gradient"),
		RARITY("screen.hologrammenu.anvil.lore_style.rarity");

		private final String translationKey;

		LoreDynamicStyle(String translationKey) {
			this.translationKey = translationKey;
		}

		private String translationKey() {
			return translationKey;
		}
	}

	private record VanillaEnchantOption(String id, String label) {
	}

	private record RpgEffectOption(String id, String name) {
	}

	private record WordRange(int start, int end) {
	}

	private record SelectionRange(int start, int end) {
		private static final SelectionRange NONE = new SelectionRange(0, 0);

		private boolean hasSelection() {
			return end > start;
		}
	}

	private record SwordAspect(String name, String colorCode, String valueFormat, int min, int max, int step) {
		private String valueText(int value) {
			if (name.equals("Cooldown")) {
				return String.format(java.util.Locale.ROOT, valueFormat, String.format(java.util.Locale.ROOT, "%.1f", value / 10.0F));
			}
			return String.format(java.util.Locale.ROOT, valueFormat, value);
		}
	}

	private record ArmorAspect(String name, String colorCode, String valueFormat, int min, int max, int step) {
		private String valueText(int value) {
			return String.format(java.util.Locale.ROOT, valueFormat, value);
		}
	}

	private enum ColorEditMode {
		SOLID,
		GRADIENT
	}

	private enum GradientTarget {
		START,
		END
	}
}
