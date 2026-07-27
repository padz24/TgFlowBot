package com.tgflowbot.view;

import android.graphics.PointF;
import android.graphics.RectF;

public class NodePort {
    public enum PortType { INPUT, OUTPUT }

    private final String nodeId;
    private final PortType type;
    private final RectF bounds;

    public NodePort(String nodeId, PortType type, RectF bounds) {
        this.nodeId = nodeId;
        this.type = type;
        this.bounds = bounds;
    }

    public String getNodeId() { return nodeId; }
    public PortType getType() { return type; }
    public RectF getBounds() { return bounds; }
    public PointF getCenter() {
        return new PointF(bounds.centerX(), bounds.centerY());
    }

    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }
}
