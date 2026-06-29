package com.hologrammenu.client.screen;

import com.hologrammenu.client.mixin.accessor.ScreenInvoker;
import com.hologrammenu.client.screen.widget.DraggableTitleBarWidget;
import com.hologrammenu.client.screen.widget.LabeledFieldLayout;
import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.NpcHologramStackPanelWidget;
import com.hologrammenu.client.screen.widget.NpcStackDragHandle;
import com.hologrammenu.client.screen.widget.PartSelectEditBox;
import com.hologrammenu.client.screen.widget.TextStylePanelLayout;
import com.hologrammenu.client.screen.widget.TextStylePanelWidget;
import com.hologrammenu.client.screen.widget.UiLayoutHelper;
import com.hologrammenu.npc.NpcHologramStack;
import com.hologrammenu.text.TextFormats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class NpcHologramStackOverlay implements NpcStackDragHandle.Listener {
	private static final Map<Screen, NpcHologramStackOverlay> ACTIVE = new WeakHashMap<>();

	private final Screen parent;
	private final Supplier<String> nameTextSupplier;
	private final Consumer<List<NpcHologramStack.Entry>> stackConsumer;
	private final Supplier<int[]> panelPositionSupplier;
	private final ScreenInvoker screenInvoker;
	private final List<GuiEventListener> widgets = new ArrayList<>();
	private final Map<Integer, EditBox> fieldsByEntryIndex = new HashMap<>();
	private final List<NpcHologramStack.Entry> entries = new ArrayList<>();

	private TextStyleOverlay lineStyleOverlay;
	private int stylingLineIndex = -1;
	private int selectedIndex;
	private int draggingIndex = -1;
	private int dragTargetIndex = -1;
	private int[] rowTops = new int[0];
	private boolean open;
	private DraggablePanelGroup dragGroup;
	private Runnable onClose;

	public NpcHologramStackOverlay(
		Screen parent,
		Supplier<String> nameTextSupplier,
		Consumer<List<NpcHologramStack.Entry>> stackConsumer,
		Supplier<int[]> panelPositionSupplier
	) {
		this.parent = parent;
		this.nameTextSupplier = nameTextSupplier;
		this.stackConsumer = stackConsumer;
		this.panelPositionSupplier = panelPositionSupplier;
		this.screenInvoker = (ScreenInvoker) parent;
	}

	public static NpcHologramStackOverlay getActive(Screen screen) {
		return ACTIVE.get(screen);
	}

	public static void onScreenRemoved(Screen screen) {
		NpcHologramStackOverlay overlay = ACTIVE.remove(screen);
		if (overlay != null) {
			overlay.dispose();
		}
	}

	public void setOnClose(Runnable onClose) {
		this.onClose = onClose;
	}

	public boolean isOpen() {
		return open;
	}

	public void dispose() {
		if (!open) {
			return;
		}
		disposeLineStyleOverlay();
		tearDownWidgets();
		open = false;
		ACTIVE.remove(parent);
	}

	public List<NpcHologramStack.Entry> entries() {
		syncFieldsToEntries();
		return List.copyOf(NpcHologramStack.withNameText(entries, nameTextSupplier.get()));
	}

	public void toggle(List<NpcHologramStack.Entry> source) {
		if (open) {
			close();
		} else {
			open(source);
		}
	}

	public void open(List<NpcHologramStack.Entry> source) {
		entries.clear();
		entries.addAll(NpcHologramStack.withNameText(source, nameTextSupplier.get()));
		selectedIndex = indexOfName();
		open = true;
		ACTIVE.put(parent, this);
		int[] position = panelPositionSupplier.get();
		buildWidgets(position[0], position[1]);
	}

	public void close() {
		if (!open) {
			return;
		}
		syncFieldsToEntries();
		stackConsumer.accept(entries());
		disposeLineStyleOverlay();
		tearDownWidgets();
		open = false;
		ACTIVE.remove(parent);
		if (onClose != null) {
			onClose.run();
		}
	}

	@Override
	public void onDragStart(int rowIndex) {
		draggingIndex = rowIndex;
		dragTargetIndex = rowIndex;
	}

	@Override
	public void onDragMove(int mouseY) {
		if (draggingIndex < 0) {
			return;
		}
		dragTargetIndex = resolveTargetIndex(mouseY);
	}

	@Override
	public void onDragEnd() {
		if (draggingIndex >= 0 && dragTargetIndex >= 0 && draggingIndex != dragTargetIndex) {
			NpcHologramStack.Entry moved = entries.remove(draggingIndex);
			entries.add(dragTargetIndex, moved);
			selectedIndex = dragTargetIndex;
			stackConsumer.accept(entries());
			relayout();
		}
		draggingIndex = -1;
		dragTargetIndex = -1;
	}

	private void relayout() {
		boolean styleOpen = lineStyleOverlay != null && lineStyleOverlay.isOpen();
		var savedDraft = styleOpen ? lineStyleOverlay.getDraft() : null;
		int panelX = dragGroup != null ? dragGroup.anchorX() : panelPositionSupplier.get()[0];
		int panelY = dragGroup != null ? dragGroup.anchorY() : panelPositionSupplier.get()[1];
		tearDownWidgets();
		buildWidgets(panelX, panelY);
		if (styleOpen && savedDraft != null && lineStyleOverlay != null) {
			lineStyleOverlay.openWithDraft(savedDraft);
		}
	}

	private void buildWidgets(int panelX, int panelY) {
		fieldsByEntryIndex.clear();
		syncNameEntry();

		int partCount = entries.size();
		int buttonHeight = UiLayoutHelper.buttonHeight(Minecraft.getInstance().font);
		TextStylePanelLayout.Metrics layout = TextStylePanelLayout.metrics(partCount, 0, false);
		int panelHeight = panelHeight(partCount, buttonHeight, layout);
		int panelWidth = TextStylePanelWidget.PANEL_WIDTH;

		dragGroup = new DraggablePanelGroup(parent, "npc_hologram_stack");
		int[] position = dragGroup.resolvePosition(panelX, panelY, panelWidth, panelHeight);
		panelX = position[0];
		panelY = position[1];

		var panelWidget = new NpcHologramStackPanelWidget(panelX, panelY, panelHeight);
		screenInvoker.hologrammenu$addRenderableOnly(panelWidget);
		widgets.add(panelWidget);
		dragGroup.track(panelWidget);

		buildEntryRows(panelX, panelY, partCount, layout, buttonHeight);
		buildFooter(panelX, panelY, partCount, layout, buttonHeight);

		DraggableTitleBarWidget titleBar = dragGroup.createTitleBar(
			Component.translatable("screen.hologrammenu.npc_options.holograms_title"),
			panelWidth,
			ModPanelLayout.TITLE_BAR_HEIGHT
		);
		attach(titleBar, true);
	}

	private void buildEntryRows(int panelX, int panelY, int partCount, TextStylePanelLayout.Metrics layout, int buttonHeight) {
		rowTops = new int[partCount];
		int left = panelX + TextStylePanelLayout.CONTENT_LEFT;
		int handleSize = buttonHeight;
		int styleWidth = UiLayoutHelper.buttonWidth(Minecraft.getInstance().font, Component.translatable("screen.hologrammenu.hologram_options.style"));
		int removeWidth = buttonHeight;
		int gap = layout.buttonRowGap();
		int contentWidth = TextStylePanelLayout.CONTENT_WIDTH;
		int holoFieldWidth = contentWidth - handleSize - gap - styleWidth - gap - removeWidth;
		int nameFieldWidth = contentWidth - handleSize - gap;
		int addRowWidth = contentWidth;

		for (int index = 0; index < partCount; index++) {
			NpcHologramStack.Entry entry = entries.get(index);
			int rowY = panelY + layout.partTop() + index * layout.partRowHeight();
			rowTops[index] = rowY;
			boolean isName = entry.isName();
			int rowIndex = index;
			int x = left;

			attach(new NpcStackDragHandle(x, rowY, handleSize, index, this), true);
			x += handleSize + gap;

			if (isName) {
				EditBox field = new PartSelectEditBox(
					Minecraft.getInstance().font,
					x,
					rowY,
					nameFieldWidth,
					LabeledFieldLayout.FIELD_HEIGHT,
					Component.translatable("screen.hologrammenu.npc_options.stack_name"),
					() -> selectedIndex = rowIndex
				);
				field.setEditable(false);
				field.setValue(TextFormats.parse(nameTextSupplier.get()).text());
				attach(field, false);
				fieldsByEntryIndex.put(index, field);
			} else {
				EditBox field = new PartSelectEditBox(
					Minecraft.getInstance().font,
					x,
					rowY,
					holoFieldWidth,
					LabeledFieldLayout.FIELD_HEIGHT,
					Component.translatable("screen.hologrammenu.npc_options.stack_hologram", hologramNumber(index)),
					() -> selectedIndex = rowIndex
				);
				field.setMaxLength(256);
				field.setValue(TextFormats.parse(entry.text()).text());
				field.addFormatter((visible, start) -> TextFormats.editBoxFormat(entry.text(), visible, start));
				int capturedIndex = index;
				field.setResponder(value -> {
					selectedIndex = capturedIndex;
					NpcHologramStack.Entry current = entries.get(capturedIndex);
					entries.set(capturedIndex, new NpcHologramStack.Entry(
						NpcHologramStack.KIND_HOLOGRAM,
						TextFormats.parse(current.text()).withText(value).serialize(),
						current.scale()
					));
					stackConsumer.accept(entries());
				});
				attach(field, true);
				fieldsByEntryIndex.put(index, field);

				int styleX = x + holoFieldWidth + gap;
				attach(Button.builder(Component.translatable("screen.hologrammenu.hologram_options.style"), press -> openLineStyle(capturedIndex))
					.bounds(styleX, rowY, styleWidth, buttonHeight).build(), true);

				int removeX = styleX + styleWidth + gap;
				attach(Button.builder(Component.literal("x"), press -> removeEntry(capturedIndex))
					.bounds(removeX, rowY, removeWidth, buttonHeight).build(), true);
			}
		}

		if (partCount < NpcHologramStack.MAX_ENTRIES) {
			int addY = panelY + layout.partTop() + partCount * layout.partRowHeight();
			attach(Button.builder(Component.translatable("screen.hologrammenu.npc_options.add_hologram"), press -> addHologram())
				.bounds(left, addY, addRowWidth, buttonHeight).build(), true);
		}
	}

	private void buildFooter(int panelX, int panelY, int partCount, TextStylePanelLayout.Metrics layout, int buttonHeight) {
		int left = panelX + TextStylePanelLayout.CONTENT_LEFT;
		int contentWidth = TextStylePanelLayout.CONTENT_WIDTH;
		int gap = layout.buttonRowGap();
		int addButton = partCount < NpcHologramStack.MAX_ENTRIES ? buttonHeight + gap : 0;
		int footerY = panelY + layout.partTop() + partCount * layout.partRowHeight() + addButton + ModPanelLayout.SECTION_GAP;

		attach(Button.builder(Component.translatable("screen.hologrammenu.npc_options.add_above"), press -> addRelative(-1))
			.bounds(left, footerY, contentWidth, buttonHeight).build(), true);
		attach(Button.builder(Component.translatable("screen.hologrammenu.npc_options.add_below"), press -> addRelative(1))
			.bounds(left, footerY + buttonHeight + gap, contentWidth, buttonHeight).build(), true);
		attach(Button.builder(Component.translatable("gui.done"), press -> close())
			.bounds(left, footerY + (buttonHeight + gap) * 2, contentWidth, buttonHeight).build(), true);
	}

	private int panelHeight(int partCount, int buttonHeight, TextStylePanelLayout.Metrics layout) {
		int addButton = partCount < NpcHologramStack.MAX_ENTRIES ? buttonHeight + layout.buttonRowGap() : 0;
		int footer = ModPanelLayout.SECTION_GAP + ModPanelLayout.stackHeight(3, buttonHeight, layout.buttonRowGap()) + ModPanelLayout.PANEL_PADDING;
		return layout.partTop() + partCount * layout.partRowHeight() + addButton + footer;
	}

	private int hologramNumber(int index) {
		int number = 0;
		for (int i = 0; i <= index; i++) {
			if (!entries.get(i).isName()) {
				number++;
			}
		}
		return number;
	}

	private int resolveTargetIndex(int mouseY) {
		for (int index = 0; index < rowTops.length; index++) {
			int top = rowTops[index];
			int bottom = top + LabeledFieldLayout.FIELD_HEIGHT;
			if (mouseY >= top && mouseY <= bottom) {
				return index;
			}
		}
		return Math.max(0, Math.min(entries.size() - 1, dragTargetIndex));
	}

	private void addHologram() {
		if (entries.size() >= NpcHologramStack.MAX_ENTRIES) {
			return;
		}
		syncFieldsToEntries();
		int insertAt = Math.min(entries.size(), selectedIndex + 1);
		entries.add(insertAt, NpcHologramStack.Entry.hologram(""));
		selectedIndex = insertAt;
		stackConsumer.accept(entries());
		relayout();
	}

	private void addRelative(int direction) {
		if (entries.size() >= NpcHologramStack.MAX_ENTRIES) {
			return;
		}
		syncFieldsToEntries();
		int insertAt = direction < 0 ? selectedIndex : selectedIndex + 1;
		insertAt = Math.max(0, Math.min(entries.size(), insertAt));
		entries.add(insertAt, NpcHologramStack.Entry.hologram(""));
		selectedIndex = insertAt;
		stackConsumer.accept(entries());
		relayout();
	}

	private void removeEntry(int index) {
		if (index < 0 || index >= entries.size() || entries.get(index).isName()) {
			return;
		}
		syncFieldsToEntries();
		entries.remove(index);
		selectedIndex = Math.min(selectedIndex, entries.size() - 1);
		stackConsumer.accept(entries());
		relayout();
	}

	private void openLineStyle(int index) {
		if (index < 0 || index >= entries.size() || entries.get(index).isName()) {
			return;
		}
		syncFieldsToEntries();
		stylingLineIndex = index;
		ensureLineStyleOverlay();
		NpcHologramStack.Entry entry = entries.get(index);
		int[] position = lineStylePanelPosition();
		lineStyleOverlay.toggle(entry.text(), position[0], position[1]);
	}

	private int[] lineStylePanelPosition() {
		if (dragGroup == null) {
			return panelPositionSupplier.get();
		}
		return TextStylePanelPositions.leftOfPanel(
			parent,
			dragGroup.anchorX(),
			dragGroup.anchorY(),
			TextStylePanelWidget.PANEL_WIDTH
		);
	}

	private void ensureLineStyleOverlay() {
		if (lineStyleOverlay != null) {
			return;
		}
		lineStyleOverlay = new TextStyleOverlay(
			parent,
			() -> {
				if (stylingLineIndex < 0 || stylingLineIndex >= entries.size()) {
					return "";
				}
				return TextFormats.parse(entries.get(stylingLineIndex).text()).text();
			},
			this::applyLineStyle,
			this::lineStylePanelPosition
		);
	}

	private void applyLineStyle(String serialized) {
		if (stylingLineIndex < 0 || stylingLineIndex >= entries.size()) {
			return;
		}
		NpcHologramStack.Entry entry = entries.get(stylingLineIndex);
		String normalized = TextFormats.normalize(serialized);
		entries.set(stylingLineIndex, new NpcHologramStack.Entry(
			NpcHologramStack.KIND_HOLOGRAM,
			normalized,
			entry.scale()
		));
		stackConsumer.accept(entries());
		EditBox field = fieldsByEntryIndex.get(stylingLineIndex);
		if (field != null) {
			String plain = TextFormats.parse(normalized).text();
			if (!plain.equals(field.getValue())) {
				field.setValue(plain);
			}
		}
	}

	private void disposeLineStyleOverlay() {
		if (lineStyleOverlay != null) {
			lineStyleOverlay.dispose();
			lineStyleOverlay = null;
		}
		stylingLineIndex = -1;
	}

	private void syncNameEntry() {
		String nameText = nameTextSupplier.get();
		for (int index = 0; index < entries.size(); index++) {
			if (entries.get(index).isName()) {
				NpcHologramStack.Entry entry = entries.get(index);
				entries.set(index, new NpcHologramStack.Entry(NpcHologramStack.KIND_NAME, nameText, entry.scale()));
				EditBox field = fieldsByEntryIndex.get(index);
				if (field != null) {
					field.setValue(TextFormats.parse(nameText).text());
				}
				return;
			}
		}
	}

	private void syncFieldsToEntries() {
		syncNameEntry();
		for (Map.Entry<Integer, EditBox> fieldEntry : fieldsByEntryIndex.entrySet()) {
			int index = fieldEntry.getKey();
			if (index < 0 || index >= entries.size() || entries.get(index).isName()) {
				continue;
			}
			EditBox field = fieldEntry.getValue();
			if (entries.get(index).isName()) {
				continue;
			}
			NpcHologramStack.Entry entry = entries.get(index);
			entries.set(index, new NpcHologramStack.Entry(
				NpcHologramStack.KIND_HOLOGRAM,
				TextFormats.parse(entry.text()).withText(field.getValue()).serialize(),
				entry.scale()
			));
		}
	}

	private int indexOfName() {
		for (int index = 0; index < entries.size(); index++) {
			if (entries.get(index).isName()) {
				return index;
			}
		}
		return 0;
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
			} else if (widget instanceof AbstractWidget abstractWidget) {
				screenInvoker.hologrammenu$addRenderableWidget(abstractWidget);
			}
		} else if (widget instanceof Renderable renderable) {
			screenInvoker.hologrammenu$addRenderableOnly(renderable);
		}
		widgets.add(widget);
		if (dragGroup != null && widget instanceof AbstractWidget abstractWidget && !(widget instanceof DraggableTitleBarWidget)) {
			dragGroup.track(abstractWidget);
		}
	}

	private void tearDownWidgets() {
		for (GuiEventListener widget : widgets) {
			screenInvoker.hologrammenu$removeWidget(widget);
		}
		widgets.clear();
		fieldsByEntryIndex.clear();
		dragGroup = null;
	}
}
