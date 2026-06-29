package com.hologrammenu.client.screen;

import com.hologrammenu.client.mixin.accessor.ScreenInvoker;
import com.hologrammenu.client.screen.widget.DraggableTitleBarWidget;
import com.hologrammenu.client.screen.widget.HeadPresetPickerPanelWidget;
import com.hologrammenu.client.screen.widget.LabeledFieldLayout;
import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.ParticlePresetGridWidget;
import com.hologrammenu.client.screen.widget.UiScale;
import com.hologrammenu.particle.ParticlePresetCatalog;
import com.hologrammenu.particle.ParticlePresetCategories;
import com.hologrammenu.particle.ParticlePresetEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ParticlePresetPickerOverlay {
	private static final Map<Screen, ParticlePresetPickerOverlay> ACTIVE = new WeakHashMap<>();

	private final Screen parent;
	private final ScreenInvoker screenInvoker;
	private final Consumer<ParticlePresetEntry> onSelect;
	private final Supplier<int[]> panelPositionSupplier;
	private final List<GuiEventListener> widgets = new ArrayList<>();
	private final List<ParticlePresetEntry> entries = new ArrayList<>();

	private DraggablePanelGroup dragGroup;
	private EditBox searchField;
	private Button categoryButton;
	private Button prevPageButton;
	private Button nextPageButton;
	private StringWidget statusLabel;
	private ParticlePresetGridWidget itemGrid;

	private int layoutContentLeft;
	private int layoutContentWidth;

	private String category = ParticlePresetCategories.defaultCategory();
	private String query = "";
	private int page;
	private int totalCount;
	private int scrollRow;
	private int selectedIndex = -1;
	private boolean open;

	public ParticlePresetPickerOverlay(
		Screen parent,
		Consumer<ParticlePresetEntry> onSelect,
		Supplier<int[]> panelPositionSupplier
	) {
		this.parent = parent;
		this.onSelect = onSelect;
		this.panelPositionSupplier = panelPositionSupplier;
		this.screenInvoker = (ScreenInvoker) parent;
	}

	public static void onScreenRemoved(Screen screen) {
		ParticlePresetPickerOverlay overlay = ACTIVE.remove(screen);
		if (overlay != null) {
			overlay.close();
		}
	}

	public boolean isOpen() {
		return open;
	}

	public void toggle() {
		if (open) {
			close();
		} else {
			open();
		}
	}

	public void open() {
		open = true;
		ACTIVE.put(parent, this);
		page = 0;
		scrollRow = 0;
		selectedIndex = -1;
		refreshEntries();
		buildWidgets();
	}

	public void close() {
		if (!open) {
			return;
		}
		tearDownWidgets();
		open = false;
		ACTIVE.remove(parent);
	}

	public boolean mouseScrolled(double scrollY) {
		if (!open || scrollY == 0.0D) {
			return false;
		}
		scrollRow = Math.max(0, Math.min(maxScrollRow(), scrollRow - (int) Math.signum(scrollY)));
		return true;
	}

	private void refreshEntries() {
		entries.clear();
		entries.addAll(ParticlePresetCatalog.list(category, query, page, ParticlePresetCatalog.PAGE_SIZE));
		totalCount = ParticlePresetCatalog.count(category, query);
		if (selectedIndex >= entries.size()) {
			selectedIndex = -1;
		}
		updateStatusLabel();
		updatePageButtons();
	}

	private void buildWidgets() {
		tearDownWidgets();

		int panelWidth = HeadPresetPickerPanelWidget.panelWidth();
		int panelHeight = panelHeight();
		int panelPadding = ModPanelLayout.PANEL_PADDING;
		int contentWidth = HeadPresetPickerPanelWidget.contentWidth();
		int fieldHeight = LabeledFieldLayout.FIELD_HEIGHT;
		int rowGap = ModPanelLayout.ROW_GAP;

		int[] defaultPosition = panelPositionSupplier.get();
		dragGroup = new DraggablePanelGroup(parent, "particle_preset_picker");
		int[] position = dragGroup.resolvePosition(defaultPosition[0], defaultPosition[1], panelWidth, panelHeight);
		int panelX = position[0];
		int panelY = position[1];
		int contentLeft = panelX + panelPadding;
		layoutContentLeft = contentLeft;
		layoutContentWidth = contentWidth;
		int contentTop = panelY + HeadPresetPickerPanelWidget.CONTENT_TOP;

		var panelWidget = new HeadPresetPickerPanelWidget(
			panelX,
			panelY,
			panelWidth,
			panelHeight,
			Component.translatable("screen.hologrammenu.particle_presets.title")
		);
		screenInvoker.hologrammenu$addRenderableOnly(panelWidget);
		widgets.add(panelWidget);
		dragGroup.track(panelWidget);

		Font font = Minecraft.getInstance().font;
		int searchLabelY = contentTop;
		attachLabel(font, contentLeft, searchLabelY, contentWidth, Component.translatable("screen.hologrammenu.particle_presets.search"));
		int searchFieldY = searchLabelY + LabeledFieldLayout.LABEL_HEIGHT + LabeledFieldLayout.LABEL_GAP;
		searchField = new EditBox(font, contentLeft, searchFieldY, contentWidth, fieldHeight, Component.translatable("screen.hologrammenu.particle_presets.search"));
		searchField.setMaxLength(64);
		searchField.setValue(query);
		searchField.setResponder(value -> {
			query = value == null ? "" : value;
			page = 0;
			scrollRow = 0;
			selectedIndex = -1;
			refreshEntries();
		});
		attachInteractive(searchField);

		int categoryY = searchFieldY + fieldHeight + ModPanelLayout.ROW_GAP;
		categoryButton = Button.builder(categoryLabel(), press -> cycleCategory())
			.bounds(contentLeft, categoryY, contentWidth, fieldHeight)
			.build();
		attachButton(categoryButton);

		int gridY = categoryY + fieldHeight + ModPanelLayout.SECTION_GAP;
		itemGrid = new ParticlePresetGridWidget(
			contentLeft,
			gridY,
			HeadPresetPickerPanelWidget.COLS * HeadPresetPickerPanelWidget.slotSize(),
			HeadPresetPickerPanelWidget.gridHeight(),
			() -> entries,
			() -> scrollRow,
			() -> selectedIndex,
			this::selectIndex
		);
		attachInteractive(itemGrid);

		int pageButtonWidth = fieldHeight;
		int pageButtonsWidth = pageButtonWidth * 2 + rowGap;
		int pageButtonsX = centeredX(contentLeft, contentWidth, pageButtonsWidth);
		int pageY = gridY + HeadPresetPickerPanelWidget.gridHeight() + ModPanelLayout.ROW_GAP;
		prevPageButton = Button.builder(Component.literal("<"), press -> changePage(-1))
			.bounds(pageButtonsX, pageY, pageButtonWidth, fieldHeight)
			.build();
		nextPageButton = Button.builder(Component.literal(">"), press -> changePage(1))
			.bounds(pageButtonsX + pageButtonWidth + rowGap, pageY, pageButtonWidth, fieldHeight)
			.build();
		attachButton(prevPageButton);
		attachButton(nextPageButton);

		int statusY = pageY + fieldHeight + ModPanelLayout.ROW_GAP;
		statusLabel = new StringWidget(contentLeft, statusY, contentWidth, LabeledFieldLayout.LABEL_HEIGHT, Component.empty(), font);
		screenInvoker.hologrammenu$addRenderableOnly(statusLabel);
		widgets.add(statusLabel);
		dragGroup.track(statusLabel);

		DraggableTitleBarWidget titleBar = dragGroup.createTitleBar(
			Component.translatable("screen.hologrammenu.particle_presets.title"),
			panelWidth,
			UiScale.s(14)
		);
		attachButton(titleBar);

		updateStatusLabel();
		updatePageButtons();
	}

	private int panelHeight() {
		int fieldHeight = LabeledFieldLayout.FIELD_HEIGHT;
		int labeledSearchRow = LabeledFieldLayout.LABEL_HEIGHT + LabeledFieldLayout.LABEL_GAP + fieldHeight;
		return HeadPresetPickerPanelWidget.CONTENT_TOP
			+ labeledSearchRow + ModPanelLayout.ROW_GAP
			+ fieldHeight + ModPanelLayout.SECTION_GAP
			+ HeadPresetPickerPanelWidget.gridHeight() + ModPanelLayout.ROW_GAP
			+ fieldHeight + ModPanelLayout.ROW_GAP
			+ LabeledFieldLayout.LABEL_HEIGHT
			+ ModPanelLayout.PANEL_PADDING;
	}

	private static int centeredX(int contentLeft, int contentWidth, int width) {
		return contentLeft + (contentWidth - width) / 2;
	}

	private Component categoryLabel() {
		return Component.translatable("screen.hologrammenu.particle_presets.category", formatCategory(category));
	}

	private String formatCategory(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		return value.toLowerCase(Locale.ROOT).replace('_', ' ');
	}

	private void cycleCategory() {
		String[] categories = ParticlePresetCategories.ALL;
		int index = 0;
		for (int i = 0; i < categories.length; i++) {
			if (categories[i].equals(category)) {
				index = i;
				break;
			}
		}
		category = categories[(index + 1) % categories.length];
		page = 0;
		scrollRow = 0;
		selectedIndex = -1;
		if (categoryButton != null) {
			categoryButton.setMessage(categoryLabel());
		}
		refreshEntries();
	}

	private void changePage(int delta) {
		int maxPage = Math.max(0, (totalCount - 1) / ParticlePresetCatalog.PAGE_SIZE);
		int next = Math.max(0, Math.min(maxPage, page + delta));
		if (next == page) {
			return;
		}
		page = next;
		scrollRow = 0;
		selectedIndex = -1;
		refreshEntries();
	}

	private void selectIndex(int index) {
		if (index < 0 || index >= entries.size()) {
			return;
		}
		onSelect.accept(entries.get(index));
		close();
	}

	private void updatePageButtons() {
		if (prevPageButton == null || nextPageButton == null) {
			return;
		}
		int maxPage = Math.max(0, (totalCount - 1) / ParticlePresetCatalog.PAGE_SIZE);
		prevPageButton.active = page > 0;
		nextPageButton.active = page < maxPage;
	}

	private void updateStatusLabel() {
		if (statusLabel == null) {
			return;
		}
		int maxPage = Math.max(1, (totalCount + ParticlePresetCatalog.PAGE_SIZE - 1) / ParticlePresetCatalog.PAGE_SIZE);
		Component message = Component.translatable(
			"screen.hologrammenu.particle_presets.status",
			totalCount,
			page + 1,
			maxPage
		);
		Font font = Minecraft.getInstance().font;
		int textWidth = font.width(message);
		statusLabel.setMessage(message);
		statusLabel.setWidth(textWidth);
		statusLabel.setX(centeredX(layoutContentLeft, layoutContentWidth, textWidth));
	}

	private int maxScrollRow() {
		int totalRows = (entries.size() + HeadPresetPickerPanelWidget.COLS - 1) / HeadPresetPickerPanelWidget.COLS;
		return Math.max(0, totalRows - HeadPresetPickerPanelWidget.VISIBLE_ROWS);
	}

	private void attachLabel(Font font, int x, int y, int width, Component text) {
		StringWidget label = new StringWidget(x, y, width, LabeledFieldLayout.LABEL_HEIGHT, text, font);
		screenInvoker.hologrammenu$addRenderableOnly(label);
		widgets.add(label);
		dragGroup.track(label);
	}

	private void attachInteractive(AbstractWidget widget) {
		screenInvoker.hologrammenu$addRenderableWidget(widget);
		ModUiRenderContext.markIfInteractive(widget);
		widgets.add(widget);
		dragGroup.track(widget);
	}

	private void attachButton(AbstractWidget button) {
		screenInvoker.hologrammenu$addRenderableWidget(button);
		ModUiRenderContext.markIfInteractive(button);
		widgets.add(button);
		if (dragGroup != null && !(button instanceof DraggableTitleBarWidget)) {
			dragGroup.track(button);
		}
	}

	private void tearDownWidgets() {
		for (GuiEventListener widget : widgets) {
			screenInvoker.hologrammenu$removeWidget(widget);
		}
		widgets.clear();
		searchField = null;
		categoryButton = null;
		prevPageButton = null;
		nextPageButton = null;
		statusLabel = null;
		itemGrid = null;
		dragGroup = null;
		layoutContentLeft = 0;
		layoutContentWidth = 0;
	}
}
