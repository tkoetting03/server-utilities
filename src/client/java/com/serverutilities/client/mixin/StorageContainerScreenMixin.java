package com.serverutilities.client.mixin;

import com.serverutilities.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.serverutilities.client.mixin.accessor.ScreenInvoker;
import com.serverutilities.client.config.ClientSettings;
import com.serverutilities.client.screen.EditorMousePreservation;
import com.serverutilities.client.screen.StorageMenuEditorOverlay;
import com.serverutilities.client.screen.widget.StorageMenuTabWidget;
import com.serverutilities.client.storage.StorageMenuClientPermissions;
import com.serverutilities.client.storage.StorageMenuClientTracker;
import com.serverutilities.storage.StorageMenuLocator;
import com.serverutilities.storage.StorageMenuViewContext;
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
	private StorageMenuEditorOverlay serverutilities$storageMenuOverlay;

	@Unique
	private StorageMenuTabWidget serverutilities$storageMenuTab;

	@Inject(method = "init", at = @At("TAIL"))
	private void serverutilities$addStorageMenuTab(CallbackInfo ci) {
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
			if (serverutilities$storageMenuOverlay != null) {
				serverutilities$storageMenuOverlay.dispose();
			}
			serverutilities$storageMenuTab = null;
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

		if (serverutilities$storageMenuOverlay != null) {
			serverutilities$storageMenuOverlay.syncViewContextFromTracker();
			serverutilities$storageMenuOverlay.reattach(layout, containerSize);
		} else {
			serverutilities$storageMenuOverlay = new StorageMenuEditorOverlay(
				screen,
				layout,
				viewContext.get(),
				containerSize
			);
		}

		if (serverutilities$storageMenuTab == null) {
			serverutilities$storageMenuTab = StorageMenuTabWidget.forContainer(
				layout,
				() -> serverutilities$storageMenuOverlay.isOpen(),
				() -> serverutilities$storageMenuOverlay.toggle()
			);
		} else {
			serverutilities$storageMenuTab.reposition(layout);
		}
		invoker.serverutilities$addRenderableWidget(serverutilities$storageMenuTab);
		EditorMousePreservation.restoreIfPending();
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void serverutilities$itemPickerScroll(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
		Screen screen = (Screen) (Object) this;
		StorageMenuEditorOverlay overlay = StorageMenuEditorOverlay.getActive(screen);
		if (overlay != null && overlay.onMouseScrolled(scrollY)) {
			cir.setReturnValue(true);
		}
	}
}
