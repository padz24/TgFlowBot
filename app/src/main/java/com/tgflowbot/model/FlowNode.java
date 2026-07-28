package com.tgflowbot.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlowNode {
    private final String id;
    private String label;
    private NodeType type;
    private float x;
    private float y;
    private Map<String, String> properties;

    public FlowNode(String label, NodeType type, float x, float y) {
        this.id = UUID.randomUUID().toString();
        this.label = label;
        this.type = type;
        this.x = x;
        this.y = y;
        this.properties = new HashMap<>();
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public Map<String, String> getProperties() { return properties; }
    public void setProperties(Map<String, String> properties) { this.properties = properties; }
    public void putProperty(String key, String value) { this.properties.put(key, value); }
    public String getProperty(String key) { return this.properties.get(key); }

    public FlowNode duplicate(float offsetX, float offsetY) {
        FlowNode copy = new FlowNode(label, type, x + offsetX, y + offsetY);
        copy.properties = new HashMap<>(properties);
        return copy;
    }
}
