package com.hologrammenu.client.mixin;

import com.hologrammenu.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.hologrammenu.client.mixin.accessor.ScreenInvoker;
import com.hologrammenu.client.config.ClientSettings;
import com.hologrammenu.client.screen.EditorMousePreservation;
import com.hologrammenu.client.screen.StorageMenuEditorOverlay;
import com.hologrammenu.client.screen.widget.StorageMenuTabWidget;
import com.hologrammenu.client.storage.StorageMenuClientPermissions;
import com.hologrammenu.client.storage.StorageMenuClientTracker;
import com.hologrammenu.storage.StorageMenuLocator;
import com.hologrammenu.storage.StorageMenuViewContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(AbstractContainerScreen.class)
public abstract class StorageContainerScreenMixin {
	@Unique
	private StorageMenuEditorOverlay hologrammenu$storageMenuOverlay;

	@Unique
	private StorageMenuTabWidget hologrammenu$storageMenuTab;

	@Inject(method = "init", at = @At("TAIL"))
	private void hologrammenu$addStorageMenuTab(CallbackInfo ci) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		if (!StorageMenuClientPermissions.isStorageEditorScreen(screen)) {
			return;
		}
		if (!StorageMenuLocator.isSupportedMenu(screen.getMenu())) {
			return;
		}

		if (!StorageMenuClientPermissions.canEdit()) {
			return;
		}

		if (!ClientSettings.containerEditorWidgetEnabled) {
			if (hologrammenu$storageMenuOverlay != null) {
				hologrammenu$storageMenuOverlay.dispose();
			}
			hologrammenu$storageMenuTab = null;
			return;
		}

		Optional<StorageMenuViewContext> viewContext = StorageMenuClientTracker.getActiveView()
			.or(() -> StorageMenuLocator.resolveBlockPos(screen.getMenu()).map(StorageMenuViewContext::root));
		if (viewContext.isEmpty()) {
			return;
		}

		int containerSize = StorageMenuLocator.containerSize(screen.getMenu());
		if (containerSize <= 0) {
			return;
		}

		ScreenInvoker invoker = (ScreenInvoker) (Screen) screen;
		AbstractContainerScreenAccessor layout = (AbstractContainerScreenAccessor) screen;

		if (hologrammenu$storageMenuOverlay != null) {
			hologrammenu$storageMenuOverlay.syncViewContextFromTracker();
			hologrammenu$storageMenuOverlay.reattach(layout, containerSize);
		} else {
			hologrammenu$storageMenuOverlay = new StorageMenuEditorOverlay(
				screen,
				layout,
				viewContext.get(),
				containerSize
			);
		}

		if (hologrammenu$storageMenuTab == null) {
			hologrammenu$storageMenuTab = StorageMenuTabWidget.forContainer(
				layout,
				() -> hologrammenu$storageMenuOverlay.isOpen(),
				() -> hologrammenu$storageMenuOverlay.toggle()
			);
		} else {
			hologrammenu$storageMenuTab.reposition(layout);
		}
		invoker.hologrammenu$addRenderableWidget(hologrammenu$storageMenuTab);
		EditorMousePreservation.restoreIfPending();
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void hologrammenu$itemPickerScroll(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
		Screen screen = (Screen) (Object) this;
		StorageMenuEditorOverlay overlay = StorageMenuEditorOverlay.getActive(screen);
		if (overlay != null && overlay.onMouseScrolled(scrollY)) {
			cir.setReturnValue(true);
		}
	}
}
