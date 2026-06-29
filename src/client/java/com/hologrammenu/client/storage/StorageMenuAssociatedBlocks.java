package com.hologrammenu.client.storage;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class StorageMenuAssociatedBlocks {
	private static final Set<BlockPos> KNOWN = new HashSet<>();

	private StorageMenuAssociatedBlocks() {
	}

	public static void remember(BlockPos pos) {
		if (pos != null) {
			KNOWN.add(pos.immutable());
		}
	}

	public static void forget(BlockPos pos) {
		if (pos != null) {
			KNOWN.remove(pos);
		}
	}

	public static Set<BlockPos> known() {
		return Collections.unmodifiableSet(KNOWN);
	}

	public static void clear() {
		KNOWN.clear();
	}
}
