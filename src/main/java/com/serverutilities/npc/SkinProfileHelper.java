package com.serverutilities.npc;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import com.serverutilities.mixin.accessor.MannequinAccessor;

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

	public static void resolveSkinAsync(MinecraftServer server, Mannequin mannequin, String skinValue) {
		if (skinValue == null || skinValue.isBlank()) {
			return;
		}
		ResolvableProfile unresolved = ResolvableProfile.createUnresolved(skinValue.trim());
		((MannequinAccessor) mannequin).serverutilities$setProfile(unresolved);

		unresolved.resolveProfile(server.services().profileResolver()).thenAcceptAsync(resolved -> {
			if (resolved != null && !mannequin.isRemoved()) {
				com.mojang.authlib.GameProfile profile = (com.mojang.authlib.GameProfile) resolved;
				((MannequinAccessor) mannequin).serverutilities$setProfile(ResolvableProfile.createResolved(profile));
			}
		}, server);
	}
}
