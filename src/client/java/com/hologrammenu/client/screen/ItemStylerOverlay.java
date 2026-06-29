package com.hologrammenu.client.screen;

import com.hologrammenu.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.hologrammenu.client.mixin.accessor.ScreenInvoker;
import com.hologrammenu.client.screen.widget.UiLayoutHelper;
import com.hologrammenu.itemstyler.ItemStylerMenu;
import com.hologrammenu.network.ModPackets;
import com.hologrammenu.text.TextFormats;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

public final class ItemStylerOverlay {
	private static final Map<Screen, ItemStylerOverlay> ACTIVE = new WeakHashMap<>();

	private final AbstractContainerScreen<?> screen;
	private final ScreenInvoker screenInvoker;
	private final TextStyleOverlay styleOverlay;
	private EditBox nameField;
	private Button confirmButton;
	private Button styleButton;
	private String styledName = "";

	public ItemStylerOverlay(AbstractContainerScreen<?> screen, AbstractContainerScreenAccessor layout) {
		this.screen = screen;
		this.screenInvoker = (ScreenInvoker) (Screen) screen;
		this.styleOverlay = new TextStyleOverlay(
			screen,
			this::plainName,
			TextStyleTarget.plainField(this::plainName, ignored -> {
			}, serialized -> {
				styledName = serialized;
				ClientPlayNetworking.send(new ModPackets.ItemStylerApplyNamePayload(serialized));
			}),
			() -> TextStylePanelPositions.besideContainer(layout, screen)
		);
		this.styleOverlay.setOnClose(() -> {
			if (styleButton != null) {
				styleButton.active = true;
			}
		});
		ACTIVE.put((Screen) screen, this);
	}

	public static boolean isItemStylerScreen(Screen screen) {
		return screen instanceof AbstractContainerScreen<?> containerScreen && containerScreen.getMenu() instanceof ItemStylerMenu;
	}

	public static void onScreenRemoved(Screen screen) {
		ItemStylerOverlay overlay = ACTIVE.remove(screen);
		if (overlay != null) {
			overlay.close();
		}
	}

	public void attach(AbstractContainerScreenAccessor layout) {
		loadNameFromStack();
		int buttonHeight = UiLayoutHelper.buttonHeight(screen.getFont());
		int confirmWidth = Math.max(UiLayoutHelper.buttonWidth(screen.getFont(), Component.translatable("screen.hologrammenu.item_styler.confirm")), 54);
		int nameWidth = 68;
		int x = layout.hologrammenu$getLeftPos() + 7;
		int y = layout.hologrammenu$getTopPos() + 18;
		int actionX = layout.hologrammenu$getLeftPos() + 104;
		int actionWidth = layout.hologrammenu$getImageWidth() - 112;

		nameField = new EditBox(screen.getFont(), x, y, nameWidth, buttonHeight, Component.translatable("screen.hologrammenu.item_styler.name"));
		nameField.setMaxLength(64);
		nameField.setHint(Component.translatable("screen.hologrammenu.item_styler.name_hint"));
		nameField.setValue(plainName());
		nameField.setResponder(value -> styledName = TextFormats.parse(styledName).withText(value).serialize());
		screenInvoker.hologrammenu$addRenderableWidget(nameField);
		ModUiRenderContext.markIfInteractive(nameField);

		confirmButton = Button.builder(Component.translatable("screen.hologrammenu.item_styler.confirm"), press -> applyName())
			.bounds(actionX, y, Math.max(confirmWidth, actionWidth), buttonHeight)
			.build();
		screenInvoker.hologrammenu$addRenderableWidget(confirmButton);
		ModUiRenderContext.markIfInteractive(confirmButton);

		styleButton = Button.builder(Component.translatable("screen.hologrammenu.item_styler.style"), press -> toggleStylePanel())
			.bounds(actionX, layout.hologrammenu$getTopPos() + 2, actionWidth, buttonHeight)
			.build();
		screenInvoker.hologrammenu$addRenderableWidget(styleButton);
		ModUiRenderContext.markIfInteractive(styleButton);

		boolean hasItem = !styledStack().isEmpty();
		nameField.active = hasItem;
		confirmButton.active = hasItem;
		styleButton.active = hasItem;
	}

	private void toggleStylePanel() {
		loadNameFromStack();
		int[] position = TextStylePanelPositions.besideContainer((AbstractContainerScreenAccessor) screen, screen);
		styleOverlay.toggle(styledName, position[0], position[1]);
	}

	private void applyName() {
		if (nameField == null || styledStack().isEmpty()) {
			return;
		}
		styledName = TextFormats.parse(styledName).withText(nameField.getValue()).serialize();
		ClientPlayNetworking.send(new ModPackets.ItemStylerApplyNamePayload(styledName));
	}

	private ItemStack styledStack() {
		return screen.getMenu() instanceof ItemStylerMenu menu ? menu.styledStack() : ItemStack.EMPTY;
	}

	private void loadNameFromStack() {
		ItemStack stack = styledStack();
		if (stack.isEmpty()) {
			styledName = "";
			return;
		}
		Component customName = stack.get(DataComponents.CUSTOM_NAME);
		styledName = customName == null ? stack.getHoverName().getString() : TextFormats.fromComponent(customName);
	}

	private String plainName() {
		return TextFormats.parse(styledName).text();
	}

	private void close() {
		styleOverlay.dispose();
	}
}
