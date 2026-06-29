package com.hologrammenu.client.npc;

import java.util.HashMap;
import java.util.Map;

public final class NpcEditorSessionState {
	public record ToggleState(
		boolean headFollowEnabled,
		float headFollowRadius,
		boolean containerEnabled,
		int containerSize,
		boolean particleEffectEnabled,
		String particleEffectId
	) {
	}

	private static final Map<Integer, ToggleState> TOGGLES_BY_ENTITY = new HashMap<>();

	private NpcEditorSessionState() {
	}

	public static void remember(
		int entityId,
		boolean headFollowEnabled,
		float headFollowRadius,
		boolean containerEnabled,
		int containerSize,
		boolean particleEffectEnabled,
		String particleEffectId
	) {
		TOGGLES_BY_ENTITY.put(
			entityId,
			new ToggleState(
				headFollowEnabled,
				headFollowRadius,
				containerEnabled,
				containerSize,
				particleEffectEnabled,
				particleEffectId == null ? "" : particleEffectId
			)
		);
	}

	public static ToggleState toggles(int entityId) {
		return TOGGLES_BY_ENTITY.get(entityId);
	}

	public static void clear(int entityId) {
		TOGGLES_BY_ENTITY.remove(entityId);
	}

	public static void clearAll() {
		TOGGLES_BY_ENTITY.clear();
	}
}
