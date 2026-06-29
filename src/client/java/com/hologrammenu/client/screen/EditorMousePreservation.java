package com.hologrammenu.client.screen;

import com.hologrammenu.client.mixin.accessor.MouseHandlerAccessor;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;

public final class EditorMousePreservation {
	private static final int RESTORE_TICKS = 4;

	private static double savedX;
	private static double savedY;
	private static int remainingRestoreTicks;

	private EditorMousePreservation() {
	}

	public static void arm() {
		captureCurrent();
		remainingRestoreTicks = RESTORE_TICKS;
	}

	public static void runPreservingMouse(Runnable action) {
		arm();
		action.run();
		restoreNow();
	}

	public static void tick() {
		if (remainingRestoreTicks > 0) {
			restoreNow();
			remainingRestoreTicks--;
		}
	}

	public static void restoreIfPending() {
		if (remainingRestoreTicks > 0) {
			restoreNow();
		}
	}

	private static void captureCurrent() {
		MouseHandler mouseHandler = Minecraft.getInstance().mouseHandler;
		if (mouseHandler == null) {
			return;
		}
		savedX = mouseHandler.xpos();
		savedY = mouseHandler.ypos();
	}

	private static void restoreNow() {
		Minecraft client = Minecraft.getInstance();
		if (client.mouseHandler == null) {
			return;
		}

		Window window = client.getWindow();
		GLFW.glfwSetCursorPos(window.handle(), savedX, savedY);
		MouseHandlerAccessor accessor = (MouseHandlerAccessor) client.mouseHandler;
		accessor.hologrammenu$setXpos(savedX);
		accessor.hologrammenu$setYpos(savedY);
		accessor.hologrammenu$setIgnoreFirstMove(true);
	}
}
