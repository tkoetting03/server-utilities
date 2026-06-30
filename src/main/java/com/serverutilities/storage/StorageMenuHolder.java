package com.serverutilities.storage;

import org.jspecify.annotations.Nullable;

public interface StorageMenuHolder {
	@Nullable StorageMenuBlockData serverutilities$getStorageData();

	void serverutilities$setStorageData(@Nullable StorageMenuBlockData data);
}
