package com.serverutilities.particle;

public record ParticlePresetEntry(String id, String name, String category, int previewColor) {
	public static final String NONE_ID = "";

	public boolean isNone() {
		return id == null || id.isBlank();
	}
}
