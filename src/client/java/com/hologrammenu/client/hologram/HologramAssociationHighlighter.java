package com.hologrammenu.client.hologram;

import com.hologrammenu.client.config.ClientSettings;
import com.hologrammenu.hologram.HologramHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class HologramAssociationHighlighter {
	private static final int BLOCK_OUTLINE_COLOR = 0xFFFFFF4A;
	private static final int BLOCK_OUTLINE_GLOW_COLOR = 0x88FFF36A;
	private static final int ITEM_OUTLINE_COLOR = 0xFF55FFFF;
	private static final int ITEM_OUTLINE_GLOW_COLOR = 0x8855FFFF;
	private static final double MAX_HOLOGRAM_DISTANCE_SQ = 96.0D * 96.0D;
	private static final double ITEM_SEARCH_RADIUS = 1.25D;
	private static final int BLOCK_SEARCH_RADIUS = 1;
	private static final int BLOCK_SEARCH_DOWN = 64;
	private static final int BLOCK_SEARCH_UP = 2;

	private HologramAssociationHighlighter() {
	}

	public static void register() {
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(HologramAssociationHighlighter::render);
	}

	private static void render(LevelRenderContext context) {
		if (!ClientSettings.hologramEditModeEnabled && !ClientSettings.placementModeEnabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) {
			return;
		}

		ClientLevel level = client.level;
		Vec3 cameraPos = context.levelState().cameraRenderState.pos;
		VertexConsumer buffer = context.bufferSource().getBuffer(RenderTypes.lines());
		Set<BlockPos> outlinedBlocks = new HashSet<>();
		Set<Integer> outlinedItems = new HashSet<>();

		if (ClientSettings.placementModeEnabled) {
			targetBlock(client).ifPresent(pos -> {
				if (outlinedBlocks.add(pos)) {
					renderBlockOutline(context.poseStack(), buffer, level, pos, cameraPos);
				}
			});
		}

		if (ClientSettings.hologramEditModeEnabled) {
			for (Entity entity : level.entitiesForRendering()) {
				if (!(entity instanceof Display.TextDisplay display) || !LateHologramRenderer.isManagedHologram(display)) {
					continue;
				}
				if (display.distanceToSqr(cameraPos) > MAX_HOLOGRAM_DISTANCE_SQ) {
					continue;
				}

				Optional<ItemEntity> item = associatedItem(level, display).or(() -> nearestItem(level, display.position()));
				if (item.isPresent()) {
					ItemEntity itemEntity = item.get();
					if (outlinedItems.add(itemEntity.getId())) {
						renderEntityBox(context.poseStack(), buffer, itemEntity.getBoundingBox(), cameraPos, ITEM_OUTLINE_COLOR);
					}
					continue;
				}

				Optional<BlockPos> block = HologramHelper.associatedBlock(display).or(() -> nearestAssociatedBlock(level, display.position()));
				if (block.isPresent() && outlinedBlocks.add(block.get())) {
					renderBlockOutline(context.poseStack(), buffer, level, block.get(), cameraPos);
				}
			}
		}

		context.bufferSource().endBatch(RenderTypes.lines());
	}

	private static Optional<BlockPos> targetBlock(Minecraft client) {
		if (client.player == null || client.level == null) {
			return Optional.empty();
		}
		HitResult hit = client.player.pick(HologramHelper.WAND_MAX_DISTANCE, 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
			return Optional.empty();
		}
		BlockPos pos = blockHit.getBlockPos();
		return client.level.getBlockState(pos).isAir() ? Optional.empty() : Optional.of(pos.immutable());
	}

	private static Optional<ItemEntity> associatedItem(ClientLevel level, Display.TextDisplay display) {
		Optional<UUID> uuid = HologramHelper.associatedEntityUuid(display);
		if (uuid.isEmpty()) {
			return Optional.empty();
		}
		for (Entity entity : level.entitiesForRendering()) {
			if (entity instanceof ItemEntity item && item.getUUID().equals(uuid.get()) && !item.isRemoved()) {
				return Optional.of(item);
			}
		}
		return Optional.empty();
	}

	private static Optional<ItemEntity> nearestItem(ClientLevel level, Vec3 position) {
		AABB searchBox = new AABB(position, position).inflate(ITEM_SEARCH_RADIUS);
		ItemEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, searchBox, item -> !item.isRemoved())) {
			double distance = item.position().distanceToSqr(position);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = item;
			}
		}
		return Optional.ofNullable(best);
	}

	private static Optional<BlockPos> nearestAssociatedBlock(ClientLevel level, Vec3 position) {
		BlockPos origin = BlockPos.containing(position);
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int dy = BLOCK_SEARCH_UP; dy >= -BLOCK_SEARCH_DOWN; dy--) {
			for (int dx = -BLOCK_SEARCH_RADIUS; dx <= BLOCK_SEARCH_RADIUS; dx++) {
				for (int dz = -BLOCK_SEARCH_RADIUS; dz <= BLOCK_SEARCH_RADIUS; dz++) {
					BlockPos pos = origin.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);
					if (state.isAir()) {
						continue;
					}
					VoxelShape shape = state.getShape(level, pos);
					if (shape.isEmpty()) {
						continue;
					}
					double distance = Vec3.atCenterOf(pos).distanceToSqr(position);
					if (distance < bestDistance) {
						bestDistance = distance;
						best = pos.immutable();
					}
				}
			}
		}
		return Optional.ofNullable(best);
	}

	private static void renderBlockOutline(PoseStack poseStack, VertexConsumer buffer, ClientLevel level, BlockPos pos, Vec3 cameraPos) {
		renderBlockOutline(poseStack, buffer, level, pos, cameraPos, BLOCK_OUTLINE_COLOR, BLOCK_OUTLINE_GLOW_COLOR);
	}

	public static void renderBlockOutline(PoseStack poseStack, VertexConsumer buffer, ClientLevel level, BlockPos pos, Vec3 cameraPos, int color, int glowColor) {
		BlockState state = level.getBlockState(pos);
		VoxelShape shape = state.getShape(level, pos);
		if (shape.isEmpty()) {
			shape = Shapes.block();
		}
		ShapeRenderer.renderShape(
			poseStack,
			buffer,
			Shapes.create(new AABB(pos).inflate(0.03D)),
			-cameraPos.x(),
			-cameraPos.y(),
			-cameraPos.z(),
			glowColor,
			10.0F
		);
		ShapeRenderer.renderShape(
			poseStack,
			buffer,
			shape,
			pos.getX() - cameraPos.x(),
			pos.getY() - cameraPos.y(),
			pos.getZ() - cameraPos.z(),
			color,
			8.0F
		);
	}

	private static void renderEntityBox(PoseStack poseStack, VertexConsumer buffer, AABB box, Vec3 cameraPos, int color) {
		ShapeRenderer.renderShape(
			poseStack,
			buffer,
			Shapes.create(box.inflate(0.03D)),
			-cameraPos.x(),
			-cameraPos.y(),
			-cameraPos.z(),
			ITEM_OUTLINE_GLOW_COLOR,
			10.0F
		);
		ShapeRenderer.renderShape(
			poseStack,
			buffer,
			Shapes.create(box),
			-cameraPos.x(),
			-cameraPos.y(),
			-cameraPos.z(),
			color,
			8.0F
		);
	}
}
