package com.hologrammenu.client.screen;

import com.hologrammenu.client.screen.widget.StorageMenuEditorMetrics;
import com.hologrammenu.client.screen.widget.StorageMenuNumberBox;
import com.hologrammenu.client.screen.widget.UiScale;
import com.hologrammenu.storage.StorageMenuSlotConfig;
import com.hologrammenu.storage.StorageMenuSlotType;
import com.hologrammenu.storage.StorageMenuViewContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;

public final class StorageMenuInventoryTree {
	public static final int NODE_SIZE = StorageMenuNumberBox.SIZE;
	public static final int NODE_LABEL_HEIGHT = StorageMenuNumberBox.LABEL_HEIGHT;
	public static final int ROW_HEIGHT = UiScale.s(22);
	public static final int INNER_PADDING = UiScale.s(6);
	public static final int INNER_WIDTH = StorageMenuEditorMetrics.PANEL_WIDTH - UiScale.s(12);

	public record MenuDraft(String title, Map<Integer, StorageMenuSlotConfig> slots) {
	}

	public static final class Node {
		public final int number;
		public final StorageMenuViewContext context;
		public final int parentNumber;
		public final List<Integer> childNumbers = new ArrayList<>();
		public int layoutX;
		public int layoutY;

		private Node(int number, StorageMenuViewContext context, int parentNumber) {
			this.number = number;
			this.context = context;
			this.parentNumber = parentNumber;
		}
	}

	public static int numberForContext(List<Node> nodes, StorageMenuViewContext context) {
		for (Node node : nodes) {
			if (node.context.equals(context)) {
				return node.number;
			}
		}
		return 1;
	}

	private StorageMenuInventoryTree() {
	}

	public static String draftKey(StorageMenuViewContext context) {
		return context.isRoot() ? "" : context.subMenuId();
	}

	public static List<Node> build(StorageMenuViewContext root, Function<String, MenuDraft> draftLookup) {
		List<Node> nodes = new ArrayList<>();
		Map<String, Integer> numberByKey = new HashMap<>();
		Map<Integer, Node> nodeByNumber = new HashMap<>();

		Node rootNode = new Node(1, root, 0);
		nodes.add(rootNode);
		nodeByNumber.put(1, rootNode);
		numberByKey.put("", 1);

		Queue<String> queue = new ArrayDeque<>();
		queue.add("");

		int nextNumber = 2;
		while (!queue.isEmpty()) {
			String key = queue.poll();
			MenuDraft draft = draftLookup.apply(key);
			if (draft == null) {
				continue;
			}

			Node parent = nodeByNumber.get(numberByKey.get(key));
			for (StorageMenuSlotConfig config : draft.slots().values()) {
				if (config.type() != StorageMenuSlotType.LINK || !config.hasSubMenu()) {
					continue;
				}
				String childId = config.subMenuId();
				if (numberByKey.containsKey(childId)) {
					continue;
				}

				int number = nextNumber++;
				numberByKey.put(childId, number);
				Node child = new Node(number, root.withSubMenu(childId), parent.number);
				nodes.add(child);
				nodeByNumber.put(number, child);
				parent.childNumbers.add(number);
				queue.add(childId);
			}
		}

		layout(nodes, nodeByNumber);
		return nodes;
	}

	private static void layout(List<Node> nodes, Map<Integer, Node> nodeByNumber) {
		if (nodes.isEmpty()) {
			return;
		}
		layoutSubtree(nodeByNumber.get(1), nodeByNumber, 0, 0, INNER_WIDTH);
	}

	private static void layoutSubtree(Node node, Map<Integer, Node> nodeByNumber, int depth, int left, int right) {
		int nodeSize = NODE_SIZE;
		int rowHeight = ROW_HEIGHT;
		int padding = INNER_PADDING;

		node.layoutX = left + (right - left - nodeSize) / 2;
		node.layoutY = padding + NODE_LABEL_HEIGHT + depth * rowHeight;

		if (node.childNumbers.isEmpty()) {
			return;
		}

		int childCount = node.childNumbers.size();
		int slice = (right - left) / childCount;
		for (int index = 0; index < childCount; index++) {
			Node child = nodeByNumber.get(node.childNumbers.get(index));
			layoutSubtree(child, nodeByNumber, depth + 1, left + index * slice, left + (index + 1) * slice);
		}
	}
}
