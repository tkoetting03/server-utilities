package com.serverutilities.client.mixin;

import com.serverutilities.client.mixin.accessor.AnvilScreenAccessor;
import com.serverutilities.client.mixin.accessor.AnvilScreenInvoker;
import com.serverutilities.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.serverutilities.client.mixin.accessor.ScreenInvoker;
import com.serverutilities.client.config.ClientSettings;
import com.serverutilities.client.screen.AnvilEditorTab;
import com.serverutilities.client.screen.ModEditorGuiState;
import com.serverutilities.client.screen.TextStyleOverlay;
import com.serverutilities.client.screen.TextStylePanelPositions;
import com.serverutilities.client.screen.TextStyleTarget;
import com.serverutilities.client.screen.widget.AnvilStyleTabWidget;
import com.serverutilities.text.StyledText;
import com.serverutilities.text.TextFormats;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
	@Unique
	private TextStyleOverlay serverutilities$editorOverlay;

	@Unique
	private AnvilStyleTabWidget serverutilities$editorTab;

	@Unique
	private String serverutilities$styledRename = "";

	@Inject(method = "subInit", at = @At("TAIL"))
	private void serverutilities$addEditorTab(CallbackInfo ci) {
		AnvilScreen screen = (AnvilScreen) (Object) this;
		if (!ClientSettings.styleWidgetEnabled) {
			if (serverutilities$editorOverlay != null) {
				serverutilities$editorOverlay.dispose();
				serverutilities$editorOverlay = null;
			}
			serverutilities$editorTab = null;
			return;
		}

		ScreenInvoker invoker = (ScreenInvoker) (Screen) screen;
		AnvilScreenAccessor accessor = (AnvilScreenAccessor) screen;
		AbstractContainerScreenAccessor layout = (AbstractContainerScreenAccessor) screen;
		EditBox nameField = accessor.serverutilities$getNameField();
		AnvilScreenInvoker renameInvoker = (AnvilScreenInvoker) screen;

		boolean restoreOpen = serverutilities$editorOverlay != null && serverutilities$editorOverlay.isOpen();
		StyledText savedDraft = restoreOpen ? serverutilities$editorOverlay.getDraft() : null;
		AnvilEditorTab restoredTab = restoreOpen ? serverutilities$editorOverlay.getAnvilActiveTab() : AnvilEditorTab.STYLE;
		if (serverutilities$editorOverlay != null) {
			serverutilities$editorOverlay.dispose();
		}

		nameField.setResponder(plain -> {
			serverutilities$styledRename = TextFormats.parse(serverutilities$styledRename).withText(plain).serialize();
			renameInvoker.serverutilities$onNameChanged(serverutilities$styledRename);
		});

		serverutilities$editorOverlay = TextStyleOverlay.forAnvil(
			screen,
			nameField::getValue,
			TextStyleTarget.editBox(nameField, serialized -> {
				serverutilities$styledRename = serialized;
				if (TextFormats.parse(serialized).text().equals(nameField.getValue())) {
					renameInvoker.serverutilities$onNameChanged(serialized);
				}
			}),
			() -> TextStylePanelPositions.leftOfContainer(layout, screen),
			serverutilities$inputStackSupplier(screen)
		);
		serverutilities$editorOverlay.setOnClose(() -> serverutilities$syncRenameFieldFocus(nameField));

		if (restoreOpen && savedDraft != null) {
			serverutilities$editorOverlay.openWithDraft(savedDraft, restoredTab);
		}
		serverutilities$syncRenameFieldFocus(nameField);

		if (serverutilities$editorTab == null) {
			serverutilities$editorTab = AnvilStyleTabWidget.forContainer(
				layout,
				() -> serverutilities$editorOverlay.isOpen(),
				() -> serverutilities$toggleEditorPanel(nameField)
			);
		} else {
			serverutilities$editorTab.reposition(layout);
		}

		invoker.serverutilities$addRenderableWidget(serverutilities$editorTab);
	}

	@Unique
	private void serverutilities$toggleEditorPanel(EditBox nameField) {
		String plain = nameField.getValue();
		serverutilities$styledRename = TextFormats.parse(serverutilities$styledRename).withText(plain).serialize();
		AnvilScreen screen = (AnvilScreen) (Object) this;
		AbstractContainerScreenAccessor layout = (AbstractContainerScreenAccessor) screen;
		int[] position = TextStylePanelPositions.leftOfContainer(layout, screen);
		serverutilities$editorOverlay.toggle(serverutilities$styledRename, position[0], position[1]);
		serverutilities$syncRenameFieldFocus(nameField);
	}

	@Redirect(
		method = "keyPressed",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/EditBox;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"
		)
	)
	private boolean serverutilities$routeKeyPressedToFocusedEditBox(EditBox nameField, KeyEvent event) {
		if (ModEditorGuiState.shouldDeferAnvilNameKeyRouting((Screen) (Object) this)) {
			return false;
		}
		return nameField.keyPressed(event);
	}

	@Redirect(
		method = "keyPressed",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/EditBox;canConsumeInput()Z"
		)
	)
	private boolean serverutilities$routeCanConsumeInputToFocusedEditBox(EditBox nameField) {
		if (ModEditorGuiState.shouldDeferAnvilNameKeyRouting((Screen) (Object) this)) {
			return false;
		}
		return nameField.canConsumeInput();
	}

	@Unique
	private void serverutilities$syncRenameFieldFocus(EditBox nameField) {
		boolean editorOpen = serverutilities$editorOverlay != null && serverutilities$editorOverlay.isOpen();
		nameField.setCanLoseFocus(editorOpen);
		if (!editorOpen) {
			AnvilScreen screen = (AnvilScreen) (Object) this;
			screen.setFocused(nameField);
		}
	}

	@Unique
	private static java.util.function.Supplier<ItemStack> serverutilities$inputStackSupplier(AnvilScreen screen) {
		return () -> screen.getMenu().getSlot(0).getItem();
	}
}
