package com.hologrammenu.head;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class HeadTextureEncoding {
	private HeadTextureEncoding() {
	}

	public static String toBase64(String textureHash) {
		if (textureHash == null || textureHash.isBlank()) {
			return "";
		}
		String payload = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/"
			+ textureHash.trim()
			+ "\"}}}";
		return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
	}
}
