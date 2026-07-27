package com.tgflowbot.view;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.tgflowbot.model.Connection;
import com.tgflowbot.model.FlowNode;
import com.tgflowbot.model.NodeType;
import com.tgflowbot.model.Workflow;

import java.util.ArrayList;
import java.util.List;

public class FlowCanvasView extends View {

    private static final float GRID_SIZE = 40f;
    private static final float MIN_ZOOM = 0.3f;
    private static final float MAX_ZOOM = 3.0f;

    private Workflow workflow;
    private final List<ViewNode> viewNodes = new ArrayList<>();
    private ViewNode selectedNode;
    private ViewNode dragNode;
    private float dragOffsetX, dragOffsetY;
    private float lastTouchX, lastTouchY;
    private boolean isPanning = false;

    private ViewNode connectSourceNode;
    private boolean isConnecting = false;
    private PointF connectEndPoint;
    private NodePort connectSourcePort;

    private float scaleFactor = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private Handler longPressHandler;
    private boolean longPressFired;

    private final Paint gridPaint;
    private final Paint connectionPaint;
    private final Paint connectionDragPaint;
    private final Paint selectedStrokePaint;
    private final Paint dotPaint;
    private final Paint labelPaint;
    private boolean dotAnimating = false;
    private float dotProgress = 0f;
    private final Handler dotHandler = new Handler();
    private final Runnable dotRunnable = new Runnable() {
        @Override
        public void run() {
            dotProgress += 0.06f;
            if (dotProgress > 1f) {
                dotAnimating = false;
                invalidate();
                return;
            }
            invalidate();
            dotHandler.postDelayed(this, 20);
        }
    };
    private final Paint dotGlowPaint;

    private PointF dropTargetPos;
    private NodeType dropNodeType;

    private OnNodeActionListener nodeActionListener;

    public interface OnNodeActionListener {
        void onNodeSelected(FlowNode node);
        void onNodeDoubleTap(FlowNode node);
        void onNodeLongPress(FlowNode node);
        void onConnectionCreated(Connection connection);
        void onNodeDropped(FlowNode node);
        void onCanvasLongPress();
    }

