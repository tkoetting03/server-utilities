package com.hologrammenu.client.screen;

import com.hologrammenu.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.hologrammenu.client.mixin.accessor.ScreenInvoker;
import com.hologrammenu.client.screen.widget.DraggableTitleBarWidget;
import com.hologrammenu.client.screen.widget.LabeledFieldLayout;
import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.ShopItemPickerGridWidget;
import com.hologrammenu.client.screen.widget.ShopItemPickerPanelWidget;
import com.hologrammenu.client.screen.widget.UiScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class ShopItemPickerOverlay {
	private final Screen parent;
	private final ScreenInvoker screenInvoker;
	private AbstractContainerScreenAccessor layout;
	private final List<GuiEventListener> widgets = new ArrayList<>();
	private final List<Item> allItems = new ArrayList<>();

	private DraggablePanelGroup dragGroup;
	private EditBox searchField;
	private ShopItemPickerGridWidget itemGrid;
	private Button confirmButton;
	private Component title = Component.empty();
	private Consumer<ItemStack> onSelect = stack -> {};
	private boolean confirmSelection;

	private List<Item> visibleItems = List.of();
	private boolean open;
	private int scrollRow;
	private int selectedIndex = -1;

	public ShopItemPickerOverlay(Screen parent, AbstractContainerScreenAccessor layout) {
		this.parent = parent;
		this.screenInvoker = (ScreenInvoker) parent;
		this.layout = layout;
	}

	public void updateLayout(AbstractContainerScreenAccessor layout) {
		this.layout = layout;
		if (open) {
			buildWidgets();
		}
	}

	public boolean isOpen() {
		return open;
	}

	public void open(Component title, Consumer<ItemStack> onSelect) {
		open(title, onSelect, false);
	}

	public void open(Component title, Consumer<ItemStack> onSelect, boolean confirmSelection) {
		this.title = title;
		this.onSelect = onSelect;
		this.confirmSelection = confirmSelection;
		if (allItems.isEmpty()) {
			for (Item item : BuiltInRegistries.ITEM) {
				if (item != Items.AIR) {
					allItems.add(item);
				}
			}
			allItems.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
		}
		scrollRow = 0;
		selectedIndex = -1;
		open = true;
		buildWidgets();
	}

	public void close() {
		if (!open) {
			return;
		}
		tearDownWidgets();
		open = false;
		scrollRow = 0;
		selectedIndex = -1;
		confirmSelection = false;
	}

	public boolean mouseScrolled(double scrollY) {
		if (!open || scrollY == 0.0D) {
			return false;
		}
		scrollRow = Math.max(0, Math.min(maxScrollRow(), scrollRow - (int) Math.signum(scrollY)));
		return true;
	}

	private void buildWidgets() {
		tearDownWidgets();
		refreshVisibleItems();

		int panelWidth = ShopItemPickerPanelWidget.panelWidth();
		int panelHeight = ShopItemPickerPanelWidget.panelHeight(confirmSelection);
		int panelPadding = ModPanelLayout.PANEL_PADDING;
		int contentWidth = ShopItemPickerPanelWidget.contentWidth();
		int fieldHeight = LabeledFieldLayout.FIELD_HEIGHT;

		int[] defaultPosition = ShopItemPickerPanelPositions.rightOfContainer(layout, parent);
		dragGroup = new DraggablePanelGroup(parent, "shop_item_picker");
		int[] position = dragGroup.resolvePosition(defaultPosition[0], defaultPosition[1], panelWidth, panelHeight);
		int panelX = position[0];
		int panelY = position[1];
		int contentLeft = panelX + panelPadding;
		int contentTop = panelY + ShopItemPickerPanelWidget.CONTENT_TOP;

		var panelWidget = new ShopItemPickerPanelWidget(panelX, panelY, title, confirmSelection);
		screenInvoker.hologrammenu$addRenderableOnly(panelWidget);
		widgets.add(panelWidget);
		dragGroup.track(panelWidget);

		Font font = Minecraft.getInstance().font;
		int searchLabelY = contentTop;
		attachLabel(font, contentLeft, searchLabelY, contentWidth, Component.translatable("screen.hologrammenu.shop.search_items"));
		int searchFieldY = searchLabelY + LabeledFieldLayout.LABEL_HEIGHT + LabeledFieldLayout.LABEL_GAP;
		searchField = new EditBox(
			font,
			contentLeft,
			searchFieldY,
			contentWidth,
			fieldHeight,
			Component.translatable("screen.hologrammenu.shop.search_items")
		);
		searchField.setMaxLength(64);
		searchField.setResponder(value -> {
			scrollRow = 0;
			selectedIndex = -1;
			refreshVisibleItems();
		});
		attachInteractive(searchField);

		int gridY = searchFieldY + fieldHeight + ModPanelLayout.SECTION_GAP;
		itemGrid = new ShopItemPickerGridWidget(
			contentLeft,
			gridY,
			ShopItemPickerPanelWidget.COLS * UiScale.s(UiScale.PICKER_SLOT_BASE),
			ShopItemPickerPanelWidget.gridHeight(),
			() -> visibleItems,
			() -> scrollRow,
			() -> selectedIndex,
			this::selectIndex
		);
		attachInteractive(itemGrid);

		if (confirmSelection) {
			int confirmY = gridY + ShopItemPickerPanelWidget.gridHeight() + ModPanelLayout.ROW_GAP;
			confirmButton = Button.builder(Component.translatable("screen.hologrammenu.shop.confirm"), press -> confirmSelectedItem())
				.bounds(contentLeft, confirmY, contentWidth, fieldHeight)
				.build();
			confirmButton.active = false;
			attachButton(confirmButton);
		}

		DraggableTitleBarWidget titleBar = dragGroup.createTitleBar(title, panelWidth, UiScale.s(14));
		attachButton(titleBar);
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
		itemGrid = null;
		confirmButton = null;
		dragGroup = null;
	}

	private void refreshVisibleItems() {
		String query = searchField == null ? "" : searchField.getValue().trim().toLowerCase(Locale.ROOT);
		if (query.isEmpty()) {
			visibleItems = List.copyOf(allItems);
			updateConfirmButton();
			return;
		}
		List<Item> filtered = new ArrayList<>();
		for (Item item : allItems) {
			String id = BuiltInRegistries.ITEM.getKey(item).toString();
			String name = new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT);
			if (id.contains(query) || name.contains(query)) {
				filtered.add(item);
			}
		}
		visibleItems = filtered;
		if (selectedIndex >= visibleItems.size()) {
			selectedIndex = -1;
		}
		updateConfirmButton();
	}

	private void selectIndex(int index) {
		if (index < 0 || index >= visibleItems.size()) {
			return;
		}
		if (confirmSelection) {
			selectedIndex = index;
			updateConfirmButton();
			return;
		}
		applySelection(index);
	}

	private void confirmSelectedItem() {
		if (selectedIndex < 0 || selectedIndex >= visibleItems.size()) {
			return;
		}
		applySelection(selectedIndex);
	}

	private void applySelection(int index) {
		onSelect.accept(new ItemStack(visibleItems.get(index)));
		close();
	}

	private void updateConfirmButton() {
		if (confirmButton != null) {
			confirmButton.active = selectedIndex >= 0 && selectedIndex < visibleItems.size();
		}
	}

	private int maxScrollRow() {
		int totalRows = (visibleItems.size() + ShopItemPickerPanelWidget.COLS - 1) / ShopItemPickerPanelWidget.COLS;
		return Math.max(0, totalRows - ShopItemPickerPanelWidget.VISIBLE_ROWS);
	}
}
