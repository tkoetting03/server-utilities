package com.hologrammenu.client.screen;

import com.hologrammenu.client.mixin.accessor.ScreenInvoker;
import com.hologrammenu.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.hologrammenu.client.screen.widget.DraggableTitleBarWidget;
import com.hologrammenu.client.screen.widget.LabeledFieldLayout;
import com.hologrammenu.client.screen.widget.StorageMenuNamePanelWidget;
import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.UiLayoutHelper;
import com.hologrammenu.client.screen.widget.UiScale;
import com.hologrammenu.storage.StorageMenuItemNames;
import com.hologrammenu.text.StyledText;
import com.hologrammenu.text.TextFormats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class StorageMenuNameOverlay {
	private static final Map<Screen, StorageMenuNameOverlay> ACTIVE = new WeakHashMap<>();

	private final Screen parent;
	private final ScreenInvoker screenInvoker;
	private final Supplier<ItemStack> stackSupplier;
	private final Consumer<ItemStack> onApply;
	private final List<GuiEventListener> widgets = new ArrayList<>();

	private TextStyleOverlay styleOverlay;
	private DraggablePanelGroup dragGroup;
	private EditBox nameField;
	private String styledName = "";
	private boolean open;
	private boolean styleDockedLayout;
	private int panelX;
	private int panelY;

	public StorageMenuNameOverlay(
		Screen parent,
		Supplier<ItemStack> stackSupplier,
		Consumer<ItemStack> onApply
	) {
		this.parent = parent;
		this.screenInvoker = (ScreenInvoker) parent;
		this.stackSupplier = stackSupplier;
		this.onApply = onApply;
	}

	public static void onScreenRemoved(Screen screen) {
		StorageMenuNameOverlay overlay = ACTIVE.remove(screen);
		if (overlay != null) {
			overlay.close();
		}
	}

	public boolean isOpen() {
		return open;
	}

	public void dispose() {
		if (open) {
			close();
		}
		if (styleOverlay != null) {
			styleOverlay.dispose();
			styleOverlay = null;
		}
	}

	public void toggle() {
		if (open) {
			close();
		} else {
			open();
		}
	}

	public void open() {
		ItemStack stack = stackSupplier.get();
		if (stack.isEmpty()) {
			return;
		}

		if (open) {
			return;
		}

		styledName = StorageMenuItemNames.readCustomName(stack);
		styleDockedLayout = false;
		open = true;
		ACTIVE.put(parent, this);
		buildWidgets();
	}

	public void close() {
		if (!open) {
			return;
		}

		if (styleOverlay != null) {
			styleOverlay.close();
		}
		apply();
		tearDownWidgets();
		open = false;
		styleDockedLayout = false;
		ACTIVE.remove(parent);
	}

	private void buildWidgets() {
		tearDownWidgets();
		boolean styleWasOpen = styleOverlay != null && styleOverlay.isOpen();
		StyledText savedStyleDraft = styleWasOpen ? styleOverlay.getDraft() : null;
		if (styleOverlay != null) {
			styleOverlay.dispose();
			styleOverlay = null;
		}

		int[] defaultPosition = panelPosition();
		dragGroup = new DraggablePanelGroup(parent, "name_editor");
		int[] position = dragGroup.resolvePosition(
			defaultPosition[0],
			defaultPosition[1],
			StorageMenuNamePanelWidget.PANEL_WIDTH,
			StorageMenuNamePanelWidget.panelHeight()
		);
		panelX = position[0];
		panelY = position[1];

		boolean styleEditing = styleWasOpen;

		var panelWidget = new StorageMenuNamePanelWidget(panelX, panelY);
		screenInvoker.hologrammenu$addRenderableOnly(panelWidget);
		widgets.add(panelWidget);
		dragGroup.track(panelWidget);

		int fieldX = panelX + ModPanelLayout.PANEL_PADDING;
		int fieldY = panelY + StorageMenuNamePanelWidget.CONTENT_TOP;
		int fieldWidth = ModPanelLayout.CONTENT_WIDTH;
		int buttonH = UiLayoutHelper.defaultButtonHeight();
		int rowGap = ModPanelLayout.ROW_GAP;

		if (!styleEditing) {
			nameField = new EditBox(
				Minecraft.getInstance().font,
				fieldX,
				fieldY,
				fieldWidth,
				LabeledFieldLayout.FIELD_HEIGHT,
				Component.translatable("screen.hologrammenu.storage_menu.name_field")
			);
			nameField.setMaxLength(64);
			nameField.setValue(TextFormats.parse(styledName).text());
			nameField.setHint(Component.translatable("screen.hologrammenu.storage_menu.name_hint"));
			nameField.setResponder(plain -> {
				styledName = TextFormats.parse(styledName).withText(plain).serialize();
				apply();
			});
			screenInvoker.hologrammenu$addRenderableWidget(nameField);
			ModUiRenderContext.markIfInteractive(nameField);
			widgets.add(nameField);
			dragGroup.track(nameField);
		} else {
			nameField = null;
		}

		if (styleOverlay == null) {
			styleOverlay = new TextStyleOverlay(
				parent,
				this::plainTextForStyle,
				serialized -> {
					styledName = serialized;
					if (nameField != null) {
						String plain = TextFormats.parse(serialized).text();
						if (!plain.equals(nameField.getValue())) {
							nameField.setValue(plain);
						}
					}
					ItemStack preview = StorageMenuItemNames.withCustomName(stackSupplier.get(), serialized);
					onApply.accept(preview);
				},
				this::stylePanelPosition
			);
			styleOverlay.setOnClose(() -> {
				if (styleDockedLayout) {
					styleDockedLayout = false;
					buildWidgets();
				}
			});
		}
		if (styleWasOpen && savedStyleDraft != null) {
			styleOverlay.openWithDraft(savedStyleDraft.withText(plainTextForStyle()));
		}

		int buttonRowY = styleEditing
			? fieldY
			: fieldY + LabeledFieldLayout.FIELD_HEIGHT + rowGap;
		int buttonWidth = ModPanelLayout.columnWidth(fieldWidth, 2, rowGap);

		attachButton(Button.builder(
			Component.translatable("screen.hologrammenu.hologram_options.style"),
			press -> openStylePanel()
		).bounds(fieldX, buttonRowY, buttonWidth, buttonH).build());

		attachButton(Button.builder(
			Component.translatable("screen.hologrammenu.storage_menu.name_clear"),
			press -> clearName()
		).bounds(fieldX + buttonWidth + rowGap, buttonRowY, buttonWidth, buttonH).build());

		int doneY = buttonRowY + buttonH + rowGap;
		attachButton(Button.builder(
			Component.translatable("gui.done"),
			press -> close()
		).bounds(fieldX, doneY, fieldWidth, buttonH).build());

		DraggableTitleBarWidget titleBar = dragGroup.createTitleBar(
			Component.translatable("screen.hologrammenu.storage_menu.name_title"),
			StorageMenuNamePanelWidget.PANEL_WIDTH,
			UiScale.s(14)
		);
		attachButton(titleBar);
	}

	private String plainTextForStyle() {
		if (styleOverlay != null && styleOverlay.isOpen()) {
			return styleOverlay.getPlainText();
		}
		if (nameField != null) {
			return nameField.getValue();
		}
		return TextFormats.parse(styledName).text();
	}

	private int[] panelPosition() {
		AbstractContainerScreenAccessor layout = (AbstractContainerScreenAccessor) parent;
		return StorageMenuNamePanelPositions.rightOfContainer(layout, parent);
	}

	private int[] stylePanelPosition() {
		if (styleDockedLayout) {
			return TextStylePanelPositions.belowPanel(parent, panelX, panelY, StorageMenuNamePanelWidget.panelHeight());
		}
		return TextStylePanelPositions.rightOfPanel(
			parent,
			panelX,
			panelY,
			StorageMenuNamePanelWidget.PANEL_WIDTH
		);
	}

	private void openStylePanel() {
		if (styleOverlay == null) {
			return;
		}
		styledName = TextFormats.parse(styledName).withText(plainTextForStyle()).serialize();
		boolean opening = !styleOverlay.isOpen();
		if (opening && !styleDockedLayout) {
			styleDockedLayout = true;
			buildWidgets();
		}
		int[] stylePos = stylePanelPosition();
		styleOverlay.toggle(styledName, stylePos[0], stylePos[1]);
		if (!styleOverlay.isOpen() && styleDockedLayout) {
			styleDockedLayout = false;
			buildWidgets();
		} else if (styleOverlay.isOpen() && styleDockedLayout) {
			buildWidgets();
			styleOverlay.relayout();
		}
	}

	private void clearName() {
		styledName = "";
		if (nameField != null) {
			nameField.setValue("");
		}
		if (styleOverlay != null && styleOverlay.isOpen()) {
			styleOverlay.close();
		}
		apply();
	}

	private void apply() {
		if (styleOverlay != null && styleOverlay.isOpen()) {
			styledName = TextFormats.normalize(styleOverlay.getDraft().serialize());
		} else if (nameField != null) {
			styledName = TextFormats.normalize(TextFormats.parse(styledName).withText(nameField.getValue()).serialize());
		}
		ItemStack updated = StorageMenuItemNames.withCustomName(stackSupplier.get(), styledName);
		onApply.accept(updated);
	}

	private void tearDownWidgets() {
		for (GuiEventListener widget : widgets) {
			screenInvoker.hologrammenu$removeWidget(widget);
		}
		widgets.clear();
		nameField = null;
	}

	private void attachButton(AbstractWidget button) {
		screenInvoker.hologrammenu$addRenderableWidget(button);
		ModUiRenderContext.markIfInteractive(button);
		widgets.add(button);
		if (dragGroup != null && !(button instanceof DraggableTitleBarWidget)) {
			dragGroup.track(button);
		}
	}
}
