package com.hologrammenu.client.screen.widget;

public final class UiScale {
	public static final float FACTOR = 0.75F;
	public static final float TEXT_SCALE = 0.75F;

	/** Vanilla inventory slot size before UI scaling. */
	public static final int VANILLA_SLOT = 16;
	/** Container editor slot size, 25% smaller than the previous 16px base. */
	public static final int SLOT_BASE = 12;
	/** Inventory tree node size, 25% smaller than the previous 18px base. */
	public static final int NUMBER_BOX_BASE = 14;
	/** Shop picker grid slot size, 25% smaller than the previous 18px base. */
	public static final int PICKER_SLOT_BASE = 14;
	/** Head preset picker grid slot size — larger than the shop picker for easier browsing. */
	public static final int HEAD_PRESET_SLOT_BASE = 32;

	private UiScale() {
	}

	public static int s(int pixels) {
		if (pixels == 0) {
			return 0;
		}
		return Math.max(1, Math.round(pixels * FACTOR));
	}
}
