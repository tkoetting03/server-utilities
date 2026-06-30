package com.serverutilities.client.screen;

import com.serverutilities.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.serverutilities.client.mixin.accessor.ScreenInvoker;
import com.serverutilities.client.screen.widget.UiLayoutHelper;
import com.serverutilities.client.screen.widget.VanillaIconButton;
import com.serverutilities.itemstyler.ItemStylerMenu;
import com.serverutilities.network.ModPackets;
import com.serverutilities.text.TextFormats;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
		this.styleOverlay = TextStyleOverlay.forItemStyler(
			screen,
			this::plainName,
			TextStyleTarget.plainField(this::plainName, ignored -> {
			}, serialized -> {
				styledName = serialized;
				ClientPlayNetworking.send(new ModPackets.ItemStylerApplyNamePayload(serialized));
			}),
			() -> TextStylePanelPositions.besideContainer(layout, screen),
			this::styledStack
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
		int confirmWidth = Math.max(UiLayoutHelper.iconButtonWidth(screen.getFont(), Component.translatable("screen.serverutilities.item_styler.confirm")), 54);
		int styleWidth = Math.max(UiLayoutHelper.iconButtonWidth(screen.getFont(), Component.translatable("screen.serverutilities.item_styler.style")), 54);
		int nameWidth = 68;
		int x = layout.serverutilities$getLeftPos() + 7;
		int y = layout.serverutilities$getTopPos() + 18;
		int actionX = layout.serverutilities$getLeftPos() + 104;
		int actionWidth = Math.max(layout.serverutilities$getImageWidth() - 112, Math.max(confirmWidth, styleWidth));

		nameField = new EditBox(screen.getFont(), x, y, nameWidth, buttonHeight, Component.translatable("screen.serverutilities.item_styler.name"));
		nameField.setMaxLength(64);
		nameField.setHint(Component.translatable("screen.serverutilities.item_styler.name_hint"));
		nameField.setValue(plainName());
		nameField.setResponder(value -> styledName = TextFormats.parse(styledName).withText(value).serialize());
		screenInvoker.serverutilities$addRenderableWidget(nameField);
		ModUiRenderContext.markIfInteractive(nameField);

		confirmButton = VanillaIconButton.create(
			actionX,
			y,
			actionWidth,
			buttonHeight,
			Component.translatable("screen.serverutilities.item_styler.confirm"),
			new ItemStack(Items.EMERALD),
			press -> applyName()
		);
		screenInvoker.serverutilities$addRenderableWidget(confirmButton);
		ModUiRenderContext.markIfInteractive(confirmButton);

		styleButton = VanillaIconButton.create(
			actionX,
			layout.serverutilities$getTopPos() + 2,
			actionWidth,
			buttonHeight,
			Component.translatable("screen.serverutilities.item_styler.style"),
			new ItemStack(Items.NAME_TAG),
			press -> toggleStylePanel()
		);
		screenInvoker.serverutilities$addRenderableWidget(styleButton);
		ModUiRenderContext.markIfInteractive(styleButton);

		nameField.active = true;
		confirmButton.active = true;
		styleButton.active = true;
	}

	private void toggleStylePanel() {
		loadNameFromStack();
		int[] position = TextStylePanelPositions.besideContainer((AbstractContainerScreenAccessor) screen, screen);
		styleOverlay.toggleAnvilTab(styledName, position[0], position[1], AnvilEditorTab.STYLE);
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
