package com.serverutilities.client.screen.widget;

import com.serverutilities.hologram.HologramLineStack;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class HologramHeightSlider extends AbstractSliderButton {
	private final Component label;
	private final FloatReader reader;
	private final FloatWriter writer;
	private final Runnable onChanged;

	public HologramHeightSlider(int x, int y, int width, Component label, float initial, FloatReader reader, FloatWriter writer, Runnable onChanged) {
		this(x, y, width, UiLayoutHelper.defaultButtonHeight(), label, initial, reader, writer, onChanged);
	}

	public HologramHeightSlider(int x, int y, int width, int height, Component label, float initial, FloatReader reader, FloatWriter writer, Runnable onChanged) {
		super(x, y, width, height, Component.empty(), toSlider(initial));
		this.label = label;
		this.reader = reader;
		this.writer = writer;
		this.onChanged = onChanged;
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		setMessage(Component.literal(label.getString() + ": " + String.format("%.2fm", reader.read())));
	}

	@Override
	protected void applyValue() {
		writer.write(fromSlider(this.value));
		updateMessage();
		onChanged.run();
	}

	private static double toSlider(float offset) {
		float clamped = HologramLineStack.clampHeightOffset(offset);
		float range = HologramLineStack.MAX_HEIGHT_OFFSET - HologramLineStack.MIN_HEIGHT_OFFSET;
		return (clamped - HologramLineStack.MIN_HEIGHT_OFFSET) / range;
	}

	private static float fromSlider(double sliderValue) {
		float range = HologramLineStack.MAX_HEIGHT_OFFSET - HologramLineStack.MIN_HEIGHT_OFFSET;
		return HologramLineStack.clampHeightOffset(HologramLineStack.MIN_HEIGHT_OFFSET + (float) sliderValue * range);
	}

	@FunctionalInterface
	public interface FloatReader {
		float read();
	}

	@FunctionalInterface
	public interface FloatWriter {
		void write(float value);
	}
}