    public FlowCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setLongClickable(true);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setAlpha(60);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{4f, 4f}, 0f));

        connectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        connectionPaint.setColor(0xFF1976D2);
        connectionPaint.setStrokeWidth(3f);
        connectionPaint.setStyle(Paint.Style.STROKE);
        connectionPaint.setPathEffect(new DashPathEffect(new float[]{8f, 4f}, 0f));

        connectionDragPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        connectionDragPaint.setColor(0xFF4CAF50);
        connectionDragPaint.setStrokeWidth(3f);
        connectionDragPaint.setStyle(Paint.Style.STROKE);
        connectionDragPaint.setAlpha(180);

        selectedStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedStrokePaint.setColor(0xFFFF5722);
        selectedStrokePaint.setStyle(Paint.Style.STROKE);
        selectedStrokePaint.setStrokeWidth(4f);

        dotGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotGlowPaint.setColor(0xFFFFEB3B);
        dotGlowPaint.setStyle(Paint.Style.FILL);
        dotGlowPaint.setAlpha(60);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(0xFFFFEB3B);
        dotPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(24f);
        labelPaint.setFakeBoldText(true);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(0xFF1976D2);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        longPressHandler = new Handler();

        this.workflow = new Workflow();

        setOnDragListener(new CanvasDragListener());
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent e) {
            longPressFired = true;
            float tx = (e.getX() - offsetX) / scaleFactor;
            float ty = (e.getY() - offsetY) / scaleFactor;
            ViewNode node = findNodeAt(tx, ty);
            if (node != null && nodeActionListener != null) {
                selectedNode = node;
                invalidate();
                nodeActionListener.onNodeLongPress(node.getData());
            } else if (nodeActionListener != null) {
                nodeActionListener.onCanvasLongPress();
            }
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float tx = (e.getX() - offsetX) / scaleFactor;
            float ty = (e.getY() - offsetY) / scaleFactor;
            ViewNode node = findNodeAt(tx, ty);
            if (node != null && nodeActionListener != null) {
                nodeActionListener.onNodeDoubleTap(node.getData());
                return true;
            }
            return false;
        }
    }

    private class CanvasDragListener implements OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            float tx = (event.getX() - offsetX) / scaleFactor;
            float ty = (event.getY() - offsetY) / scaleFactor;

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED: {
                    ClipDescription desc = event.getClipDescription();
                    if (desc != null && desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        return true;
                    }
                    return false;
                }
                case DragEvent.ACTION_DRAG_ENTERED: {
                    dropTargetPos = new PointF(tx, ty);
                    invalidate();
                    return true;
                }
                case DragEvent.ACTION_DRAG_LOCATION: {
                    dropTargetPos = new PointF(tx, ty);
                    invalidate();
                    return true;
                }
                case DragEvent.ACTION_DRAG_EXITED: {
                    dropTargetPos = null;
                    invalidate();
                    return true;
                }
                case DragEvent.ACTION_DROP: {
                    ClipData data = event.getClipData();
                    if (data == null || data.getItemCount() == 0) return false;

                    String methodName = data.getItemAt(0).getText().toString();
                    NodeType type = null;
                    String label = methodName;

                    if (data.getDescription().getLabel() != null) {
                        String[] parts = data.getDescription().getLabel().toString().split("\\|");
                        if (parts.length >= 2) {
                            label = parts[0];
                            type = NodeType.valueOf(parts[1]);
                        }
                    }

                    if (type == null) return false;

                    FlowNode node = new FlowNode(label, type, tx - 90f, ty - 38f);
                    if (methodName != null && !methodName.isEmpty()) {
                        node.putProperty("_method", methodName);
                    }

                    if (nodeActionListener != null) {
                        nodeActionListener.onNodeDropped(node);
                    }
                    addNode(node);

                    dropTargetPos = null;
                    invalidate();
                    return true;
                }
                case DragEvent.ACTION_DRAG_ENDED: {
                    dropTargetPos = null;
                    invalidate();
                    return true;
                }
            }
            return true;
        }
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
        rebuildViewNodes();
        invalidate();
    }

    public Workflow getWorkflow() { return workflow; }

    public void setNodeActionListener(OnNodeActionListener listener) {
        this.nodeActionListener = listener;
    }

    public void addNode(FlowNode node) {
        workflow.addNode(node);
        viewNodes.add(new ViewNode(node));
        invalidate();
    }

    public void removeSelectedNode() {
        if (selectedNode != null) {
            workflow.removeNode(selectedNode.getData());
            viewNodes.remove(selectedNode);
            selectedNode = null;
            invalidate();
        }
    }

    public void clearAll() {
        workflow.getNodes().clear();
        workflow.getConnections().clear();
        viewNodes.clear();
        selectedNode = null;
        invalidate();
    }

    private void rebuildViewNodes() {
        viewNodes.clear();
        for (FlowNode node : workflow.getNodes()) {
            viewNodes.add(new ViewNode(node));
        }
    }

    public ViewNode getViewNodeByNodeId(String nodeId) {
        for (ViewNode vn : viewNodes) {
            if (vn.getData().getId().equals(nodeId)) return vn;
        }
        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scaleFactor, scaleFactor);

        drawGrid(canvas);
        drawConnections(canvas);

        for (ViewNode vn : viewNodes) {
            vn.draw(canvas);
            if (vn == selectedNode) {
                RectF r = new RectF(
                    vn.getData().getX() - 4f,
                    vn.getData().getY() - 4f,
                    vn.getData().getX() + vn.getWidth() + 4f,
                    vn.getData().getY() + vn.getHeight() + 4f
                );
                canvas.drawRoundRect(r, 14f, 14f, selectedStrokePaint);
            }
        }

        if (dropTargetPos != null) {
            Paint dropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dropPaint.setColor(0xFF4CAF50);
            dropPaint.setAlpha(100);
            dropPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(dropTargetPos.x, dropTargetPos.y, 10f, dropPaint);
            dropPaint.setStyle(Paint.Style.STROKE);
            dropPaint.setAlpha(180);
            dropPaint.setStrokeWidth(2f);
            canvas.drawCircle(dropTargetPos.x, dropTargetPos.y, 24f, dropPaint);
        }

        if (isConnecting && connectEndPoint != null) {
            PointF start = connectSourcePort.getCenter();
            drawBezierConnection(canvas, start.x, start.y,
                    connectEndPoint.x, connectEndPoint.y, connectionDragPaint);
        }

        canvas.restore();
    }

    private void drawGrid(Canvas canvas) {
        float gridSize = GRID_SIZE;
        float startX = 0;
        float startY = 0;

        int colCount = (int) (getWidth() / scaleFactor / gridSize) + 4;
        int rowCount = (int) (getHeight() / scaleFactor / gridSize) + 4;

        for (int i = 0; i < colCount; i++) {
            float x = startX + i * gridSize;
            canvas.drawLine(x, startY, x, startY + rowCount * gridSize, gridPaint);
        }
        for (int i = 0; i < rowCount; i++) {
            float y = startY + i * gridSize;
            canvas.drawLine(startX, y, startX + colCount * gridSize, y, gridPaint);
        }
    }

    private void drawConnections(Canvas canvas) {
        for (Connection conn : workflow.getConnections()) {
            ViewNode sourceVn = getViewNodeByNodeId(conn.getSourceNodeId());
            ViewNode targetVn = getViewNodeByNodeId(conn.getTargetNodeId());
            if (sourceVn != null && targetVn != null) {
                PointF start = sourceVn.getOutputPort() != null ? sourceVn.getOutputPort().getCenter() : null;
                PointF end = targetVn.getInputPort() != null ? targetVn.getInputPort().getCenter() : null;
                if (start == null || end == null) continue;
                drawBezierConnection(canvas, start.x, start.y, end.x, end.y, connectionPaint);

                if (sourceVn.getData().getType() == com.tgflowbot.model.NodeType.CONDITION) {
                    float mx = (start.x + end.x) / 2f;
                    float my = (start.y + end.y) / 2f - 18f;
                labelPaint.setColor(conn.getConditionResult() ? 0xFF4CAF50 : 0xFFF44336);
                canvas.drawText(conn.getConditionResult() ? "IF" : "ELSE", mx, my, labelPaint);
                }
            }
        }

        if (dotAnimating) {
            for (Connection conn : workflow.getConnections()) {
                ViewNode sourceVn = getViewNodeByNodeId(conn.getSourceNodeId());
                ViewNode targetVn = getViewNodeByNodeId(conn.getTargetNodeId());
                if (sourceVn == null || targetVn == null) continue;
                PointF start = sourceVn.getOutputPort() != null ? sourceVn.getOutputPort().getCenter() : null;
                PointF end = targetVn.getInputPort() != null ? targetVn.getInputPort().getCenter() : null;
                if (start == null || end == null) continue;
                float t = dotProgress;
                float x1 = start.x, y1 = start.y, x2 = end.x, y2 = end.y;
                float ctrlX = (x1 + x2) / 2f;
                float bx = bezierX(x1, ctrlX, ctrlX, x2, t);
                float by = bezierY(y1, y1, y2, y2, t);
                float dotRadius = 5f + 3f * (1f - Math.abs(t - 0.5f) * 2f);
                int alpha = (int) (255 * (1f - Math.abs(t - 0.5f) * 1.6f));
                dotGlowPaint.setAlpha(alpha / 3);
                dotPaint.setAlpha(alpha);
                canvas.drawCircle(bx, by, dotRadius + 6f, dotGlowPaint);
                canvas.drawCircle(bx, by, dotRadius, dotPaint);
            }
        }
    }

    private float bezierX(float p0, float p1, float p2, float p3, float t) {
        float u = 1f - t;
        return u * u * u * p0 + 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t * p3;
    }

    private float bezierY(float p0, float p1, float p2, float p3, float t) {
        float u = 1f - t;
        return u * u * u * p0 + 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t * p3;
    }

    public void triggerPulse() {
        dotAnimating = true;
        dotProgress = 0f;
        dotHandler.removeCallbacks(dotRunnable);
        dotHandler.post(dotRunnable);
    }

    private void drawBezierConnection(Canvas canvas, float x1, float y1,
                                       float x2, float y2, Paint paint) {
        Path path = new Path();
        path.moveTo(x1, y1);
        float ctrlX = (x1 + x2) / 2f;
        path.cubicTo(ctrlX, y1, ctrlX, y2, x2, y2);
        canvas.drawPath(path, paint);

        canvas.drawCircle(x1, y1, 6f, paint);
        canvas.drawCircle(x2, y2, 6f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        float touchX = (event.getX() - offsetX) / scaleFactor;
        float touchY = (event.getY() - offsetY) / scaleFactor;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isPanning = false;

                ViewNode portNode = findNodeAtOutputPort(touchX, touchY);
                if (portNode != null) {
                    isConnecting = true;
                    connectSourceNode = portNode;
                    connectSourcePort = portNode.getOutputPort();
                    connectEndPoint = new PointF(touchX, touchY);
                    selectedNode = portNode;
                    invalidate();
                    if (nodeActionListener != null) {
                        nodeActionListener.onNodeSelected(portNode.getData());
                    }
                    break;
                }

                dragNode = findNodeAt(touchX, touchY);
                if (dragNode != null) {
                    selectedNode = dragNode;
                    invalidate();

                    dragOffsetX = touchX - dragNode.getData().getX();
                    dragOffsetY = touchY - dragNode.getData().getY();

                    if (nodeActionListener != null) {
                        nodeActionListener.onNodeSelected(dragNode.getData());
                    }
                } else {
                    selectedNode = null;
                    invalidate();
                    if (nodeActionListener != null) {
                        nodeActionListener.onNodeSelected(null);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (isConnecting && connectSourceNode != null) {
                    connectEndPoint = new PointF(
                            (event.getX() - offsetX) / scaleFactor,
                            (event.getY() - offsetY) / scaleFactor
                    );
                    invalidate();
                } else if (dragNode != null) {
                    float newX = touchX - dragOffsetX;
                    float newY = touchY - dragOffsetY;
                    dragNode.getData().setX(newX);
                    dragNode.getData().setY(newY);
                    invalidate();
                } else {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isPanning = true;
                        offsetX += dx;
                        offsetY += dy;
                        lastTouchX = event.getX();
                        lastTouchY = event.getY();
                        invalidate();
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (longPressFired) {
                    longPressFired = false;
                    break;
                }
                if (isConnecting && connectSourceNode != null) {
                    ViewNode targetNode = findNodeAtInputPort(
                            (event.getX() - offsetX) / scaleFactor,
                            (event.getY() - offsetY) / scaleFactor
                    );
                    if (targetNode != null && targetNode != connectSourceNode) {
                        boolean isCondition = connectSourceNode.getData().getType() == com.tgflowbot.model.NodeType.CONDITION;
                        boolean condResult = true;
                        if (isCondition) {
                            String[] options = {"IF (benar)", "ELSE (salah)"};
                            final com.tgflowbot.view.ViewNode srcFinal = connectSourceNode;
                            final com.tgflowbot.view.ViewNode tgtFinal = targetNode;
                            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                                    .setTitle("Path untuk kondisi")
                                    .setItems(options, (di, w) -> {
                                        boolean result = w == 0;
                                        Connection conn = new Connection(
                                                srcFinal.getData().getId(),
                                                tgtFinal.getData().getId(),
                                                result
                                        );
                                        workflow.addConnection(conn);
                                        if (nodeActionListener != null) {
                                            nodeActionListener.onConnectionCreated(conn);
                                        }
                                        isConnecting = false;
                                        connectSourceNode = null;
                                        connectSourcePort = null;
                                        connectEndPoint = null;
                                        invalidate();
                                    })
                                    .setCancelable(true)
                                    .setOnCancelListener(di -> {
                                        isConnecting = false;
                                        connectSourceNode = null;
                                        connectSourcePort = null;
                                        connectEndPoint = null;
                                        invalidate();
                                    })
                                    .create();
                            dialog.show();
                            break;
                        }
                        Connection conn = new Connection(
                                connectSourceNode.getData().getId(),
                                targetNode.getData().getId(),
                                true
                        );
                        workflow.addConnection(conn);
                        if (nodeActionListener != null) {
                            nodeActionListener.onConnectionCreated(conn);
                        }
                    }
                    isConnecting = false;
                    connectSourceNode = null;
                    connectSourcePort = null;
                    connectEndPoint = null;
                    invalidate();
                }

                if (!isPanning && dragNode == null && !isConnecting) {
                    performClick();
                }

                dragNode = null;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                longPressFired = false;
                isConnecting = false;
                connectSourceNode = null;
                connectSourcePort = null;
                connectEndPoint = null;
                dragNode = null;
                invalidate();
                break;
            }
        }

        return true;
    }

    private ViewNode findNodeAt(float x, float y) {
        for (int i = viewNodes.size() - 1; i >= 0; i--) {
            ViewNode vn = viewNodes.get(i);
            if (vn.contains(x, y)) return vn;
        }
        return null;
    }

    private ViewNode findNodeAtOutputPort(float x, float y) {
        for (int i = viewNodes.size() - 1; i >= 0; i--) {
            ViewNode vn = viewNodes.get(i);
            if (vn.containsOutputPort(x, y)) return vn;
        }
        return null;
    }

    private ViewNode findNodeAtInputPort(float x, float y) {
        for (ViewNode vn : viewNodes) {
            if (vn.containsInputPort(x, y)) return vn;
        }
        return null;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float focusX, focusY;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float newScale = scaleFactor * detector.getScaleFactor();
            newScale = Math.max(MIN_ZOOM, Math.min(newScale, MAX_ZOOM));

            float prevScale = scaleFactor;
            scaleFactor = newScale;

            float scaleRatio = scaleFactor / prevScale;
            offsetX = focusX - scaleRatio * (focusX - offsetX);
            offsetY = focusY - scaleRatio * (focusY - offsetY);

            invalidate();
            return true;
        }
    }
}
