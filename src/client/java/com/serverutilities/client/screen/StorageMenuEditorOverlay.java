package com.serverutilities.client.screen;

import com.serverutilities.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.serverutilities.client.mixin.accessor.ScreenInvoker;
import com.serverutilities.client.screen.ModUiSelectionState;
import com.serverutilities.client.screen.widget.StorageMenuEditorMetrics;
import com.serverutilities.client.screen.widget.StorageMenuEditorPanelWidget;
import com.serverutilities.client.screen.widget.StorageMenuInventoryNumberBadge;
import com.serverutilities.client.screen.widget.StorageMenuInventoryTreeWidget;
import com.serverutilities.client.screen.widget.StorageMenuTreeNodeWidget;
import com.serverutilities.client.screen.widget.DraggableTitleBarWidget;
import com.serverutilities.client.screen.widget.LabeledFieldLayout;
import com.serverutilities.client.screen.widget.ModPanelLayout;
import com.serverutilities.client.screen.widget.FloatScaleSlider;
import com.serverutilities.client.screen.widget.HologramHeightSlider;
import com.serverutilities.client.screen.widget.StorageMenuHologramSettingsPanelWidget;
import com.serverutilities.client.screen.widget.StorageMenuSlotButton;
import com.serverutilities.client.screen.widget.UiLayoutHelper;
import com.serverutilities.client.screen.widget.UiScale;
import com.serverutilities.client.screen.widget.VanillaIconButton;
import com.serverutilities.client.storage.StorageMenuClientTracker;
import com.serverutilities.storage.ShopItemRules;
import com.serverutilities.network.ModPackets;
import com.serverutilities.storage.ShopDescriptions;
import com.serverutilities.storage.ShopDefinition;
import com.serverutilities.storage.ShopListing;
import com.serverutilities.storage.StorageMenuChrome;
import com.serverutilities.storage.StorageMenuDefinition;
import com.serverutilities.storage.StorageMenuFillerItems;
import com.serverutilities.storage.StorageMenuNetwork;
import com.serverutilities.storage.StorageMenuHologramSettings;
import com.serverutilities.storage.StorageMenuSlotConfig;
import com.serverutilities.storage.StorageMenuSlotType;
import com.serverutilities.storage.StorageMenuViewContext;
import com.serverutilities.storage.StorageMenuSizes;
import com.serverutilities.storage.StorageSubMenuManager;
import com.serverutilities.text.StyledText;
import com.serverutilities.text.TextFormats;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StorageMenuEditorOverlay {
	private record CachedDraft(
		int containerSize,
		String title,
		Map<Integer, StorageMenuSlotConfig> slots,
		boolean invulnerable,
		boolean hologramLabel,
		StorageMenuHologramSettings hologramSettings
	) {
	}

	private static final Map<Screen, StorageMenuEditorOverlay> ACTIVE = new java.util.WeakHashMap<>();

	private final Screen parent;
	private AbstractContainerScreenAccessor layout;
	private StorageMenuViewContext viewContext;
	private final ScreenInvoker screenInvoker;
	private final List<GuiEventListener> widgets = new ArrayList<>();

	private int containerSize;
	private final Map<Integer, StorageMenuSlotConfig> draftSlots = new HashMap<>();
	private final Map<String, CachedDraft> draftCache = new HashMap<>();
	private String menuTitle = "";
	private int selectedSlot = 0;
	private boolean open;
	private boolean awaitingSync;
	private boolean invulnerable;
	private boolean hologramLabel;
	private StorageMenuHologramSettings hologramSettings = StorageMenuHologramSettings.DEFAULT;
	private boolean hologramSettingsOpen;
	private boolean autoApplying;
	private boolean updatingFields;

	private StorageMenuEditorTab activeTab = StorageMenuEditorTab.MENU;
	private boolean shopEnabled;
	private final Map<Integer, ShopListing> shopListings = new HashMap<>();
	private int selectedShopSlot;

	private EditBox titleField;
	private EditBox itemField;
	private EditBox commandField;
	private EditBox shopProductField;
	private EditBox shopProductAmountField;
	private EditBox shopStockField;
	private EditBox costAmountField;
	private EditBox costItemField;
	private Button typeButton;
	private Button nameButton;
	private Button invulnerableButton;
	private Button hologramLabelButton;
	private Button hologramSettingsButton;
	private Button menuTabButton;
	private Button shopTabButton;

	private StorageMenuNameOverlay nameOverlay;
	private ShopItemPickerOverlay itemPickerOverlay;
	private TextStyleOverlay titleStyleOverlay;
	private DraggablePanelGroup editorDragGroup;
	private DraggablePanelGroup treeDragGroup;
	private DraggablePanelGroup activeDragGroup;
	private int editorPanelX;
	private int editorPanelY;

	public StorageMenuEditorOverlay(
		Screen parent,
		AbstractContainerScreenAccessor layout,
		StorageMenuViewContext initialViewContext,
		int containerSize
	) {
		this.parent = parent;
		this.layout = layout;
		this.viewContext = initialViewContext.immutable();
		this.containerSize = containerSize;
		this.screenInvoker = (ScreenInvoker) parent;
	}

	public static StorageMenuEditorOverlay getActive(Screen screen) {
		return ACTIVE.get(screen);
	}

	public static void handleSync(StorageMenuNetwork.MenuData menuData) {
		Minecraft client = Minecraft.getInstance();
		if (client.screen == null) {
			return;
		}
		StorageMenuEditorOverlay overlay = ACTIVE.get(client.screen);
		if (overlay != null) {
			overlay.applySync(menuData);
		}
	}

	public static void handleContext(StorageMenuViewContext context) {
		Minecraft client = Minecraft.getInstance();
		if (client.screen == null) {
			return;
		}
		StorageMenuEditorOverlay overlay = ACTIVE.get(client.screen);
		if (overlay != null) {
			overlay.applyContext(context);
		}
	}

	public static void onScreenRemoved(Screen screen) {
		StorageMenuEditorOverlay overlay = ACTIVE.remove(screen);
		if (overlay != null) {
			overlay.close();
		}
	}

	public boolean isOpen() {
		return open;
	}

	public boolean hasOpenModPanel() {
		if (open) {
			return true;
		}
		if (nameOverlay != null && nameOverlay.isOpen()) {
			return true;
		}
		if (itemPickerOverlay != null && itemPickerOverlay.isOpen()) {
			return true;
		}
		return titleStyleOverlay != null && titleStyleOverlay.isOpen();
	}

	public void dispose() {
		if (open) {
			disposeNameOverlay();
			disposeItemPickerOverlay();
			disposeTitleStyleOverlay();
			tearDownWidgets();
			open = false;
			ACTIVE.remove(parent);
		}
	}

	public boolean onMouseScrolled(double scrollY) {
		return itemPickerOverlay != null && itemPickerOverlay.isOpen() && itemPickerOverlay.mouseScrolled(scrollY);
	}

	public void reattach(AbstractContainerScreenAccessor layout, int containerSize) {
		this.layout = layout;
		if (itemPickerOverlay != null) {
			itemPickerOverlay.updateLayout(layout);
		}
		if (containerSize > 0) {
			this.containerSize = containerSize;
		}
		ACTIVE.put(parent, this);
		if (open) {
			rebuild();
		}
	}

	public void toggle() {
		if (open) {
			close();
		} else {
			syncViewContextFromTracker();
			open();
		}
	}

	public void syncViewContextFromTracker() {
		StorageMenuClientTracker.getActiveView().ifPresent(context -> {
			if (!context.equals(viewContext)) {
				applyContext(context);
			}
		});
	}

	public void open() {
		if (open) {
			return;
		}

		syncViewContextFromTracker();
		open = true;
		awaitingSync = true;
		ACTIVE.put(parent, this);
		requestMenuSync(viewContext);
		if (!viewContext.isRoot()) {
			requestMenuSync(rootTreeContext());
		}
	}

	private void requestMenuSync(StorageMenuViewContext context) {
		ClientPlayNetworking.send(new ModPackets.StorageMenuRequestPayload(
			context.anchorPos(),
			context.subMenuId(),
			context.npcEntityId()
		));
	}

	public void applySync(StorageMenuNetwork.MenuData menuData) {
		cacheDraftFromMenuData(menuData);
		if (open) {
			requestMissingTreeDrafts();
		}

		if (!menuData.viewContext().equals(viewContext)) {
			if (open) {
				scheduleRebuild();
			}
			return;
		}

		if (open && !awaitingSync) {
			return;
		}

		containerSize = menuData.containerSize();
		menuTitle = menuData.title() == null ? "" : menuData.title();
		invulnerable = menuData.invulnerable();
		hologramLabel = menuData.hologramLabel();
		hologramSettings = menuData.hologramSettings() == null ? StorageMenuHologramSettings.DEFAULT : menuData.hologramSettings();
		refreshHologramLabelButton();
		if (menuData.viewContext().isRoot()) {
			shopListings.clear();
			for (StorageMenuNetwork.ShopListingData listing : menuData.shopListings()) {
				shopListings.put(listing.slotIndex(), listing.toListing());
			}
			shopEnabled = menuData.shopEnabled() || !shopListings.isEmpty();
		}
		draftSlots.clear();
		for (StorageMenuNetwork.SlotData slot : menuData.slots()) {
			StorageMenuSlotConfig config = slot.toConfig();
			if (reservesChromeIndex(config.index())) {
				continue;
			}
			draftSlots.put(config.index(), config);
		}
		cacheCurrentDraft();
		awaitingSync = false;

		if (open) {
			rebuild();
		}
	}

	private void applyContext(StorageMenuViewContext context) {
		if (context.equals(viewContext)) {
			return;
		}
		EditorMousePreservation.arm();
		if (!context.isRoot()) {
			activeTab = StorageMenuEditorTab.MENU;
		}
		stashCurrentDraft();
		viewContext = context.immutable();
		selectedSlot = 0;
		if (!open) {
			return;
		}
		if (loadCachedDraft(viewContext)) {
			awaitingSync = false;
			rebuild();
			return;
		}
		awaitingSync = true;
		ClientPlayNetworking.send(new ModPackets.StorageMenuRequestPayload(
			viewContext.anchorPos(),
			viewContext.subMenuId(),
			viewContext.npcEntityId()
		));
		rebuild();
	}

	public void close() {
		if (!open) {
			return;
		}

		disposeNameOverlay();
		disposeItemPickerOverlay();
		disposeTitleStyleOverlay();
		tearDownWidgets();
		open = false;
		awaitingSync = false;
		ACTIVE.remove(parent);
	}

	private void rebuild() {
		EditorMousePreservation.runPreservingMouse(this::rebuildInternal);
	}

	private void scheduleRebuild() {
		Minecraft.getInstance().execute(() -> EditorMousePreservation.runPreservingMouse(this::rebuildInternal));
	}

	private void rebuildInternal() {
		if (nameOverlay != null) {
			nameOverlay.close();
		}
		tearDownWidgets();
		int editorHeight = editorContentHeight();
		int[] defaultEditorPos = StorageMenuPanelPositions.leftOfContainer(layout, parent, editorHeight);
		editorDragGroup = new DraggablePanelGroup(parent, "storage_editor");
		int[] editorPos = editorDragGroup.resolvePosition(
			defaultEditorPos[0],
			defaultEditorPos[1],
			StorageMenuEditorPanelWidget.PANEL_WIDTH,
			editorHeight
		);
		buildWidgets(editorPos[0], editorPos[1], editorHeight);
	}

	private int editorContentHeight() {
		if (activeTab == StorageMenuEditorTab.SHOP && canEditShop()) {
			return shopContentHeight();
		}
		int columns = columnsForSize(containerSize);
		int rows = (containerSize + columns - 1) / columns;
		int gridHeight = rows * StorageMenuEditorMetrics.SLOT_STEP;
		int titleSection = LabeledFieldLayout.labeledRowHeight();
		int contentTop = canEditShop() ? StorageMenuEditorMetrics.CONTENT_TOP : StorageMenuEditorMetrics.HEADER_TOP;
		return contentTop + titleSection + StorageMenuEditorMetrics.INVENTORY_NUMBER_ROW_HEIGHT + gridHeight + StorageMenuEditorMetrics.SECTION_GAP + footerHeight();
	}

	private int shopContentHeight() {
		int columns = columnsForSize(containerSize);
		int rows = (containerSize + columns - 1) / columns;
		int gridHeight = rows * StorageMenuEditorMetrics.SLOT_STEP;
		return StorageMenuEditorMetrics.CONTENT_TOP + gridHeight + StorageMenuEditorMetrics.SECTION_GAP + shopFooterHeight();
	}

	private int shopFooterHeight() {
		int height = StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT + StorageMenuEditorMetrics.FOOTER_ROW_GAP;
		if (isShopItemSlot(selectedShopSlot)) {
			height += LabeledFieldLayout.labeledRowHeight();
			height += LabeledFieldLayout.labeledRowHeight();
			height += LabeledFieldLayout.LABEL_HEIGHT + LabeledFieldLayout.LABEL_GAP + StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT;
			height += LabeledFieldLayout.FIELD_HEIGHT + LabeledFieldLayout.ROW_GAP;
			height += LabeledFieldLayout.labeledRowHeight();
		} else {
			height += StorageMenuEditorMetrics.HINT_HEIGHT + LabeledFieldLayout.ROW_GAP;
		}
		height += StorageMenuEditorMetrics.FOOTER_ROW_GAP;
		height += StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT;
		height += StorageMenuEditorMetrics.FOOTER_SECTION_GAP;
		height += StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT;
		height += StorageMenuEditorMetrics.FOOTER_ROW_GAP;
		return height;
	}

	private int footerHeight() {
		int height = StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT + StorageMenuEditorMetrics.FOOTER_ROW_GAP;
		StorageMenuSlotType selectedType = slotConfig(selectedSlot).type();
		if (usesShopFooterLayout(selectedType)) {
			height += LabeledFieldLayout.labeledRowHeight();
			height += LabeledFieldLayout.labeledRowHeight();
		} else if (usesLinkFooterLayout(selectedType)) {
			height += LabeledFieldLayout.LABEL_HEIGHT + LabeledFieldLayout.LABEL_GAP + StorageMenuEditorMetrics.HINT_HEIGHT;
		} else {
			height += LabeledFieldLayout.labeledRowHeight();
			height += LabeledFieldLayout.labeledRowHeight();
		}
		height += StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT;
		height += StorageMenuEditorMetrics.FOOTER_ROW_GAP;
		height += StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT;
		height += StorageMenuEditorMetrics.FOOTER_SECTION_GAP;
		if (canResizeChest()) {
			height += StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT + StorageMenuEditorMetrics.FOOTER_ROW_GAP;
		}
		if (canToggleInvulnerable()) {
			height += StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT + StorageMenuEditorMetrics.FOOTER_ROW_GAP;
		}
		if (canToggleHologramLabel()) {
			height += StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT + StorageMenuEditorMetrics.FOOTER_ROW_GAP;
		}
		if (canEditHologramSettings()) {
			height += StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT + StorageMenuEditorMetrics.FOOTER_ROW_GAP;
		}
		height += StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT;
		height += StorageMenuEditorMetrics.SECTION_GAP;
		return height;
	}

	private static int columnWidth(int contentWidth, int columns, int gap) {
		return ModPanelLayout.columnWidth(contentWidth, columns, gap);
	}

	private int totalPanelHeight() {
		return editorContentHeight();
	}

	private List<StorageMenuInventoryTree.Node> buildTreeNodes() {
		return StorageMenuInventoryTree.build(
			rootTreeContext(),
			this::draftForTree
		);
	}

	private StorageMenuViewContext rootTreeContext() {
		if (viewContext.isNpcAnchored()) {
			return StorageMenuViewContext.forNpc(viewContext.npcEntityId());
		}
		return StorageMenuViewContext.root(viewContext.anchorPos());
	}

	private void cacheDraftFromMenuData(StorageMenuNetwork.MenuData menuData) {
		StorageMenuViewContext context = menuData.viewContext();
		Map<Integer, StorageMenuSlotConfig> slots = new HashMap<>();
		for (StorageMenuNetwork.SlotData slot : menuData.slots()) {
			StorageMenuSlotConfig config = slot.toConfig();
			if (StorageMenuChrome.isReservedIndex(menuData.containerSize(), config.index(), !context.isRoot())) {
				continue;
			}
			slots.put(config.index(), config);
		}
		draftCache.put(
			StorageMenuInventoryTree.draftKey(context),
			new CachedDraft(
				menuData.containerSize(),
				menuData.title() == null ? "" : menuData.title(),
				slots,
				menuData.invulnerable(),
				menuData.hologramLabel(),
				menuData.hologramSettings()
			)
		);
	}

	private boolean isTreeContextResolved() {
		if (viewContext.isRoot()) {
			return true;
		}
		for (StorageMenuInventoryTree.Node node : buildTreeNodes()) {
			if (node.context.equals(viewContext)) {
				return true;
			}
		}
		return false;
	}

	private void requestMissingTreeDrafts() {
		if (isTreeContextResolved()) {
			return;
		}

		String rootKey = StorageMenuInventoryTree.draftKey(rootTreeContext());
		if (!draftCache.containsKey(rootKey)) {
			requestMenuSync(rootTreeContext());
		}

		java.util.Set<String> requested = new java.util.HashSet<>();
		for (CachedDraft draft : draftCache.values()) {
			for (StorageMenuSlotConfig config : draft.slots().values()) {
				if (config.type() != StorageMenuSlotType.LINK || !config.hasSubMenu()) {
					continue;
				}
				String childId = config.subMenuId();
				if (draftCache.containsKey(childId) || !requested.add(childId)) {
					continue;
				}
				requestMenuSync(rootTreeContext().withSubMenu(childId));
			}
		}
	}

	private StorageMenuInventoryTree.MenuDraft draftForTree(String key) {
		String currentKey = StorageMenuInventoryTree.draftKey(viewContext);
		if (key.equals(currentKey)) {
			return new StorageMenuInventoryTree.MenuDraft(menuTitle, new HashMap<>(draftSlots));
		}
		CachedDraft cached = draftCache.get(key);
		if (cached == null) {
			return null;
		}
		return new StorageMenuInventoryTree.MenuDraft(cached.title(), new HashMap<>(cached.slots()));
	}

	private void cacheCurrentDraft() {
		draftCache.put(
			StorageMenuInventoryTree.draftKey(viewContext),
			new CachedDraft(containerSize, menuTitle, new HashMap<>(draftSlots), invulnerable, hologramLabel, hologramSettings)
		);
	}

	private void stashCurrentDraft() {
		cacheCurrentDraft();
	}

	private boolean loadCachedDraft(StorageMenuViewContext context) {
		CachedDraft cached = draftCache.get(StorageMenuInventoryTree.draftKey(context));
		if (cached == null) {
			return false;
		}
		containerSize = cached.containerSize();
		menuTitle = cached.title();
		invulnerable = cached.invulnerable();
		hologramLabel = cached.hologramLabel();
		hologramSettings = cached.hologramSettings() == null ? StorageMenuHologramSettings.DEFAULT : cached.hologramSettings();
		draftSlots.clear();
		draftSlots.putAll(cached.slots());
		return true;
	}

	private void switchToInventory(StorageMenuViewContext context) {
		if (context.equals(viewContext)) {
			return;
		}
		EditorMousePreservation.arm();
		if (!context.isRoot()) {
			activeTab = StorageMenuEditorTab.MENU;
		}
		stashCurrentDraft();
		viewContext = context.immutable();
		selectedSlot = 0;
		StorageMenuClientTracker.setActiveView(viewContext);
		if (loadCachedDraft(viewContext)) {
			awaitingSync = false;
			scheduleRebuild();
			return;
		}
		menuTitle = "";
		draftSlots.clear();
		awaitingSync = true;
		ClientPlayNetworking.send(new ModPackets.StorageMenuRequestPayload(
			viewContext.anchorPos(),
			viewContext.subMenuId(),
			viewContext.npcEntityId()
		));
		scheduleRebuild();
	}

	private boolean reservesChromeIndex(int index) {
		return StorageMenuChrome.isReservedIndex(containerSize, index, !viewContext.isRoot());
	}

	private void buildWidgets(int panelX, int panelY, int editorHeight) {
		editorPanelX = panelX;
		editorPanelY = panelY;
		int pad = StorageMenuEditorMetrics.PANEL_PADDING;
		int contentWidth = StorageMenuEditorMetrics.CONTENT_WIDTH;

		activeDragGroup = editorDragGroup;
		attach(new StorageMenuEditorPanelWidget(panelX, panelY, editorHeight), false);
		attachEditorTabs(panelX, panelY, pad, contentWidth);

		if (activeTab == StorageMenuEditorTab.SHOP && canEditShop()) {
			buildShopWidgets(panelX, panelY, pad, contentWidth);
		} else {
			buildMenuWidgets(panelX, panelY, pad, contentWidth);
		}
		activeDragGroup = null;
		if (hologramSettingsOpen && canEditHologramSettings()) {
			buildHologramSettingsPanel(panelX, panelY);
		}
		attachDraggableTitle(
			editorDragGroup,
			Component.translatable("screen.serverutilities.storage_menu.title"),
			StorageMenuEditorPanelWidget.PANEL_WIDTH,
			StorageMenuEditorMetrics.HEADER_TOP
		);
		attachInventoryTree();
		updateTabSelection();
	}

	private void updateTabSelection() {
		if (menuTabButton != null) {
			ModUiSelectionState.unmarkSelected(menuTabButton);
		}
		if (shopTabButton != null) {
			ModUiSelectionState.unmarkSelected(shopTabButton);
		}
		if (menuTabButton == null || shopTabButton == null) {
			return;
		}
		if (activeTab == StorageMenuEditorTab.MENU) {
			ModUiSelectionState.markSelected(menuTabButton);
		} else {
			ModUiSelectionState.markSelected(shopTabButton);
		}
	}

	private void attachEditorTabs(int panelX, int panelY, int pad, int contentWidth) {
		if (!canEditShop()) {
			return;
		}

		int tabY = panelY + StorageMenuEditorMetrics.HEADER_TOP;
		int tabGap = ModPanelLayout.ROW_GAP;
		int tabWidth = (contentWidth - tabGap) / 2;
		int tabX = panelX + pad;
		int tabButtonHeight = StorageMenuEditorMetrics.TAB_ROW_HEIGHT - UiScale.s(4);

		menuTabButton = Button.builder(Component.translatable("screen.serverutilities.storage_menu.tab.menu"), press -> switchTab(StorageMenuEditorTab.MENU))
			.bounds(tabX, tabY, tabWidth, tabButtonHeight).build();
		shopTabButton = Button.builder(Component.translatable("screen.serverutilities.storage_menu.tab.shop"), press -> switchTab(StorageMenuEditorTab.SHOP))
			.bounds(tabX + tabWidth + tabGap, tabY, tabWidth, tabButtonHeight).build();
		attach(menuTabButton, true);
		attach(shopTabButton, true);
	}

	private void switchTab(StorageMenuEditorTab tab) {
		if (!canEditShop() && tab == StorageMenuEditorTab.SHOP) {
			return;
		}
		if (activeTab == tab) {
			return;
		}
		if (tab == StorageMenuEditorTab.SHOP) {
			selectedShopSlot = selectedSlot;
		} else {
			selectedSlot = selectedShopSlot;
		}
		activeTab = tab;
		selectedShopSlot = Math.min(selectedShopSlot, Math.max(0, containerSize - 1));
		selectedSlot = Math.min(selectedSlot, Math.max(0, containerSize - 1));
		scheduleRebuild();
	}

	private boolean canEditShop() {
		return true;
	}

	private void buildMenuWidgets(int panelX, int panelY, int pad, int contentWidth) {
		int columns = columnsForSize(containerSize);
		int rows = (containerSize + columns - 1) / columns;
		int gridHeight = rows * StorageMenuEditorMetrics.SLOT_STEP;
		int contentTop = canEditShop() ? StorageMenuEditorMetrics.CONTENT_TOP : StorageMenuEditorMetrics.HEADER_TOP;
		int titleRowY = panelY + contentTop;
		int afterTitle = attachTitleRow(panelX + pad, titleRowY, contentWidth);

		int inventoryNumber = StorageMenuInventoryTree.numberForContext(buildTreeNodes(), viewContext);
		attach(new StorageMenuInventoryNumberBadge(panelX + pad, afterTitle, inventoryNumber), false);
		int gridY = afterTitle + StorageMenuEditorMetrics.INVENTORY_NUMBER_ROW_HEIGHT;

		int gridWidth = columns * StorageMenuEditorMetrics.SLOT_STEP;
		int gridX = panelX + pad + Math.max(0, (contentWidth - gridWidth) / 2);
		for (int index = 0; index < containerSize; index++) {
			int col = index % columns;
			int row = index / columns;
			attach(new StorageMenuSlotButton(
				gridX + col * StorageMenuEditorMetrics.SLOT_STEP,
				gridY + row * StorageMenuEditorMetrics.SLOT_STEP,
				index,
				() -> selectedSlot,
				this::selectSlot,
				this::slotConfig
			), true);
		}

		int footerY = gridY + gridHeight + StorageMenuEditorMetrics.SECTION_GAP;
		StorageMenuSlotConfig selected = slotConfig(selectedSlot);

		Button type = Button.builder(typeLabel(selected.type()), press -> cycleType())
			.bounds(panelX + pad, footerY, contentWidth, StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT).build();
		typeButton = type;
		attach(type, true);

		int nextY = footerY + StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT + StorageMenuEditorMetrics.FOOTER_ROW_GAP;
		boolean linkMode = selected.type() == StorageMenuSlotType.LINK;
		boolean shopMode = usesShopFooterLayout(selected.type());
		if (shopMode) {
			nextY = attachShopItemFields(panelX + pad, nextY, contentWidth, selectedSlot, false);
		} else if (linkMode) {
			attachLabel(panelX + pad, nextY, contentWidth, Component.translatable("screen.serverutilities.storage_menu.sub_inventory_hint"));
			nextY += LabeledFieldLayout.LABEL_HEIGHT + LabeledFieldLayout.LABEL_GAP + StorageMenuEditorMetrics.HINT_HEIGHT;
		} else {
			nextY = attachLabeledField(panelX + pad, nextY, Component.translatable("screen.serverutilities.storage_menu.item"), field -> {
				itemField = field;
				field.setMaxLength(128);
				field.setValue(itemId(selected.displayStack()));
				field.setResponder(value -> {
					if (!updatingFields) {
						updateSelectedSlot(slotConfig(selectedSlot).withDisplayStack(parseItem(value)));
					}
				});
			});
			nextY = attachLabeledField(panelX + pad, nextY, Component.translatable("screen.serverutilities.storage_menu.command"), field -> {
				commandField = field;
				field.setMaxLength(256);
				field.setValue(selected.command());
				field.setResponder(value -> {
					if (!updatingFields) {
						updateSelectedSlot(slotConfig(selectedSlot).withCommand(value));
					}
				});
			});
		}

		int actionY = nextY;
		int x = panelX + pad;
		int buttonH = StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT;
		int rowGap = StorageMenuEditorMetrics.FOOTER_ROW_GAP;

		attach(VanillaIconButton.create(x, actionY, contentWidth, buttonH, Component.translatable("screen.serverutilities.storage_menu.insert_item"), new ItemStack(Items.ITEM_FRAME), press -> openInsertItemPicker()), true);

		int nameFillY = actionY + buttonH + rowGap;
		int halfWidth = columnWidth(contentWidth, 2, rowGap);
		Button nameButtonWidget = VanillaIconButton.create(x, nameFillY, halfWidth, buttonH, Component.translatable("screen.serverutilities.storage_menu.name_item"), new ItemStack(Items.NAME_TAG), press -> toggleNameEditor());
		nameButtonWidget.active = canNameSelectedSlot();
		nameButton = nameButtonWidget;
		attach(nameButtonWidget, true);
		attach(VanillaIconButton.create(x + halfWidth + rowGap, nameFillY, halfWidth, buttonH, Component.translatable("screen.serverutilities.storage_menu.fill_filler"), StorageMenuFillerItems.defaultFiller(), press -> fillFiller()), true);

		int applyRowY = nameFillY + buttonH + StorageMenuEditorMetrics.FOOTER_SECTION_GAP;
		if (canResizeChest()) {
			attach(VanillaIconButton.create(x, applyRowY, contentWidth, buttonH, chestSizeLabel(), new ItemStack(Items.CHEST), press -> toggleChestSize()), true);
			applyRowY += buttonH + rowGap;
		}
		if (canToggleInvulnerable()) {
			invulnerableButton = VanillaIconButton.create(x, applyRowY, contentWidth, buttonH, invulnerableLabel(), new ItemStack(Items.IRON_DOOR), press -> toggleInvulnerable());
			attach(invulnerableButton, true);
			applyRowY += buttonH + rowGap;
		}
		if (canToggleHologramLabel()) {
			hologramLabelButton = VanillaIconButton.create(x, applyRowY, contentWidth, buttonH, hologramLabelButtonLabel(), new ItemStack(Items.GLOW_INK_SAC), press -> toggleHologramLabel());
			attach(hologramLabelButton, true);
			applyRowY += buttonH + rowGap;
		}
		if (canEditHologramSettings()) {
			hologramSettingsButton = VanillaIconButton.create(
				x,
				applyRowY,
				contentWidth,
				buttonH,
				Component.translatable("screen.serverutilities.storage_menu.hologram_settings"),
				new ItemStack(Items.REPEATER),
				press -> toggleHologramSettingsPanel()
			);
			attach(hologramSettingsButton, true);
			applyRowY += buttonH + rowGap;
		}

		int actionWidth = columnWidth(contentWidth, 2, rowGap);
		attach(VanillaIconButton.create(x, applyRowY, actionWidth, buttonH, Component.translatable("screen.serverutilities.storage_menu.clear"), new ItemStack(Items.BARRIER), press -> clearMenu()), true);
		attach(VanillaIconButton.create(x + actionWidth + rowGap, applyRowY, actionWidth, buttonH, Component.translatable("screen.serverutilities.storage_menu.done"), new ItemStack(Items.LIME_DYE), press -> close()), true);

		reattachTitleStyleOverlay();
	}

	private boolean canEditHologramSettings() {
		return canToggleHologramLabel() && !viewContext.isNpcAnchored();
	}

	private void toggleHologramSettingsPanel() {
		hologramSettingsOpen = !hologramSettingsOpen;
		scheduleRebuild();
	}

	private Component seeThroughButtonLabel() {
		return Component.translatable(hologramSettings.seeThroughWalls()
			? "screen.serverutilities.hologram_options.see_through_on"
			: "screen.serverutilities.hologram_options.see_through_off");
	}

	private void buildHologramSettingsPanel(int editorX, int editorY) {
		int buttonH = UiLayoutHelper.defaultButtonHeight();
		int gap = ModPanelLayout.ROW_GAP;
		int panelHeight = StorageMenuHologramSettingsPanelWidget.CONTENT_TOP
			+ buttonH + gap
			+ buttonH + gap
			+ buttonH + gap
			+ buttonH
			+ ModPanelLayout.PANEL_PADDING;
		int panelX = layout.serverutilities$getLeftPos() + layout.serverutilities$getImageWidth() + ModPanelLayout.PANEL_HORIZONTAL_GAP;
		int panelY = layout.serverutilities$getTopPos();
		int x = panelX + ModPanelLayout.PANEL_PADDING;
		int y = panelY + StorageMenuHologramSettingsPanelWidget.CONTENT_TOP;
		int width = StorageMenuHologramSettingsPanelWidget.PANEL_WIDTH - ModPanelLayout.PANEL_PADDING * 2;

		attach(new StorageMenuHologramSettingsPanelWidget(panelX, panelY, panelHeight), false);
		attach(new HologramHeightSlider(
			x,
			y,
			width,
			Component.translatable("screen.serverutilities.storage_menu.hologram_height"),
			hologramSettings.heightOffset(),
			() -> hologramSettings.heightOffset(),
			value -> {
				hologramSettings = hologramSettings.withHeightOffset(value);
				autoApply();
			},
			() -> {
			}
		), true);
		y += buttonH + gap;

		attach(new FloatScaleSlider(
			x,
			y,
			width,
			Component.translatable("screen.serverutilities.storage_menu.hologram_size"),
			hologramSettings.scale(),
			() -> hologramSettings.scale(),
			value -> {
				hologramSettings = hologramSettings.withScale(value);
				autoApply();
			},
			() -> {
			}
		), true);
		y += buttonH + gap;

		attach(Button.builder(
			seeThroughButtonLabel(),
			press -> {
				hologramSettings = hologramSettings.withSeeThroughWalls(!hologramSettings.seeThroughWalls());
				press.setMessage(seeThroughButtonLabel());
				autoApply();
			}
		).bounds(x, y, width, buttonH).build(), true);
		y += buttonH + gap;

		attach(VanillaIconButton.create(
			x,
			y,
			width,
			buttonH,
			Component.translatable("screen.serverutilities.storage_menu.hologram_settings_close"),
			new ItemStack(Items.LIME_DYE),
			press -> {
				hologramSettingsOpen = false;
				scheduleRebuild();
			}
		), true);
	}

	private void buildShopWidgets(int panelX, int panelY, int pad, int contentWidth) {
		int columns = columnsForSize(containerSize);
		int rows = (containerSize + columns - 1) / columns;
		int gridHeight = rows * StorageMenuEditorMetrics.SLOT_STEP;
		int x = panelX + pad;
		int buttonH = StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT;
		int rowGap = StorageMenuEditorMetrics.FOOTER_ROW_GAP;

		attach(VanillaIconButton.create(
			x,
			panelY + StorageMenuEditorMetrics.CONTENT_TOP,
			contentWidth,
			buttonH,
			shopEnabledLabel(),
			new ItemStack(Items.EMERALD),
			press -> toggleShopEnabled()
		), true);

		int gridWidth = columns * StorageMenuEditorMetrics.SLOT_STEP;
		int gridX = x + Math.max(0, (contentWidth - gridWidth) / 2);
		int gridY = panelY + StorageMenuEditorMetrics.CONTENT_TOP + buttonH + StorageMenuEditorMetrics.FOOTER_ROW_GAP;
		for (int index = 0; index < containerSize; index++) {
			int col = index % columns;
			int row = index / columns;
			attach(new StorageMenuSlotButton(
				gridX + col * StorageMenuEditorMetrics.SLOT_STEP,
				gridY + row * StorageMenuEditorMetrics.SLOT_STEP,
				index,
				() -> selectedShopSlot,
				this::selectShopSlot,
				this::guiVisibleSlotConfig
			), true);
		}

		int footerY = gridY + gridHeight + StorageMenuEditorMetrics.SECTION_GAP;
		int nextY = footerY;
		if (isShopItemSlot(selectedShopSlot)) {
			nextY = attachShopItemFields(x, footerY, contentWidth, selectedShopSlot, true);
		} else {
			attachLabel(x, footerY, contentWidth, Component.translatable("screen.serverutilities.shop.select_shop_item_hint"));
			nextY = footerY + StorageMenuEditorMetrics.HINT_HEIGHT + LabeledFieldLayout.ROW_GAP;
		}

		int actionY = nextY;
		int halfWidth = columnWidth(contentWidth, 2, rowGap);
		Button useProductButton = VanillaIconButton.create(
			x,
			actionY,
			halfWidth,
			buttonH,
			Component.translatable("screen.serverutilities.shop.use_product"),
			new ItemStack(Items.CHEST),
			press -> useHeldShopProduct()
		);
		useProductButton.active = isShopItemSlot(selectedShopSlot);
		attach(useProductButton, true);
		Button shopNameButton = VanillaIconButton.create(
			x + halfWidth + rowGap,
			actionY,
			halfWidth,
			buttonH,
			Component.translatable("screen.serverutilities.storage_menu.name_item"),
			new ItemStack(Items.NAME_TAG),
			press -> toggleNameEditor()
		);
		shopNameButton.active = isShopItemSlot(selectedShopSlot) && canNameGuiVisibleSlot();
		nameButton = shopNameButton;
		attach(shopNameButton, true);

		int applyRowY = actionY + buttonH + StorageMenuEditorMetrics.FOOTER_SECTION_GAP;
		int actionWidth = columnWidth(contentWidth, 2, rowGap);
		attach(VanillaIconButton.create(
			x,
			applyRowY,
			actionWidth,
			buttonH,
			Component.translatable("screen.serverutilities.shop.clear"),
			new ItemStack(Items.BARRIER),
			press -> clearShop()
		), true);
		attach(VanillaIconButton.create(
			x + actionWidth + rowGap,
			applyRowY,
			actionWidth,
			buttonH,
			Component.translatable("screen.serverutilities.storage_menu.done"),
			new ItemStack(Items.LIME_DYE),
			press -> close()
		), true);
	}

	private void attachInventoryTree() {
		List<StorageMenuInventoryTree.Node> treeNodes = buildTreeNodes();
		int treeHeight = StorageMenuInventoryTreeWidget.computeHeight(treeNodes);
		int[] defaultTreePos = StorageMenuPanelPositions.belowContainer(layout, parent, treeHeight);
		treeDragGroup = new DraggablePanelGroup(parent, "inventory_tree");
		int[] treePos = treeDragGroup.resolvePosition(
			defaultTreePos[0],
			defaultTreePos[1],
			StorageMenuInventoryTreeWidget.PANEL_WIDTH,
			treeHeight
		);
		int treeX = treePos[0];
		int treeY = treePos[1];

		activeDragGroup = treeDragGroup;
		attach(new StorageMenuInventoryTreeWidget(treeX, treeY, treeHeight, treeNodes), false);

		int innerX = treeX + StorageMenuInventoryTreeWidget.OUTER_PADDING;
		int innerY = treeY + StorageMenuInventoryTreeWidget.TITLE_HEIGHT + StorageMenuInventoryTreeWidget.OUTER_PADDING;
		for (StorageMenuInventoryTree.Node node : treeNodes) {
			int nodeX = innerX + node.layoutX;
			int nodeY = innerY + node.layoutY;
			boolean selected = node.context.equals(viewContext);
			attach(new StorageMenuTreeNodeWidget(
				nodeX,
				nodeY,
				node.number,
				selected,
				() -> {
					if (!selected) {
						switchToInventory(node.context);
					}
				}
			), true);
		}
		activeDragGroup = null;
		attachDraggableTitle(
			treeDragGroup,
			Component.translatable("screen.serverutilities.storage_menu.inventory_tree"),
			StorageMenuInventoryTreeWidget.PANEL_WIDTH,
			StorageMenuInventoryTreeWidget.TITLE_HEIGHT + StorageMenuInventoryTreeWidget.OUTER_PADDING
		);
	}

	private void attachLabel(int x, int y, int width, Component text) {
		Font font = Minecraft.getInstance().font;
		StringWidget label = new StringWidget(x, y, width, LabeledFieldLayout.LABEL_HEIGHT, text, font);
		attach(label, false);
	}

	private int attachLabeledField(int x, int labelY, Component label, java.util.function.Consumer<EditBox> configure) {
		Font font = Minecraft.getInstance().font;
		attachLabel(x, labelY, StorageMenuEditorMetrics.CONTENT_WIDTH, label);
		int fieldY = labelY + LabeledFieldLayout.LABEL_HEIGHT + LabeledFieldLayout.LABEL_GAP;
		EditBox field = createField(font, x, fieldY, StorageMenuEditorMetrics.CONTENT_WIDTH, label);
		configure.accept(field);
		attach(field, true);
		return fieldY + LabeledFieldLayout.FIELD_HEIGHT + LabeledFieldLayout.ROW_GAP;
	}

	private static EditBox createField(Font font, int x, int y, int width, Component narration) {
		return new EditBox(font, x, y, width, LabeledFieldLayout.FIELD_HEIGHT, narration);
	}

	private void attach(GuiEventListener widget, boolean interactive) {
		if (interactive) {
			if (widget instanceof AbstractWidget abstractWidget) {
				screenInvoker.serverutilities$addRenderableWidget(abstractWidget);
			} else if (widget instanceof Button button) {
				screenInvoker.serverutilities$addRenderableWidget(button);
			} else {
				screenInvoker.serverutilities$addRenderableWidget((AbstractWidget) widget);
			}
		} else if (widget instanceof AbstractWidget abstractWidget) {
			screenInvoker.serverutilities$addRenderableOnly(abstractWidget);
		}
		widgets.add(widget);
		if (widget instanceof AbstractWidget abstractWidget) {
			ModUiRenderContext.markIfInteractive(abstractWidget);
		}
		if (activeDragGroup != null && widget instanceof AbstractWidget abstractWidget && !(widget instanceof DraggableTitleBarWidget)) {
			activeDragGroup.track(abstractWidget);
		}
	}

	private void attachDraggableTitle(DraggablePanelGroup group, Component title, int width, int height) {
		if (group == null) {
			return;
		}
		DraggableTitleBarWidget titleBar = group.createTitleBar(title, width, height);
		attach(titleBar, true);
	}

	private void tearDownWidgets() {
		for (GuiEventListener widget : widgets) {
			screenInvoker.serverutilities$removeWidget(widget);
		}
		widgets.clear();
		titleField = null;
		itemField = null;
		commandField = null;
		shopProductField = null;
		shopProductAmountField = null;
		shopStockField = null;
		costAmountField = null;
		costItemField = null;
		typeButton = null;
		nameButton = null;
		invulnerableButton = null;
		hologramLabelButton = null;
		hologramSettingsButton = null;
		if (menuTabButton != null) {
			ModUiSelectionState.unmarkSelected(menuTabButton);
		}
		if (shopTabButton != null) {
			ModUiSelectionState.unmarkSelected(shopTabButton);
		}
		menuTabButton = null;
		shopTabButton = null;
	}

	private void refreshFooterForSelection() {
		StorageMenuSlotConfig selected = slotConfig(selectedSlot);
		if (typeButton != null) {
			typeButton.setMessage(typeLabel(selected.type()));
		}
		if (itemField != null && !itemField.isFocused()) {
			setFieldValueSilently(itemField, itemId(selected.displayStack()));
		}
		if (commandField != null && !commandField.isFocused()) {
			setFieldValueSilently(commandField, selected.command());
		}
		if (nameButton != null) {
			nameButton.active = canNameSelectedSlot();
		}
	}

	private boolean usesLinkFooterLayout(StorageMenuSlotType type) {
		return type == StorageMenuSlotType.LINK;
	}

	private boolean usesShopFooterLayout(StorageMenuSlotType type) {
		return type == StorageMenuSlotType.SHOP_ITEM;
	}

	private boolean isShopItemSlot(int index) {
		return slotConfig(index).type() == StorageMenuSlotType.SHOP_ITEM;
	}

	private void selectSlot(int index) {
		if (reservesChromeIndex(index)) {
			return;
		}
		StorageMenuSlotType previousType = slotConfig(selectedSlot).type();
		selectedSlot = index;
		StorageMenuSlotConfig selected = slotConfig(selectedSlot);
		if (usesLinkFooterLayout(previousType) != usesLinkFooterLayout(selected.type())
			|| usesShopFooterLayout(previousType) != usesShopFooterLayout(selected.type())) {
			scheduleRebuild();
		} else {
			refreshFooterForSelection();
		}
	}

	private void cycleType() {
		StorageMenuSlotConfig selected = slotConfig(selectedSlot);
		StorageMenuSlotType next = switch (selected.type()) {
			case EMPTY -> StorageMenuSlotType.FILLER;
			case FILLER -> StorageMenuSlotType.COMMAND;
			case COMMAND -> StorageMenuSlotType.LINK;
			case LINK -> StorageMenuSlotType.SHOP_ITEM;
			case SHOP_ITEM -> StorageMenuSlotType.EMPTY;
			case BACK, CLOSE -> StorageMenuSlotType.EMPTY;
		};
		if (selected.type() == StorageMenuSlotType.SHOP_ITEM && next != StorageMenuSlotType.SHOP_ITEM) {
			shopListings.remove(selectedSlot);
		}
		StorageMenuSlotConfig updated = selected.withType(next);
		if (next == StorageMenuSlotType.FILLER && updated.displayStack().isEmpty()) {
			updated = updated.withDisplayStack(StorageMenuFillerItems.defaultFiller());
		}
		if (next == StorageMenuSlotType.COMMAND && updated.displayStack().isEmpty()) {
			updated = updated.withDisplayStack(StorageMenuFillerItems.defaultFiller());
		}
		if (next == StorageMenuSlotType.LINK && updated.displayStack().isEmpty()) {
			updated = updated.withDisplayStack(StorageMenuFillerItems.defaultFiller());
		}
		if (next == StorageMenuSlotType.LINK && !updated.hasSubMenu()) {
			updated = updated.withSubMenuId(StorageSubMenuManager.createId());
		}
		if (next == StorageMenuSlotType.EMPTY) {
			updated = StorageMenuSlotConfig.empty(selectedSlot);
		}
		if (next == StorageMenuSlotType.SHOP_ITEM) {
			shopListings.putIfAbsent(selectedSlot, ShopListing.empty(selectedSlot));
		}
		updateSelectedSlot(updated);
		scheduleRebuild();
	}

	private void openInsertItemPicker() {
		ensureItemPickerOverlay();
		itemPickerOverlay.open(
			Component.translatable("screen.serverutilities.storage_menu.select_item"),
			stack -> {
				updateSelectedSlot(slotConfig(selectedSlot).withDisplayStack(stack.copyWithCount(1)));
				if (itemField != null && !itemField.isFocused()) {
					setFieldValueSilently(itemField, itemId(stack));
				}
				scheduleRebuild();
			}
		);
	}

	private boolean canNameSelectedSlot() {
		StorageMenuSlotConfig selected = slotConfig(selectedSlot);
		return selected.type() == StorageMenuSlotType.COMMAND || canNameStack(selected.displayStack());
	}

	private boolean canNameGuiVisibleSlot() {
		return canNameStack(guiVisibleSlotConfig(selectedShopSlot).displayStack());
	}

	private static boolean canNameStack(ItemStack stack) {
		return !stack.isEmpty();
	}

	private void toggleNameEditor() {
		if (activeTab == StorageMenuEditorTab.SHOP ? !canNameGuiVisibleSlot() : !canNameSelectedSlot()) {
			return;
		}
		ensureNameOverlay();
		nameOverlay.toggle();
	}

	private int attachTitleRow(int x, int labelY, int contentWidth) {
		attachLabel(x, labelY, contentWidth, Component.translatable("screen.serverutilities.storage_menu.container_title"));
		int fieldY = labelY + LabeledFieldLayout.LABEL_HEIGHT + LabeledFieldLayout.LABEL_GAP;
		int styleWidth = UiLayoutHelper.buttonWidth(Minecraft.getInstance().font, Component.translatable("screen.serverutilities.hologram_options.style"));
		int gap = UiScale.s(4);
		int fieldWidth = contentWidth - styleWidth - gap;

		Font font = Minecraft.getInstance().font;
		titleField = new EditBox(font, x, fieldY, fieldWidth, LabeledFieldLayout.FIELD_HEIGHT, Component.translatable("screen.serverutilities.storage_menu.container_title"));
		titleField.setMaxLength(48);
		titleField.setValue(TextFormats.parse(menuTitle).text());
		titleField.setHint(Component.translatable("screen.serverutilities.storage_menu.container_title_hint"));
		titleField.addFormatter((visible, start) -> TextFormats.editBoxFormat(menuTitle, visible, start));
		titleField.setResponder(value -> {
			menuTitle = TextFormats.parse(menuTitle).withText(value).serialize();
			autoApply();
		});
		attach(titleField, true);

		attach(Button.builder(Component.translatable("screen.serverutilities.hologram_options.style"), press -> toggleTitleStyle())
			.bounds(x + fieldWidth + gap, fieldY, styleWidth, LabeledFieldLayout.FIELD_HEIGHT)
			.build(), true);

		return fieldY + LabeledFieldLayout.FIELD_HEIGHT + LabeledFieldLayout.ROW_GAP;
	}

	private void toggleTitleStyle() {
		if (titleField == null) {
			return;
		}
		ensureTitleStyleOverlay();
		menuTitle = TextFormats.parse(menuTitle).withText(titleField.getValue()).serialize();
		int[] position = TextStylePanelPositions.besideContainer(layout, parent);
		titleStyleOverlay.toggle(menuTitle, position[0], position[1]);
	}

	private void ensureTitleStyleOverlay() {
		if (titleStyleOverlay != null || titleField == null) {
			return;
		}
		titleStyleOverlay = new TextStyleOverlay(
			parent,
			() -> titleField.getValue(),
			serialized -> {
				menuTitle = TextFormats.normalize(serialized);
				String plain = TextFormats.parse(menuTitle).text();
				if (!plain.equals(titleField.getValue())) {
					titleField.setValue(plain);
				}
				autoApply();
			},
			() -> TextStylePanelPositions.besideContainer(layout, parent)
		);
	}

	private void recreateTitleStyleOverlay() {
		boolean wasOpen = titleStyleOverlay != null && titleStyleOverlay.isOpen();
		StyledText savedDraft = wasOpen ? titleStyleOverlay.getDraft() : null;
		disposeTitleStyleOverlay();
		if (titleField == null) {
			return;
		}
		titleStyleOverlay = new TextStyleOverlay(
			parent,
			() -> titleField.getValue(),
			serialized -> {
				menuTitle = TextFormats.normalize(serialized);
				String plain = TextFormats.parse(serialized).text();
				if (!plain.equals(titleField.getValue())) {
					titleField.setValue(plain);
				}
				autoApply();
			},
			() -> TextStylePanelPositions.besideContainer(layout, parent)
		);
		if (wasOpen && savedDraft != null) {
			titleStyleOverlay.openWithDraft(savedDraft.withText(titleField.getValue()));
		}
	}

	private void disposeTitleStyleOverlay() {
		if (titleStyleOverlay != null) {
			titleStyleOverlay.dispose();
			titleStyleOverlay = null;
		}
	}

	private void reattachTitleStyleOverlay() {
		recreateTitleStyleOverlay();
	}

	private void ensureNameOverlay() {
		if (nameOverlay == null) {
			nameOverlay = new StorageMenuNameOverlay(
				parent,
				this::nameEditorStack,
				stack -> {
					if (activeTab == StorageMenuEditorTab.SHOP) {
						applyShopProduct(selectedShopSlot, stack);
						refreshShopFooter();
					} else if (isShopItemSlot(selectedSlot)) {
						applyShopProduct(selectedSlot, stack);
						refreshFooterForSelection();
					} else {
						updateSelectedSlot(slotConfig(selectedSlot).withDisplayStack(stack));
						refreshFooterForSelection();
					}
				}
			);
		}
	}

	private ItemStack nameEditorStack() {
		if (activeTab == StorageMenuEditorTab.SHOP) {
			return guiVisibleSlotConfig(selectedShopSlot).displayStack();
		}
		StorageMenuSlotConfig selected = slotConfig(selectedSlot);
		if (selected.type() == StorageMenuSlotType.COMMAND && selected.displayStack().isEmpty()) {
			return StorageMenuFillerItems.defaultFiller();
		}
		return selected.displayStack();
	}

	private void disposeNameOverlay() {
		if (nameOverlay != null) {
			nameOverlay.dispose();
			nameOverlay = null;
		}
	}

	private void ensureItemPickerOverlay() {
		if (itemPickerOverlay == null) {
			itemPickerOverlay = new ShopItemPickerOverlay(parent, layout);
		} else {
			itemPickerOverlay.updateLayout(layout);
		}
	}

	private void disposeItemPickerOverlay() {
		if (itemPickerOverlay != null) {
			itemPickerOverlay.close();
			itemPickerOverlay = null;
		}
	}

	private void fillFiller() {
		ItemStack filler = StorageMenuFillerItems.defaultFiller();
		boolean changed = false;
		for (int index = 0; index < containerSize; index++) {
			if (reservesChromeIndex(index)) {
				continue;
			}
			StorageMenuSlotConfig config = slotConfig(index);
			if (config.isProtectedFromFill()) {
				continue;
			}
			draftSlots.put(index, new StorageMenuSlotConfig(index, StorageMenuSlotType.FILLER, filler.copy(), "", StorageMenuSlotConfig.NO_SUB_MENU));
			changed = true;
		}
		if (!changed) {
			return;
		}
		autoApply();
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			client.player.sendOverlayMessage(Component.translatable("screen.serverutilities.storage_menu.filled"));
		}
	}

	private void updateSelectedSlot(StorageMenuSlotConfig config) {
		if (config.type() == StorageMenuSlotType.SHOP_ITEM
			&& !config.displayStack().isEmpty()
			&& ShopItemRules.isBlockedShopProduct(config.displayStack())) {
			Minecraft client = Minecraft.getInstance();
			if (client.player != null) {
				client.player.sendOverlayMessage(Component.translatable("screen.serverutilities.shop.container_blocked"));
			}
			return;
		}
		if (config.isTrivial()) {
			draftSlots.remove(config.index());
		} else {
			draftSlots.put(config.index(), config);
		}
		if (config.type() == StorageMenuSlotType.SHOP_ITEM && !config.displayStack().isEmpty()) {
			ShopListing listing = shopListing(config.index()).withProduct(config.displayStack().copy());
			updateShopListing(listing);
		} else {
			autoApply();
		}
	}

	private void apply() {
		flushTitleStyleDraft();
		List<StorageMenuSlotConfig> slots = new ArrayList<>();
		for (StorageMenuSlotConfig config : draftSlots.values()) {
			if (reservesChromeIndex(config.index())) {
				continue;
			}
			if (!config.isPersistable()) {
				continue;
			}
			slots.add(config);
		}
		StorageMenuDefinition definition = StorageMenuDefinition.empty(containerSize)
			.withEnabled(true)
			.withTitle(menuTitle)
			.withSlots(slots);
		ClientPlayNetworking.send(new ModPackets.StorageMenuSavePayload(
			buildMenuData(definition)
		));
		cacheCurrentDraft();
	}

	private void autoApply() {
		if (!open || awaitingSync || autoApplying) {
			return;
		}
		autoApplying = true;
		try {
			apply();
		} finally {
			autoApplying = false;
		}
	}

	private StorageMenuNetwork.MenuData buildMenuData(StorageMenuDefinition definition) {
		ShopDefinition shop = buildShopDefinition();
		return StorageMenuNetwork.MenuData.fromDefinition(viewContext, definition, invulnerable, hologramLabel, hologramSettings, shop);
	}

	private ShopDefinition buildShopDefinition() {
		if (!canEditShop()) {
			return ShopDefinition.EMPTY;
		}
		Map<Integer, ShopListing> listings = new HashMap<>();
		for (int index = 0; index < containerSize; index++) {
			if (!isShopItemSlot(index)) {
				continue;
			}
			ShopListing listing = shopListing(index);
			ItemStack product = slotConfig(index).displayStack();
			if (!product.isEmpty() && !ShopItemRules.isBlockedShopProduct(product)) {
				listing = listing.withProduct(product.copy());
			}
			if (listing.isConfigured()) {
				listings.put(index, ShopDescriptions.withProductCostDescription(listing));
			}
		}
		boolean enabled = shopEnabled && !listings.isEmpty();
		if (!listings.isEmpty()) {
			enabled = true;
		}
		return new ShopDefinition(enabled, listings);
	}

	private void clearShop() {
		shopEnabled = false;
		shopListings.clear();
		for (int index = 0; index < containerSize; index++) {
			StorageMenuSlotConfig config = slotConfig(index);
			if (config.type() == StorageMenuSlotType.SHOP_ITEM) {
				draftSlots.remove(index);
			}
		}
		autoApply();
		scheduleRebuild();
	}

	private Component shopEnabledLabel() {
		return shopEnabled
			? Component.translatable("screen.serverutilities.shop.enabled_on")
			: Component.translatable("screen.serverutilities.shop.enabled_off");
	}

	private void toggleShopEnabled() {
		shopEnabled = !shopEnabled;
		autoApply();
		scheduleRebuild();
	}

	private ShopListing shopListing(int index) {
		return shopListings.getOrDefault(index, ShopListing.empty(index));
	}

	private StorageMenuSlotConfig guiVisibleSlotConfig(int index) {
		StorageMenuSlotConfig config = slotConfig(index);
		if (config.type() == StorageMenuSlotType.SHOP_ITEM) {
			ShopListing listing = shopListing(index);
			if (listing.isConfigured()) {
				return config.withDisplayStack(listing.product());
			}
		}
		return config;
	}

	private void applyShopProduct(int slotIndex, ItemStack stack) {
		int amount = stack.isEmpty() ? 1 : Math.max(1, stack.getCount());
		if (shopProductAmountField != null && !shopProductAmountField.getValue().isBlank()) {
			amount = parseAmount(shopProductAmountField.getValue());
		} else if (amount <= 1) {
			amount = productAmount(slotIndex);
		}
		applyShopProduct(slotIndex, stack, amount);
	}

	private void applyShopProduct(int slotIndex, ItemStack stack, int amount) {
		if (!stack.isEmpty() && ShopItemRules.isBlockedShopProduct(stack)) {
			Minecraft client = Minecraft.getInstance();
			if (client.player != null) {
				client.player.sendOverlayMessage(Component.translatable("screen.serverutilities.shop.container_blocked"));
			}
			return;
		}
		if (stack.isEmpty()) {
			updateSelectedSlot(new StorageMenuSlotConfig(slotIndex, StorageMenuSlotType.SHOP_ITEM, ItemStack.EMPTY, "", StorageMenuSlotConfig.NO_SUB_MENU));
			updateShopListing(ShopListing.empty(slotIndex));
			return;
		}
		int clamped = Math.max(1, Math.min(64, amount));
		updateSelectedSlot(new StorageMenuSlotConfig(slotIndex, StorageMenuSlotType.SHOP_ITEM, stack.copyWithCount(clamped), "", StorageMenuSlotConfig.NO_SUB_MENU));
	}

	private void flushTitleStyleDraft() {
		if (titleStyleOverlay != null && titleStyleOverlay.isOpen()) {
			menuTitle = TextFormats.normalize(titleStyleOverlay.getDraft().serialize());
			if (titleField != null) {
				titleField.setValue(TextFormats.parse(menuTitle).text());
			}
		} else if (titleField != null) {
			menuTitle = TextFormats.parse(menuTitle).withText(titleField.getValue()).serialize();
		}
	}

	private void selectShopSlot(int index) {
		if (reservesChromeIndex(index)) {
			return;
		}
		selectedShopSlot = index;
		selectedSlot = index;
		scheduleRebuild();
	}

	private void refreshShopFooter() {
		if (!isShopItemSlot(selectedShopSlot)) {
			return;
		}
		StorageMenuSlotConfig visible = guiVisibleSlotConfig(selectedShopSlot);
		if (shopProductField != null && !shopProductField.isFocused()) {
			setFieldValueSilently(shopProductField, itemId(visible.displayStack()));
		}
		if (shopProductAmountField != null && !shopProductAmountField.isFocused()) {
			setFieldValueSilently(shopProductAmountField, Integer.toString(productAmount(selectedShopSlot)));
		}
		ShopListing selected = shopListing(selectedShopSlot);
		if (shopStockField != null && !shopStockField.isFocused()) {
			setFieldValueSilently(shopStockField, formatStock(selected.stock()));
		}
		refreshCostFields(selected, false);
		if (nameButton != null && activeTab == StorageMenuEditorTab.SHOP) {
			nameButton.active = canNameGuiVisibleSlot();
		}
	}

	private void refreshCostFields(ShopListing listing, boolean force) {
		if (costAmountField != null && (force || !costAmountField.isFocused())) {
			setFieldValueSilently(costAmountField, Integer.toString(Math.max(1, listing.cost().getCount())));
		}
		if (costItemField != null && (force || !costItemField.isFocused())) {
			setFieldValueSilently(costItemField, costItemDisplay(listing));
		}
	}

	private void setFieldValueSilently(EditBox field, String value) {
		updatingFields = true;
		try {
			field.setValue(value == null ? "" : value);
		} finally {
			updatingFields = false;
		}
	}

	private void updateShopListing(ShopListing listing) {
		ShopListing described = ShopDescriptions.withProductCostDescription(listing);
		if (described.isConfigured() || hasEditorDraft(described)) {
			shopListings.put(described.slotIndex(), described);
			StorageMenuSlotConfig config = slotConfig(described.slotIndex());
			if (config.type() == StorageMenuSlotType.SHOP_ITEM && !described.product().isEmpty()) {
				draftSlots.put(config.index(), config.withDisplayStack(described.product().copy()));
			}
		} else {
			shopListings.remove(described.slotIndex());
		}
		refreshShopFooter();
		autoApply();
	}

	private static boolean hasEditorDraft(ShopListing listing) {
		return !listing.product().isEmpty() || !listing.cost().isEmpty();
	}

	private void useHeldShopProduct() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || !isShopItemSlot(selectedShopSlot)) {
			return;
		}
		ItemStack held = client.player.getMainHandItem();
		if (held.isEmpty()) {
			return;
		}
		applyShopProduct(selectedShopSlot, held.copy(), Math.max(1, held.getCount()));
		if (shopProductField != null && !shopProductField.isFocused()) {
			setFieldValueSilently(shopProductField, itemId(held));
		}
		if (shopProductAmountField != null && !shopProductAmountField.isFocused()) {
			setFieldValueSilently(shopProductAmountField, Integer.toString(Math.max(1, held.getCount())));
		}
	}

	private int attachShopItemFields(int x, int y, int contentWidth, int slotIndex, boolean includePurchaseFields) {
		int nextY = attachLabeledField(x, y, Component.translatable("screen.serverutilities.shop.product"), field -> {
			shopProductField = field;
			field.setMaxLength(128);
			field.setValue(itemId(slotConfig(slotIndex).displayStack()));
			field.setResponder(value -> {
				if (!updatingFields) {
					applyShopProduct(slotIndex, parseItem(value));
				}
			});
		});

		if (includePurchaseFields) {
			nextY = attachShopProductAmountFields(x, nextY, contentWidth, slotIndex);
			nextY = attachShopCostFields(x, nextY, contentWidth, slotIndex);
		}

		return attachLabeledField(x, nextY, Component.translatable("screen.serverutilities.shop.stock"), field -> {
			shopStockField = field;
			field.setMaxLength(6);
			field.setValue(formatStock(shopListing(slotIndex).stock()));
			field.setResponder(value -> {
				if (!updatingFields) {
					updateShopListing(shopListing(slotIndex).withStock(parseStock(value)));
				}
			});
		});
	}

	private int attachShopProductAmountFields(int x, int y, int contentWidth, int slotIndex) {
		attachLabel(x, y, contentWidth, Component.translatable("screen.serverutilities.shop.product_amount"));
		int rowY = y + LabeledFieldLayout.LABEL_HEIGHT + LabeledFieldLayout.LABEL_GAP;
		int buttonH = StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT;
		int stepWidth = UiScale.s(20);
		int amountWidth = UiScale.s(36);
		int gap = UiScale.s(4);

		attach(Button.builder(Component.literal("-"), press -> adjustProductAmount(slotIndex, -1))
			.bounds(x, rowY, stepWidth, buttonH).build(), true);

		Font font = Minecraft.getInstance().font;
		int amountX = x + stepWidth + gap;
		shopProductAmountField = createField(font, amountX, rowY, amountWidth, Component.translatable("screen.serverutilities.shop.product_amount"));
		shopProductAmountField.setMaxLength(3);
		shopProductAmountField.setValue(Integer.toString(productAmount(slotIndex)));
		shopProductAmountField.setResponder(value -> {
			if (!updatingFields) {
				setProductAmount(slotIndex, parseAmount(value));
			}
		});
		attach(shopProductAmountField, true);

		attach(Button.builder(Component.literal("+"), press -> adjustProductAmount(slotIndex, 1))
			.bounds(amountX + amountWidth + gap, rowY, stepWidth, buttonH).build(), true);

		return rowY + buttonH + LabeledFieldLayout.ROW_GAP;
	}

	private int attachShopCostFields(int x, int y, int contentWidth, int slotIndex) {
		attachLabel(x, y, contentWidth, Component.translatable("screen.serverutilities.shop.cost"));
		int costRowY = y + LabeledFieldLayout.LABEL_HEIGHT + LabeledFieldLayout.LABEL_GAP;
		int buttonH = StorageMenuEditorMetrics.FOOTER_BUTTON_HEIGHT;
		int stepWidth = UiScale.s(20);
		int amountWidth = UiScale.s(36);
		int gap = UiScale.s(4);
		int pickerWidth = contentWidth - stepWidth * 2 - amountWidth - gap * 3;

		attach(Button.builder(Component.translatable("screen.serverutilities.shop.select_cost_item"), press -> openCostItemPicker(slotIndex))
			.bounds(x, costRowY, pickerWidth, buttonH).build(), true);
		attach(Button.builder(Component.literal("-"), press -> adjustCostAmount(slotIndex, -1))
			.bounds(x + pickerWidth + gap, costRowY, stepWidth, buttonH).build(), true);

		Font font = Minecraft.getInstance().font;
		int amountX = x + pickerWidth + gap + stepWidth + gap;
		costAmountField = createField(font, amountX, costRowY, amountWidth, Component.translatable("screen.serverutilities.shop.cost_amount"));
		costAmountField.setMaxLength(3);
		costAmountField.setValue(Integer.toString(Math.max(1, shopListing(slotIndex).cost().getCount())));
		costAmountField.setResponder(value -> {
			if (!updatingFields) {
				setCostAmount(slotIndex, parseAmount(value));
			}
		});
		attach(costAmountField, true);

		attach(Button.builder(Component.literal("+"), press -> adjustCostAmount(slotIndex, 1))
			.bounds(amountX + amountWidth + gap, costRowY, stepWidth, buttonH).build(), true);

		int costDisplayY = costRowY + buttonH + LabeledFieldLayout.LABEL_GAP;
		costItemField = createField(font, x, costDisplayY, contentWidth, Component.translatable("screen.serverutilities.shop.cost_item"));
		costItemField.setMaxLength(128);
		costItemField.setValue(costItemDisplay(shopListing(slotIndex)));
		costItemField.setHint(Component.translatable("screen.serverutilities.shop.cost_item_hint"));
		attach(costItemField, true);

		return costDisplayY + LabeledFieldLayout.FIELD_HEIGHT + LabeledFieldLayout.ROW_GAP;
	}

	private static String costItemDisplay(ShopListing listing) {
		ItemStack cost = listing.cost();
		return cost.isEmpty() ? "" : itemId(cost);
	}

	private void openCostItemPicker(int slotIndex) {
		ensureItemPickerOverlay();
		itemPickerOverlay.open(
			Component.translatable("screen.serverutilities.shop.select_cost_item"),
			stack -> {
				int amount = Math.max(1, shopListing(slotIndex).cost().getCount());
				if (costAmountField != null && !costAmountField.getValue().isBlank()) {
					amount = parseAmount(costAmountField.getValue());
				}
				ShopListing current = shopListing(slotIndex);
				ItemStack product = current.product().isEmpty()
					? slotConfig(slotIndex).displayStack().copy()
					: current.product().copy();
				ItemStack cost = stack.copyWithCount(amount);
				ShopListing updated = current.withCost(cost);
				if (!product.isEmpty()) {
					updated = updated.withProduct(product);
				}
				updateShopListing(updated);
				refreshCostFields(updated, true);
			}
		);
	}

	private void adjustCostAmount(int slotIndex, int delta) {
		ShopListing listing = shopListing(slotIndex);
		ItemStack cost = listing.cost();
		int current = cost.isEmpty() && costAmountField != null
			? parseAmount(costAmountField.getValue())
			: Math.max(1, cost.getCount());
		int amount = Math.max(1, Math.min(64, current + delta));
		if (!cost.isEmpty()) {
			ShopListing updated = listing.withCost(cost.copyWithCount(amount));
			updateShopListing(updated);
			refreshCostFields(updated, true);
		} else if (costAmountField != null) {
			setFieldValueSilently(costAmountField, Integer.toString(amount));
		}
	}

	private void adjustProductAmount(int slotIndex, int delta) {
		int current = productAmount(slotIndex);
		if (shopProductAmountField != null && !shopProductAmountField.getValue().isBlank()) {
			current = parseAmount(shopProductAmountField.getValue());
		}
		int amount = Math.max(1, Math.min(64, current + delta));
		setProductAmount(slotIndex, amount);
		if (shopProductAmountField != null) {
			setFieldValueSilently(shopProductAmountField, Integer.toString(amount));
		}
	}

	private void setProductAmount(int slotIndex, int amount) {
		ItemStack product = productStack(slotIndex);
		int clamped = Math.max(1, Math.min(64, amount));
		if (product.isEmpty()) {
			if (shopProductAmountField != null) {
				setFieldValueSilently(shopProductAmountField, Integer.toString(clamped));
			}
			return;
		}
		ShopListing updated = shopListing(slotIndex).withProduct(product.copyWithCount(clamped));
		updateShopListing(updated);
		if (shopProductAmountField != null && !shopProductAmountField.isFocused()) {
			setFieldValueSilently(shopProductAmountField, Integer.toString(clamped));
		}
	}

	private int productAmount(int slotIndex) {
		ItemStack product = productStack(slotIndex);
		return product.isEmpty() ? 1 : Math.max(1, Math.min(64, product.getCount()));
	}

	private ItemStack productStack(int slotIndex) {
		ItemStack product = shopListing(slotIndex).product();
		if (product.isEmpty()) {
			product = slotConfig(slotIndex).displayStack();
		}
		return product;
	}

	private void setCostAmount(int slotIndex, int amount) {
		ShopListing listing = shopListing(slotIndex);
		ItemStack cost = listing.cost();
		if (cost.isEmpty()) {
			return;
		}
		int clamped = Math.max(1, Math.min(64, amount));
		ShopListing updated = listing.withCost(cost.copyWithCount(clamped));
		updateShopListing(updated);
		refreshCostFields(updated, false);
	}

	private static int parseAmount(String value) {
		if (value == null || value.isBlank()) {
			return 1;
		}
		try {
			return Math.max(1, Math.min(64, Integer.parseInt(value.trim())));
		} catch (NumberFormatException ignored) {
			return 1;
		}
	}

	private static String formatStock(int stock) {
		return stock == ShopListing.UNLIMITED_STOCK ? "" : Integer.toString(stock);
	}

	private static int parseStock(String value) {
		if (value == null || value.isBlank()) {
			return ShopListing.UNLIMITED_STOCK;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ignored) {
			return ShopListing.UNLIMITED_STOCK;
		}
	}

	private boolean canResizeChest() {
		return viewContext.isRoot() && StorageMenuSizes.isChestSize(containerSize);
	}

	private boolean canToggleInvulnerable() {
		return viewContext.isRoot();
	}

	private Component chestSizeLabel() {
		return containerSize == StorageMenuSizes.DOUBLE_CHEST
			? Component.translatable("screen.serverutilities.storage_menu.size.double")
			: Component.translatable("screen.serverutilities.storage_menu.size.single");
	}

	private Component invulnerableLabel() {
		return invulnerable
			? Component.translatable("screen.serverutilities.storage_menu.invulnerable_on")
			: Component.translatable("screen.serverutilities.storage_menu.invulnerable_off");
	}

	private void toggleChestSize() {
		int newSize = StorageMenuSizes.toggleChestSize(containerSize);
		draftSlots.entrySet().removeIf(entry -> entry.getKey() >= newSize);
		containerSize = newSize;
		selectedSlot = Math.min(selectedSlot, Math.max(0, containerSize - 1));
		autoApply();
		scheduleRebuild();
	}

	private boolean canToggleHologramLabel() {
		return viewContext.isRoot();
	}

	private Component hologramLabelButtonLabel() {
		return hologramLabel
			? Component.translatable("screen.serverutilities.storage_menu.hologram_label_on")
			: Component.translatable("screen.serverutilities.storage_menu.hologram_label_off");
	}

	private void refreshHologramLabelButton() {
		if (hologramLabelButton != null) {
			hologramLabelButton.setMessage(hologramLabelButtonLabel());
		}
	}

	private void toggleHologramLabel() {
		if (!hologramLabel && (menuTitle == null || menuTitle.isBlank())) {
			Minecraft client = Minecraft.getInstance();
			if (client.player != null) {
				client.player.sendOverlayMessage(Component.translatable("screen.serverutilities.storage_menu.hologram_label_needs_title"));
			}
			return;
		}
		hologramLabel = !hologramLabel;
		refreshHologramLabelButton();
		autoApply();
	}

	private void toggleInvulnerable() {
		invulnerable = !invulnerable;
		if (invulnerableButton != null) {
			invulnerableButton.setMessage(invulnerableLabel());
		}
		autoApply();
	}

	private void clearMenu() {
		draftSlots.clear();
		menuTitle = "";
		draftCache.remove(StorageMenuInventoryTree.draftKey(viewContext));
		ClientPlayNetworking.send(new ModPackets.StorageMenuClearPayload(
			viewContext.anchorPos(),
			viewContext.subMenuId(),
			viewContext.npcEntityId()
		));
		rebuild();
	}

	private StorageMenuSlotConfig slotConfig(int index) {
		return draftSlots.getOrDefault(index, StorageMenuSlotConfig.empty(index));
	}

	private static int columnsForSize(int size) {
		if (size == net.minecraft.world.inventory.HopperMenu.CONTAINER_SIZE) {
			return 5;
		}
		if (size == 9) {
			return 3;
		}
		return 9;
	}

	private static Component typeLabel(StorageMenuSlotType type) {
		return switch (type) {
			case EMPTY -> Component.translatable("screen.serverutilities.storage_menu.type.empty");
			case FILLER -> Component.translatable("screen.serverutilities.storage_menu.type.filler");
			case COMMAND -> Component.translatable("screen.serverutilities.storage_menu.type.command");
			case LINK -> Component.translatable("screen.serverutilities.storage_menu.type.link");
			case SHOP_ITEM -> Component.translatable("screen.serverutilities.storage_menu.type.shop_item");
			case BACK -> Component.translatable("screen.serverutilities.storage_menu.type.back");
			case CLOSE -> Component.translatable("screen.serverutilities.storage_menu.type.close");
		};
	}

	private static String itemId(ItemStack stack) {
		if (stack.isEmpty()) {
			return "";
		}
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private static ItemStack parseItem(String id) {
		if (id == null || id.isBlank()) {
			return ItemStack.EMPTY;
		}
		Identifier identifier = Identifier.tryParse(id.trim());
		if (identifier == null) {
			return ItemStack.EMPTY;
		}
		var item = BuiltInRegistries.ITEM.getValue(identifier);
		if (item == null) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(item);
	}
}
