package com.serverutilities.client.screen.widget;

import com.serverutilities.client.screen.StorageMenuInventoryTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;

public class StorageMenuInventoryTreeWidget extends AbstractWidget {
	public static final int PANEL_WIDTH = StorageMenuEditorPanelWidget.PANEL_WIDTH;
	public static final int NODE_SIZE = StorageMenuInventoryTree.NODE_SIZE;
	public static final int ROW_HEIGHT = StorageMenuInventoryTree.ROW_HEIGHT;
	public static final int INNER_PADDING = StorageMenuInventoryTree.INNER_PADDING;
	public static final int INNER_WIDTH = StorageMenuInventoryTree.INNER_WIDTH;
	public static final int OUTER_PADDING = UiScale.s(6);
	public static final int TITLE_HEIGHT = UiScale.s(12);

	private final List<StorageMenuInventoryTree.Node> nodes;

	public StorageMenuInventoryTreeWidget(int x, int y, int height, List<StorageMenuInventoryTree.Node> nodes) {
		super(x, y, PANEL_WIDTH, height, Component.translatable("screen.serverutilities.storage_menu.inventory_tree"));
		this.nodes = nodes;
		this.active = false;
	}

	public static int computeHeight(List<StorageMenuInventoryTree.Node> nodes) {
		if (nodes.isEmpty()) {
			return TITLE_HEIGHT + OUTER_PADDING * 2 + ROW_HEIGHT;
		}
		int maxDepth = 0;
		for (StorageMenuInventoryTree.Node node : nodes) {
			int depth = depthOf(node, nodes);
			maxDepth = Math.max(maxDepth, depth);
		}
		int innerHeight = INNER_PADDING * 2 + StorageMenuInventoryTree.NODE_LABEL_HEIGHT + (maxDepth + 1) * ROW_HEIGHT;
		return TITLE_HEIGHT + OUTER_PADDING * 2 + innerHeight;
	}

	private static int depthOf(StorageMenuInventoryTree.Node node, List<StorageMenuInventoryTree.Node> nodes) {
		int depth = 0;
		int parent = node.parentNumber;
		while (parent > 0) {
			depth++;
			int currentParent = parent;
			parent = 0;
			for (StorageMenuInventoryTree.Node candidate : nodes) {
				if (candidate.number == currentParent) {
					parent = candidate.parentNumber;
					break;
				}
			}
		}
		return depth;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		int right = x + width;
		int bottom = y + height;

		graphics.fill(x, y, right, bottom, 0xF0101010);
		graphics.fill(x, y, right, y + 1, 0xFF6A6A6A);
		graphics.fill(x, bottom - 1, right, bottom, 0xFF2A2A2A);
		graphics.fill(x, y, x + 1, bottom, 0xFF6A6A6A);
		graphics.fill(right - 1, y, right, bottom, 0xFF2A2A2A);

		UiScaleText.draw(graphics, Minecraft.getInstance().font, getMessage(), x + StorageMenuEditorMetrics.PANEL_PADDING, y + UiScale.s(3), 0xFFFFFF);

		int innerX = x + OUTER_PADDING;
		int innerY = y + TITLE_HEIGHT + OUTER_PADDING;
		int innerRight = innerX + INNER_WIDTH;
		int innerBottom = bottom - OUTER_PADDING;

		graphics.fill(innerX, innerY, innerRight, innerBottom, 0xF0181818);
		graphics.fill(innerX, innerY, innerRight, innerY + 1, 0xFF4A4A4A);
		graphics.fill(innerX, innerBottom - 1, innerRight, innerBottom, 0xFF2A2A2A);
		graphics.fill(innerX, innerY, innerX + 1, innerBottom, 0xFF4A4A4A);
		graphics.fill(innerRight - 1, innerY, innerRight, innerBottom, 0xFF2A2A2A);

		for (StorageMenuInventoryTree.Node node : nodes) {
			if (node.parentNumber <= 0) {
				continue;
			}
			StorageMenuInventoryTree.Node parent = findNode(node.parentNumber);
			if (parent == null) {
				continue;
			}
			drawConnector(graphics, innerX, innerY, parent, node);
		}
	}

	private void drawConnector(
		GuiGraphicsExtractor graphics,
		int innerX,
		int innerY,
		StorageMenuInventoryTree.Node parent,
		StorageMenuInventoryTree.Node child
	) {
		int parentCenterX = innerX + parent.layoutX + NODE_SIZE / 2;
		int parentBottomY = innerY + parent.layoutY + NODE_SIZE;
		int childCenterX = innerX + child.layoutX + NODE_SIZE / 2;
		int childTopY = innerY + child.layoutY;
		int midY = parentBottomY + (childTopY - parentBottomY) / 2;
		int lineColor = 0xFF8A8A8A;

		graphics.fill(parentCenterX, parentBottomY, parentCenterX + 1, midY + 1, lineColor);
		int horizontalLeft = Math.min(parentCenterX, childCenterX);
		int horizontalRight = Math.max(parentCenterX, childCenterX);
		graphics.fill(horizontalLeft, midY, horizontalRight + 1, midY + 1, lineColor);
		graphics.fill(childCenterX, midY, childCenterX + 1, childTopY + 1, lineColor);
	}

	private StorageMenuInventoryTree.Node findNode(int number) {
		for (StorageMenuInventoryTree.Node node : nodes) {
			if (node.number == number) {
				return node;
			}
		}
		return null;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
	}
}
