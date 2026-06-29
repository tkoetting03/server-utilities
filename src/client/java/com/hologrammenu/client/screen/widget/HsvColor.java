package com.hologrammenu.client.screen.widget;

public final class HsvColor {
	private HsvColor() {
	}

	public static int toRgb(float hue, float saturation, float value) {
		float h = (hue % 360.0F + 360.0F) % 360.0F / 60.0F;
		float s = clamp01(saturation);
		float v = clamp01(value);

		int sector = (int) h;
		float fraction = h - sector;
		float p = v * (1.0F - s);
		float q = v * (1.0F - s * fraction);
		float t = v * (1.0F - s * (1.0F - fraction));

		float r;
		float g;
		float b;
		switch (sector) {
			case 0 -> {
				r = v;
				g = t;
				b = p;
			}
			case 1 -> {
				r = q;
				g = v;
				b = p;
			}
			case 2 -> {
				r = p;
				g = v;
				b = t;
			}
			case 3 -> {
				r = p;
				g = q;
				b = v;
			}
			case 4 -> {
				r = t;
				g = p;
				b = v;
			}
			default -> {
				r = v;
				g = p;
				b = q;
			}
		}

		return ((int) (r * 255.0F) << 16) | ((int) (g * 255.0F) << 8) | (int) (b * 255.0F);
	}

	public static float[] fromRgb(int rgb) {
		float red = ((rgb >> 16) & 0xFF) / 255.0F;
		float green = ((rgb >> 8) & 0xFF) / 255.0F;
		float blue = (rgb & 0xFF) / 255.0F;

		float max = Math.max(red, Math.max(green, blue));
		float min = Math.min(red, Math.min(green, blue));
		float delta = max - min;

		float hue = 0.0F;
		if (delta > 1.0E-4F) {
			if (max == red) {
				hue = 60.0F * (((green - blue) / delta) % 6.0F);
			} else if (max == green) {
				hue = 60.0F * (((blue - red) / delta) + 2.0F);
			} else {
				hue = 60.0F * (((red - green) / delta) + 4.0F);
			}
			if (hue < 0.0F) {
				hue += 360.0F;
			}
		}

		float saturation = max <= 0.0F ? 0.0F : delta / max;
		return new float[] { hue, saturation, max };
	}

	private static float clamp01(float value) {
		return Math.clamp(value, 0.0F, 1.0F);
	}
}
