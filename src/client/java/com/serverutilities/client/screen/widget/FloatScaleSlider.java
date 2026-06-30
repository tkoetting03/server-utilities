package com.serverutilities.client.screen.widget;

import com.serverutilities.hologram.HologramScale;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class FloatScaleSlider extends AbstractSliderButton {
	private final Component label;
	private final FloatReader reader;
	private final FloatWriter writer;
	private final Runnable onChanged;

	public FloatScaleSlider(int x, int y, int width, Component label, float initial, FloatReader reader, FloatWriter writer, Runnable onChanged) {
		this(x, y, width, UiLayoutHelper.defaultButtonHeight(), label, initial, reader, writer, onChanged);
	}

	public FloatScaleSlider(int x, int y, int width, int height, Component label, float initial, FloatReader reader, FloatWriter writer, Runnable onChanged) {
		super(x, y, width, height, Component.empty(), HologramScale.toSliderValue(initial));
		this.label = label;
		this.reader = reader;
		this.writer = writer;
		this.onChanged = onChanged;
		updateMessage();
	}

	public float getScale() {
		return HologramScale.fromSliderValue(this.value);
	}

	@Override
	protected void updateMessage() {
		setMessage(Component.literal(label.getString() + ": " + HologramScale.format(reader.read())));
	}

	@Override
	protected void applyValue() {
		writer.write(getScale());
		updateMessage();
		onChanged.run();
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
