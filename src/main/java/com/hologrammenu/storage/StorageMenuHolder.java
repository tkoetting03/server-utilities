package com.hologrammenu.storage;

import org.jspecify.annotations.Nullable;

public interface StorageMenuHolder {
	@Nullable StorageMenuBlockData hologrammenu$getStorageData();

	void hologrammenu$setStorageData(@Nullable StorageMenuBlockData data);
}
