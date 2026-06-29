package com.hologrammenu.client.mixin;

import com.hologrammenu.client.mixin.accessor.AnvilScreenAccessor;
import com.hologrammenu.client.mixin.accessor.AnvilScreenInvoker;
import com.hologrammenu.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.hologrammenu.client.mixin.accessor.ScreenInvoker;
import com.hologrammenu.client.screen.AnvilEditorTab;
import com.hologrammenu.client.screen.ModEditorGuiState;
import com.hologrammenu.client.screen.TextStyleOverlay;
import com.hologrammenu.client.screen.TextStylePanelPositions;
import com.hologrammenu.client.screen.TextStyleTarget;
import com.hologrammenu.client.screen.widget.AnvilStyleTabWidget;
import com.hologrammenu.text.StyledText;
import com.hologrammenu.text.TextFormats;
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
	private TextStyleOverlay hologrammenu$editorOverlay;

	@Unique
	private AnvilStyleTabWidget hologrammenu$editorTab;

	@Unique
	private String hologrammenu$styledRename = "";

	@Inject(method = "subInit", at = @At("TAIL"))
	private void hologrammenu$addEditorTab(CallbackInfo ci) {
		AnvilScreen screen = (AnvilScreen) (Object) this;
		ScreenInvoker invoker = (ScreenInvoker) (Screen) screen;
		AnvilScreenAccessor accessor = (AnvilScreenAccessor) screen;
		AbstractContainerScreenAccessor layout = (AbstractContainerScreenAccessor) screen;
		EditBox nameField = accessor.hologrammenu$getNameField();
		AnvilScreenInvoker renameInvoker = (AnvilScreenInvoker) screen;

		boolean restoreOpen = hologrammenu$editorOverlay != null && hologrammenu$editorOverlay.isOpen();
		StyledText savedDraft = restoreOpen ? hologrammenu$editorOverlay.getDraft() : null;
		AnvilEditorTab restoredTab = restoreOpen ? hologrammenu$editorOverlay.getAnvilActiveTab() : AnvilEditorTab.STYLE;
		if (hologrammenu$editorOverlay != null) {
			hologrammenu$editorOverlay.dispose();
		}

		nameField.setResponder(plain -> {
			hologrammenu$styledRename = TextFormats.parse(hologrammenu$styledRename).withText(plain).serialize();
			renameInvoker.hologrammenu$onNameChanged(hologrammenu$styledRename);
		});

		hologrammenu$editorOverlay = TextStyleOverlay.forAnvil(
			screen,
			nameField::getValue,
			TextStyleTarget.editBox(nameField, serialized -> {
				hologrammenu$styledRename = serialized;
				if (TextFormats.parse(serialized).text().equals(nameField.getValue())) {
					renameInvoker.hologrammenu$onNameChanged(serialized);
				}
			}),
			() -> TextStylePanelPositions.leftOfContainer(layout, screen),
			hologrammenu$inputStackSupplier(screen)
		);
		hologrammenu$editorOverlay.setOnClose(() -> hologrammenu$syncRenameFieldFocus(nameField));

		if (restoreOpen && savedDraft != null) {
			hologrammenu$editorOverlay.openWithDraft(savedDraft, restoredTab);
		}
		hologrammenu$syncRenameFieldFocus(nameField);

		if (hologrammenu$editorTab == null) {
			hologrammenu$editorTab = AnvilStyleTabWidget.forContainer(
				layout,
				() -> hologrammenu$editorOverlay.isOpen(),
				() -> hologrammenu$toggleEditorPanel(nameField)
			);
		} else {
			hologrammenu$editorTab.reposition(layout);
		}

		invoker.hologrammenu$addRenderableWidget(hologrammenu$editorTab);
	}

	@Unique
	private void hologrammenu$toggleEditorPanel(EditBox nameField) {
		String plain = nameField.getValue();
		hologrammenu$styledRename = TextFormats.parse(hologrammenu$styledRename).withText(plain).serialize();
		AnvilScreen screen = (AnvilScreen) (Object) this;
		AbstractContainerScreenAccessor layout = (AbstractContainerScreenAccessor) screen;
		int[] position = TextStylePanelPositions.leftOfContainer(layout, screen);
		hologrammenu$editorOverlay.toggle(hologrammenu$styledRename, position[0], position[1]);
		hologrammenu$syncRenameFieldFocus(nameField);
	}

	@Redirect(
		method = "keyPressed",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/EditBox;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"
		)
	)
	private boolean hologrammenu$routeKeyPressedToFocusedEditBox(EditBox nameField, KeyEvent event) {
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
	private boolean hologrammenu$routeCanConsumeInputToFocusedEditBox(EditBox nameField) {
		if (ModEditorGuiState.shouldDeferAnvilNameKeyRouting((Screen) (Object) this)) {
			return false;
		}
		return nameField.canConsumeInput();
	}

	@Unique
	private void hologrammenu$syncRenameFieldFocus(EditBox nameField) {
		boolean editorOpen = hologrammenu$editorOverlay != null && hologrammenu$editorOverlay.isOpen();
		nameField.setCanLoseFocus(editorOpen);
		if (!editorOpen) {
			AnvilScreen screen = (AnvilScreen) (Object) this;
			screen.setFocused(nameField);
		}
	}

	@Unique
	private static java.util.function.Supplier<ItemStack> hologrammenu$inputStackSupplier(AnvilScreen screen) {
		return () -> screen.getMenu().getSlot(0).getItem();
	}
}
