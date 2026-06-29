package com.hologrammenu.client.screen;

import com.hologrammenu.client.head.HeadPresetClientState;
import com.hologrammenu.client.head.HeadPresetStacks;
import com.hologrammenu.client.mixin.accessor.ScreenInvoker;
import com.hologrammenu.client.screen.widget.DraggableTitleBarWidget;
import com.hologrammenu.client.screen.widget.LabeledFieldLayout;
import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.HeadPresetPickerPanelWidget;
import com.hologrammenu.client.screen.widget.HeadPresetGridWidget;
import com.hologrammenu.client.screen.widget.UiScale;
import com.hologrammenu.head.HeadPresetCategories;
import com.hologrammenu.head.HeadPresetEntry;
import com.hologrammenu.head.HeadPresetService;
import com.hologrammenu.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class HeadPresetPickerOverlay {
	private static final Map<Screen, HeadPresetPickerOverlay> ACTIVE = new WeakHashMap<>();

	private final Screen parent;
	private final ScreenInvoker screenInvoker;
	private final Consumer<HeadPresetEntry> onSelect;
	private final Supplier<int[]> panelPositionSupplier;
	private final List<GuiEventListener> widgets = new ArrayList<>();
	private final List<HeadPresetEntry> entries = new ArrayList<>();
	private final List<ItemStack> entryStacks = new ArrayList<>();

	private DraggablePanelGroup dragGroup;
	private EditBox searchField;
	private Button categoryButton;
	private Button prevPageButton;
	private Button nextPageButton;
	private StringWidget statusLabel;
	private HeadPresetGridWidget itemGrid;
	private Runnable removeListener;

	private int layoutContentLeft;
	private int layoutContentWidth;

	private String category = HeadPresetCategories.defaultCategory();
	private String query = "";
	private int page;
	private int totalCount;
	private int scrollRow;
	private int selectedIndex = -1;
	private boolean open;
	private boolean available = true;
	private String statusMessage = "";
	private long nextLoadingRetryAt;

	public HeadPresetPickerOverlay(
		Screen parent,
		Consumer<HeadPresetEntry> onSelect,
		Supplier<int[]> panelPositionSupplier
	) {
		this.parent = parent;
		this.onSelect = onSelect;
		this.panelPositionSupplier = panelPositionSupplier;
		this.screenInvoker = (ScreenInvoker) parent;
	}

	public static HeadPresetPickerOverlay getActive(Screen screen) {
		return ACTIVE.get(screen);
	}

	public static void onScreenRemoved(Screen screen) {
		HeadPresetPickerOverlay overlay = ACTIVE.remove(screen);
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
		removeListener = HeadPresetClientState.addListener(this::applyResponse);
		requestEntries();
		buildWidgets();
	}

	public void close() {
		if (!open) {
			return;
		}
		tearDownWidgets();
		if (removeListener != null) {
			removeListener.run();
			removeListener = null;
		}
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

	private void requestEntries() {
		ClientPlayNetworking.send(new ModPackets.HeadPresetListRequestPayload(category, query, page));
	}

	private void applyResponse(ModPackets.HeadPresetListResponsePayload payload) {
		if (!open) {
			return;
		}
		available = payload.available();
		statusMessage = payload.message();
		if (!available && "hud.hologrammenu.head_presets.loading".equals(statusMessage)) {
			long now = System.currentTimeMillis();
			if (now >= nextLoadingRetryAt) {
				nextLoadingRetryAt = now + 500L;
				Minecraft.getInstance().execute(() -> {
					if (open) {
						requestEntries();
					}
				});
			}
			updateStatusLabel();
			return;
		}
		if (payload.category() != null && !payload.category().isBlank()) {
			category = payload.category();
		}
		query = payload.query() == null ? "" : payload.query();
		page = payload.page();
		totalCount = payload.totalCount();
		entries.clear();
		entries.addAll(payload.entries());
		entryStacks.clear();
		for (HeadPresetEntry entry : entries) {
			entryStacks.add(HeadPresetStacks.create(entry));
		}
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
		dragGroup = new DraggablePanelGroup(parent, "head_preset_picker");
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
			Component.translatable("screen.hologrammenu.head_presets.title")
		);
		screenInvoker.hologrammenu$addRenderableOnly(panelWidget);
		widgets.add(panelWidget);
		dragGroup.track(panelWidget);

		Font font = Minecraft.getInstance().font;
		int searchLabelY = contentTop;
		attachLabel(font, contentLeft, searchLabelY, contentWidth, Component.translatable("screen.hologrammenu.head_presets.search"));
		int searchFieldY = searchLabelY + LabeledFieldLayout.LABEL_HEIGHT + LabeledFieldLayout.LABEL_GAP;
		searchField = new EditBox(font, contentLeft, searchFieldY, contentWidth, fieldHeight, Component.translatable("screen.hologrammenu.head_presets.search"));
		searchField.setMaxLength(64);
		searchField.setValue(query);
		searchField.setResponder(value -> {
			query = value == null ? "" : value;
			page = 0;
			scrollRow = 0;
			selectedIndex = -1;
			requestEntries();
		});
		attachInteractive(searchField);

		int categoryY = searchFieldY + fieldHeight + ModPanelLayout.ROW_GAP;
		categoryButton = Button.builder(categoryLabel(), press -> cycleCategory())
			.bounds(contentLeft, categoryY, contentWidth, fieldHeight)
			.build();
		attachButton(categoryButton);

		int gridY = categoryY + fieldHeight + ModPanelLayout.SECTION_GAP;
		itemGrid = new HeadPresetGridWidget(
			contentLeft,
			gridY,
			HeadPresetPickerPanelWidget.COLS * HeadPresetPickerPanelWidget.slotSize(),
			HeadPresetPickerPanelWidget.gridHeight(),
			() -> entries,
			() -> entryStacks,
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
			Component.translatable("screen.hologrammenu.head_presets.title"),
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
		return Component.translatable("screen.hologrammenu.head_presets.category", formatCategory(category));
	}

	private String formatCategory(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		return value.toLowerCase(Locale.ROOT).replace('_', ' ');
	}

	private void cycleCategory() {
		String[] categories = HeadPresetService.browseCategories();
		if (categories.length == 0) {
			categories = HeadPresetCategories.PUBLIC;
		}
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
		requestEntries();
	}

	private void changePage(int delta) {
		int maxPage = Math.max(0, (totalCount - 1) / HeadPresetService.PAGE_SIZE);
		int next = Math.max(0, Math.min(maxPage, page + delta));
		if (next == page) {
			return;
		}
		page = next;
		scrollRow = 0;
		selectedIndex = -1;
		requestEntries();
	}

	private void selectIndex(int index) {
		if (!available || index < 0 || index >= entries.size()) {
			return;
		}
		onSelect.accept(entries.get(index));
		close();
	}

	private void updatePageButtons() {
		if (prevPageButton == null || nextPageButton == null) {
			return;
		}
		int maxPage = Math.max(0, (totalCount - 1) / HeadPresetService.PAGE_SIZE);
		prevPageButton.active = page > 0;
		nextPageButton.active = page < maxPage;
	}

	private void updateStatusLabel() {
		if (statusLabel == null) {
			return;
		}
		Component message;
		if (!available) {
			message = statusMessage == null || statusMessage.isBlank()
				? Component.translatable("hud.hologrammenu.head_presets.unavailable")
				: Component.translatable(statusMessage);
		} else {
			int maxPage = Math.max(1, (totalCount + HeadPresetService.PAGE_SIZE - 1) / HeadPresetService.PAGE_SIZE);
			message = Component.translatable(
				"screen.hologrammenu.head_presets.status",
				totalCount,
				page + 1,
				maxPage
			);
		}
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
