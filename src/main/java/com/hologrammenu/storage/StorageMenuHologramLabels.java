package com.hologrammenu.storage;

import com.hologrammenu.HologramMenuMod;
import com.hologrammenu.hologram.HologramHelper;
import com.hologrammenu.hologram.HologramSync;
import net.minecraft.core.BlockPos;
import com.hologrammenu.text.TextFormats;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class StorageMenuHologramLabels {
	public static final String STORAGE_LABEL_TAG = HologramMenuMod.MOD_ID + ":storage_label";
	private static final double LABEL_Y_OFFSET = 0.85D;

	private StorageMenuHologramLabels() {
	}

	public static void sync(ServerLevel level, BlockPos pos, String text, boolean enabled) {
		sync(level, pos, text, enabled, StorageMenuHologramSettings.DEFAULT);
	}

	public static void sync(ServerLevel level, BlockPos pos, String text, boolean enabled, StorageMenuHologramSettings settings) {
		remove(level, pos);
		if (!enabled || text == null || text.isBlank()) {
			return;
		}

		StorageMenuHologramSettings resolved = settings == null ? StorageMenuHologramSettings.DEFAULT : settings;
		Vec3 position = Vec3.atCenterOf(pos).add(0.0D, LABEL_Y_OFFSET + resolved.heightOffset(), 0.0D);
		Display.TextDisplay display = HologramHelper.createStorageLabel(level, position, TextFormats.toComponent(text.trim()), resolved.scale());
		HologramHelper.tagAssociatedBlock(display, pos);
	}

	public static void remove(ServerLevel level, BlockPos pos) {
		BlockPos immutablePos = pos.immutable();
		AABB box = new AABB(immutablePos).inflate(0.5D, 1.5D, 0.5D);
		for (Display.TextDisplay display : level.getEntities(EntityType.TEXT_DISPLAY, box, StorageMenuHologramLabels::isStorageLabel)) {
			if (display.blockPosition().distSqr(immutablePos) <= 2) {
				HologramSync.untrack(level, display);
				display.discard();
			}
		}
	}

	public static boolean isStorageLabel(Display.TextDisplay display) {
		return display.entityTags().contains(STORAGE_LABEL_TAG);
	}
}
