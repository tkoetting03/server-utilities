package com.hologrammenu.npc;

import net.minecraft.world.item.component.ResolvableProfile;

import java.util.Optional;

public final class SkinProfileHelper {
	private SkinProfileHelper() {
	}

	public static Optional<ResolvableProfile> resolveSkin(String skinValue) {
		if (skinValue == null || skinValue.isBlank()) {
			return Optional.empty();
		}
		return Optional.of(ResolvableProfile.createUnresolved(skinValue.trim()));
	}
}
