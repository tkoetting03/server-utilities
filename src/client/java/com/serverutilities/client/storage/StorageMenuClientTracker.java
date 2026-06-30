package com.serverutilities.client.storage;

import com.serverutilities.storage.StorageMenuViewContext;
import net.minecraft.core.BlockPos;

import java.util.Optional;

public final class StorageMenuClientTracker {
	private static StorageMenuViewContext activeView;
	private static boolean showBackButton;

	private StorageMenuClientTracker() {
	}

	public static void setActiveView(StorageMenuViewContext viewContext) {
		activeView = viewContext.immutable();
	}

	public static void setActiveMenuPos(BlockPos pos) {
		setActiveView(StorageMenuViewContext.root(pos));
	}

	public static Optional<StorageMenuViewContext> getActiveView() {
		return activeView == null ? Optional.empty() : Optional.of(activeView);
	}

	public static Optional<BlockPos> getActiveMenuPos() {
		return getActiveView().map(StorageMenuViewContext::anchorPos);
	}

	public static void rememberOpenedContainer(BlockPos pos) {
		setActiveMenuPos(pos);
	}

	public static Optional<BlockPos> resolveOpenedContainer() {
		return getActiveMenuPos();
	}

	public static void setShowBackButton(boolean showBack) {
		showBackButton = showBack;
	}

	public static boolean showBackButton() {
		return showBackButton;
	}

	public static void clear() {
		activeView = null;
		showBackButton = false;
	}
}
