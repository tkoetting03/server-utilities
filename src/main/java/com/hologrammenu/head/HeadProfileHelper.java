package com.hologrammenu.head;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public final class HeadProfileHelper {
	private HeadProfileHelper() {
	}

	public static Optional<ResolvableProfile> resolveSkin(String skinValue) {
		if (skinValue == null || skinValue.isBlank()) {
			return Optional.empty();
		}
		String trimmed = skinValue.trim();
		Optional<String> headId = HeadPresetIds.parseId(trimmed);
		if (headId.isPresent()) {
			return HeadPresetCatalog.getEntry(headId.get())
				.flatMap(entry -> fromTextureBase64(entry.name(), entry.base64()))
				.or(() -> HeadPresetCatalog.getBase64(headId.get())
					.flatMap(base64 -> fromTextureBase64(headId.get(), base64)));
		}
		return Optional.of(ResolvableProfile.createUnresolved(trimmed));
	}

	public static Optional<ResolvableProfile> fromTextureBase64(String name, String base64) {
		Optional<String> normalizedBase64 = normalizeTextureBase64(base64);
		if (normalizedBase64.isEmpty()) {
			return Optional.empty();
		}
		String profileName = sanitizeProfileName(name);
		Property texture = new Property("textures", normalizedBase64.get());
		PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", texture));
		GameProfile profile = new GameProfile(deterministicUuid(normalizedBase64.get()), profileName, properties);
		return Optional.of(ResolvableProfile.createResolved(profile));
	}

	private static Optional<String> normalizeTextureBase64(String textureValue) {
		if (textureValue == null || textureValue.isBlank()) {
			return Optional.empty();
		}
		String trimmed = textureValue.trim();
		Optional<String> hash = extractTextureHash(trimmed);
		if (hash.isPresent()) {
			return Optional.of(HeadTextureEncoding.toBase64(hash.get()));
		}
		Optional<String> decodedJson = decodeBase64(trimmed);
		if (decodedJson.isEmpty()) {
			return Optional.empty();
		}
		return extractTextureHash(decodedJson.get())
			.map(HeadTextureEncoding::toBase64);
	}

	private static Optional<String> decodeBase64(String value) {
		try {
			byte[] decoded = Base64.getDecoder().decode(value);
			return Optional.of(new String(decoded, StandardCharsets.UTF_8));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	private static Optional<String> extractTextureHash(String value) {
		String trimmed = value == null ? "" : value.trim();
		if (trimmed.isEmpty()) {
			return Optional.empty();
		}
		if (isTextureHash(trimmed)) {
			return Optional.of(trimmed);
		}
		int marker = trimmed.indexOf("textures.minecraft.net/texture/");
		if (marker >= 0) {
			String suffix = trimmed.substring(marker + "textures.minecraft.net/texture/".length());
			int end = 0;
			while (end < suffix.length() && isTextureHashCharacter(suffix.charAt(end))) {
				end++;
			}
			if (end > 0) {
				return Optional.of(suffix.substring(0, end));
			}
		}
		if (!trimmed.startsWith("{")) {
			return Optional.empty();
		}
		try {
			JsonElement rootElement = JsonParser.parseString(trimmed);
			if (!rootElement.isJsonObject()) {
				return Optional.empty();
			}
			JsonObject root = rootElement.getAsJsonObject();
			JsonObject textures = root.has("textures") && root.get("textures").isJsonObject()
				? root.getAsJsonObject("textures")
				: null;
			JsonObject skin = textures != null && textures.has("SKIN") && textures.get("SKIN").isJsonObject()
				? textures.getAsJsonObject("SKIN")
				: null;
			if (skin == null || !skin.has("url")) {
				return Optional.empty();
			}
			return extractTextureHash(skin.get("url").getAsString());
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	private static boolean isTextureHash(String value) {
		if (value.length() < 32 || value.length() > 128) {
			return false;
		}
		for (int index = 0; index < value.length(); index++) {
			if (!isTextureHashCharacter(value.charAt(index))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isTextureHashCharacter(char character) {
		return (character >= 'a' && character <= 'f')
			|| (character >= 'A' && character <= 'F')
			|| (character >= '0' && character <= '9');
	}

	private static String sanitizeProfileName(String name) {
		if (name == null || name.isBlank()) {
			return "Head";
		}
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < name.length() && builder.length() < 16; index++) {
			char character = name.charAt(index);
			if ((character >= 'a' && character <= 'z')
				|| (character >= 'A' && character <= 'Z')
				|| (character >= '0' && character <= '9')
				|| character == '_') {
				builder.append(character);
			}
		}
		return builder.isEmpty() ? "Head" : builder.toString();
	}

	private static UUID deterministicUuid(String base64) {
		return UUID.nameUUIDFromBytes(("hologrammenu:head:" + base64).getBytes(StandardCharsets.UTF_8));
	}
}
