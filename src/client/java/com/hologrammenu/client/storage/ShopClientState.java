package com.hologrammenu.client.storage;

import com.hologrammenu.storage.ShopDefinition;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class ShopClientState {
	private static final Map<BlockPos, ShopDefinition> BY_POS = new HashMap<>();

	private ShopClientState() {
	}

	public static void set(BlockPos pos, ShopDefinition shop) {
		if (pos == null || shop == null) {
			return;
		}
		BY_POS.put(pos.immutable(), shop);
	}

	public static ShopDefinition get(BlockPos pos) {
		if (pos == null) {
			return ShopDefinition.EMPTY;
		}
		return BY_POS.getOrDefault(pos, ShopDefinition.EMPTY);
	}

	public static void clear() {
		BY_POS.clear();
	}
}
