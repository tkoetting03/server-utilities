package com.hologrammenu.client.hologram;

import com.hologrammenu.HologramMenuMod;
import com.hologrammenu.hologram.HologramScale;
import com.hologrammenu.npc.NpcHologramLabels;
import com.hologrammenu.storage.StorageMenuHologramLabels;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

public final class LateHologramRenderer {
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final double MAX_RENDER_DISTANCE_SQ = 128.0D * 128.0D;

	private LateHologramRenderer() {
	}

	public static void register() {
		LevelRenderEvents.END_MAIN.register(LateHologramRenderer::render);
	}

	public static boolean isManagedHologram(Entity entity) {
		if (!(entity instanceof Display.TextDisplay display)) {
			return false;
		}
		return HologramClientRegistry.isHologram(display)
			|| display.entityTags().contains(HologramMenuMod.HOLOGRAM_TAG)
			|| display.entityTags().contains(StorageMenuHologramLabels.STORAGE_LABEL_TAG)
			|| display.entityTags().contains(NpcHologramLabels.NPC_LABEL_TAG);
	}

	private static void render(LevelRenderContext context) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) {
			return;
		}

		Camera camera = client.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.position();
		PoseStack poseStack = context.poseStack();
		MultiBufferSource.BufferSource bufferSource = context.bufferSource();

		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof Display.TextDisplay display) || !isManagedHologram(display)) {
				continue;
			}
			if (display.distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE_SQ) {
				continue;
			}
			renderDisplay(display, camera, cameraPos, poseStack, bufferSource, client.font);
		}

		bufferSource.endBatch();
	}

	private static void renderDisplay(
		Display.TextDisplay display,
		Camera camera,
		Vec3 cameraPos,
		PoseStack poseStack,
		MultiBufferSource.BufferSource bufferSource,
		Font font
	) {
		Component text = display.getText();
		if (text == null || text.getString().isBlank()) {
			return;
		}

		List<FormattedCharSequence> lines = font.split(text, Math.max(1, display.getLineWidth()));
		if (lines.isEmpty()) {
			return;
		}

		int width = 0;
		for (FormattedCharSequence line : lines) {
			width = Math.max(width, font.width(line));
		}

		int lineHeight = font.lineHeight + 1;
		int height = lines.size() * lineHeight - 1;
		byte flags = display.getFlags();
		Display.TextDisplay.Align alignment = Display.TextDisplay.getAlign(flags);
		boolean shadow = (flags & Display.TextDisplay.FLAG_SHADOW) != 0;
		boolean seeThrough = (flags & Display.TextDisplay.FLAG_SEE_THROUGH) != 0;
		int color = ((display.getTextOpacity() & 0xFF) << 24) | 0xFFFFFF;

		poseStack.pushPose();
		poseStack.translate(display.getX() - cameraPos.x(), display.getY() - cameraPos.y(), display.getZ() - cameraPos.z());
		poseStack.mulPose(centerBillboard(camera));
		Matrix4f pose = poseStack.last().pose();
		pose.rotate((float)Math.PI, 0.0F, 1.0F, 0.0F);
		float scale = HologramScale.getScale(display);
		pose.scale(-0.025F * scale, -0.025F * scale, -0.025F * scale);
		pose.translate(1.0F - width / 2.0F, -height, 0.0F);

		float y = 0.0F;
		for (FormattedCharSequence line : lines) {
			float x = switch (alignment) {
				case LEFT -> 0.0F;
				case RIGHT -> width - font.width(line);
				case CENTER -> width / 2.0F - font.width(line) / 2.0F;
			};
			font.drawInBatch(
				line,
				x,
				y,
				color,
				shadow,
				pose,
				bufferSource,
				Font.DisplayMode.SEE_THROUGH,
				0,
				FULL_BRIGHT
			);
			y += lineHeight;
		}

		poseStack.popPose();
	}

	private static Quaternionf centerBillboard(Camera camera) {
		return new Quaternionf().rotationYXZ(
			(float)(-Math.PI / 180.0D) * (camera.yRot() - 180.0F),
			(float)(Math.PI / 180.0D) * -camera.xRot(),
			0.0F
		);
	}
}
