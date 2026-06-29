package com.hologrammenu.client.head;

import com.hologrammenu.network.ModPackets;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class HeadPresetClientState {
	private static ModPackets.HeadPresetListResponsePayload latest;
	private static final List<Consumer<ModPackets.HeadPresetListResponsePayload>> LISTENERS = new CopyOnWriteArrayList<>();

	private HeadPresetClientState() {
	}

	public static ModPackets.HeadPresetListResponsePayload latest() {
		return latest;
	}

	public static void accept(ModPackets.HeadPresetListResponsePayload payload) {
		latest = payload;
		for (Consumer<ModPackets.HeadPresetListResponsePayload> listener : LISTENERS) {
			listener.accept(payload);
		}
	}

	public static Runnable addListener(Consumer<ModPackets.HeadPresetListResponsePayload> listener) {
		LISTENERS.add(listener);
		if (latest != null) {
			listener.accept(latest);
		}
		return () -> LISTENERS.remove(listener);
	}
}
