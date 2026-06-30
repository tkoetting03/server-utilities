package com.serverutilities.client.screen.widget;

import com.serverutilities.npc.NpcConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class NpcRadiusSlider extends AbstractSliderButton {
	private final Component label;
	private final FloatReader reader;
	private final FloatWriter writer;

	public NpcRadiusSlider(int x, int y, int width, Component label, float initial, FloatReader reader, FloatWriter writer) {
		super(x, y, width, UiLayoutHelper.defaultButtonHeight(), Component.empty(), toSlider(initial));
		this.label = label;
		this.reader = reader;
		this.writer = writer;
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		setMessage(Component.literal(label.getString() + ": " + String.format("%.1fm", reader.read())));
	}

	@Override
	protected void applyValue() {
		writer.write(fromSlider(this.value));
		updateMessage();
	}

	private static double toSlider(float radius) {
		float clamped = NpcConfig.clampRadius(radius);
		return (clamped - NpcConfig.MIN_HEAD_RADIUS) / (NpcConfig.MAX_HEAD_RADIUS - NpcConfig.MIN_HEAD_RADIUS);
	}

	private static float fromSlider(double sliderValue) {
		return NpcConfig.clampRadius(
			NpcConfig.MIN_HEAD_RADIUS + (float) sliderValue * (NpcConfig.MAX_HEAD_RADIUS - NpcConfig.MIN_HEAD_RADIUS)
		);
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
