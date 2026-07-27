package com.tgflowbot.model;

import java.util.ArrayList;
import java.util.List;

public class Workflow {
    private final List<FlowNode> nodes;
    private final List<Connection> connections;

    public Workflow() {
        this.nodes = new ArrayList<>();
        this.connections = new ArrayList<>();
    }

    public List<FlowNode> getNodes() { return nodes; }
    public List<Connection> getConnections() { return connections; }

    public void addNode(FlowNode node) { nodes.add(node); }
    public void removeNode(FlowNode node) {
        nodes.remove(node);
        connections.removeIf(c ->
                c.getSourceNodeId().equals(node.getId()) ||
                c.getTargetNodeId().equals(node.getId()));
    }

    public void addConnection(Connection connection) {
        if (!hasConnection(connection.getSourceNodeId(), connection.getTargetNodeId(),
                connection.getConditionResult())) {
            connections.add(connection);
        }
    }

    public boolean hasConnection(String sourceNodeId, String targetNodeId) {
        for (Connection c : connections) {
            if (c.getSourceNodeId().equals(sourceNodeId)
                    && c.getTargetNodeId().equals(targetNodeId)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasConnection(String sourceNodeId, String targetNodeId, boolean conditionResult) {
        for (Connection c : connections) {
            if (c.getSourceNodeId().equals(sourceNodeId)
                    && c.getTargetNodeId().equals(targetNodeId)
                    && c.getConditionResult() == conditionResult) {
                return true;
            }
        }
        return false;
    }

    public void deduplicateConnections() {
        List<Connection> unique = new ArrayList<>();
        for (Connection c : connections) {
            boolean dup = false;
            for (Connection u : unique) {
                if (u.getSourceNodeId().equals(c.getSourceNodeId())
                        && u.getTargetNodeId().equals(c.getTargetNodeId())
                        && u.getConditionResult() == c.getConditionResult()) {
                    dup = true;
                    break;
                }
            }
            if (!dup) unique.add(c);
        }
        connections.clear();
        connections.addAll(unique);
    }
    public void removeConnection(Connection connection) { connections.remove(connection); }
    public FlowNode findNodeById(String id) {
        for (FlowNode n : nodes) {
            if (n.getId().equals(id)) return n;
        }
        return null;
    }
}
