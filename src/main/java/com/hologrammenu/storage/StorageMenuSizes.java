package com.hologrammenu.storage;

public final class StorageMenuSizes {
	public static final int SINGLE_CHEST = 27;
	public static final int DOUBLE_CHEST = 54;

	private StorageMenuSizes() {
	}

	public static boolean isChestSize(int size) {
		return size == SINGLE_CHEST || size == DOUBLE_CHEST;
	}

	public static int toggleChestSize(int size) {
		return size == DOUBLE_CHEST ? SINGLE_CHEST : DOUBLE_CHEST;
	}
}
