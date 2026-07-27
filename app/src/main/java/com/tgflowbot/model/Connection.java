package com.tgflowbot.model;

import java.util.UUID;

public class Connection {
    private final String id;
    private final String sourceNodeId;
    private final String targetNodeId;
    private boolean conditionResult = true;

    public Connection(String sourceNodeId, String targetNodeId) {
        this.id = UUID.randomUUID().toString();
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
    }

    public Connection(String sourceNodeId, String targetNodeId, boolean conditionResult) {
        this.id = UUID.randomUUID().toString();
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.conditionResult = conditionResult;
    }

    public String getId() { return id; }
    public String getSourceNodeId() { return sourceNodeId; }
    public String getTargetNodeId() { return targetNodeId; }
    public boolean getConditionResult() { return conditionResult; }
    public void setConditionResult(boolean conditionResult) { this.conditionResult = conditionResult; }
}
