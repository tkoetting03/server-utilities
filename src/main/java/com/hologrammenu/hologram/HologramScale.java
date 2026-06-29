package com.hologrammenu.hologram;

import com.hologrammenu.mixin.accessor.DisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

public final class HologramScale {
	public static final float MIN = 0.25F;
	public static final float MAX = 4.0F;
	public static final float DEFAULT = 1.0F;

	private static final String SCALE_PREFIX = "hologrammenu:hologram_scale:";
	private static final String LEGACY_TEXT_SCALE_PREFIX = "hologrammenu:text_scale:";

	private HologramScale() {
	}

	public static float getScale(Display display) {
		Float stored = readScaleTag(display, SCALE_PREFIX);
		if (stored != null) {
			Float legacyText = readScaleTag(display, LEGACY_TEXT_SCALE_PREFIX);
			float scale = legacyText != null ? stored * legacyText : stored;
			return clamp(scale);
		}
		return clamp(readUniformScale(display));
	}

	public static void apply(Display display, float scale) {
		float clamped = clamp(scale);
		writeScaleTag(display, SCALE_PREFIX, clamped);
		display.entityTags().removeIf(tag -> tag.startsWith(LEGACY_TEXT_SCALE_PREFIX));

		Transformation current = DisplayAccessor.hologrammenu$createTransformation(display.getEntityData());
		Vector3f uniformScale = new Vector3f(clamped, clamped, clamped);
		Transformation updated = new Transformation(
			current.translation(),
			current.leftRotation(),
			uniformScale,
			current.rightRotation()
		);
		((DisplayAccessor) display).hologrammenu$setTransformation(updated);
	}

	public static float readUniformScale(Display display) {
		Transformation transformation = DisplayAccessor.hologrammenu$createTransformation(display.getEntityData());
		var scale = transformation.scale();
		return (scale.x() + scale.y() + scale.z()) / 3.0F;
	}

	public static float clamp(float value) {
		return Math.clamp(value, MIN, MAX);
	}

	public static double toSliderValue(float scale) {
		float clamped = clamp(scale);
		return (clamped - MIN) / (MAX - MIN);
	}

	public static float fromSliderValue(double sliderValue) {
		return clamp(MIN + (float) sliderValue * (MAX - MIN));
	}

	public static String format(float scale) {
		return String.format("%.2fx", scale);
	}

	private static Float readScaleTag(Entity entity, String prefix) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(prefix)) {
				try {
					return clamp(Float.parseFloat(tag.substring(prefix.length())));
				} catch (NumberFormatException ignored) {
					return null;
				}
			}
		}
		return null;
	}

	private static void writeScaleTag(Entity entity, String prefix, float value) {
		entity.entityTags().removeIf(tag -> tag.startsWith(prefix));
		entity.addTag(prefix + value);
	}
}
