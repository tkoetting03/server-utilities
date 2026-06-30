package com.serverutilities.client.storage;

import com.serverutilities.client.config.ClientSettings;
import com.serverutilities.client.hologram.HologramAssociationHighlighter;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public final class StorageMenuAssociationHighlighter {
	private static final int BLOCK_OUTLINE_COLOR = 0xFF55FF7A;
	private static final int BLOCK_OUTLINE_GLOW_COLOR = 0x8855FF7A;
	private static final double MAX_DISTANCE_SQ = 96.0D * 96.0D;

	private StorageMenuAssociationHighlighter() {
	}

	public static void register() {
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(StorageMenuAssociationHighlighter::render);
	}

	private static void render(LevelRenderContext context) {
		if (!ClientSettings.storagePlacementModeEnabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) {
			return;
		}

		ClientLevel level = client.level;
		Vec3 cameraPos = context.levelState().cameraRenderState.pos;
		VertexConsumer buffer = context.bufferSource().getBuffer(RenderTypes.lines());
		Set<BlockPos> outlined = new HashSet<>();

		targetBlock(client).ifPresent(pos -> renderBlock(context, level, cameraPos, buffer, outlined, pos));
		for (BlockPos pos : StorageMenuAssociatedBlocks.known()) {
			renderBlock(context, level, cameraPos, buffer, outlined, pos);
		}

		context.bufferSource().endBatch(RenderTypes.lines());
	}

	private static void renderBlock(
		LevelRenderContext context,
		ClientLevel level,
		Vec3 cameraPos,
		VertexConsumer buffer,
		Set<BlockPos> outlined,
		BlockPos pos
	) {
		if (!outlined.add(pos) || level.getBlockState(pos).isAir() || Vec3.atCenterOf(pos).distanceToSqr(cameraPos) > MAX_DISTANCE_SQ) {
			return;
		}
		HologramAssociationHighlighter.renderBlockOutline(
			context.poseStack(),
			buffer,
			level,
			pos,
			cameraPos,
			BLOCK_OUTLINE_COLOR,
			BLOCK_OUTLINE_GLOW_COLOR
		);
	}

	private static java.util.Optional<BlockPos> targetBlock(Minecraft client) {
		double reach = client.player.blockInteractionRange();
		HitResult hit = client.player.pick(reach, 0.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
			return java.util.Optional.empty();
		}
		BlockPos pos = blockHit.getBlockPos();
		return client.level.getBlockState(pos).isAir() ? java.util.Optional.empty() : java.util.Optional.of(pos.immutable());
	}
}
