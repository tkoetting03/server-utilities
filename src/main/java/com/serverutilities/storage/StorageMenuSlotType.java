package com.serverutilities.storage;

public enum StorageMenuSlotType {
	EMPTY,
	FILLER,
	COMMAND,
	LINK,
	SHOP_ITEM,
	BACK,
	CLOSE;

	public static StorageMenuSlotType fromId(int id) {
		StorageMenuSlotType[] values = values();
		if (id < 0 || id >= values.length) {
			return EMPTY;
		}
		return values[id];
	}

	public boolean isEditable() {
		return this != BACK && this != CLOSE;
	}
}
