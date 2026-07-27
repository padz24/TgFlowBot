package com.tgflowbot.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.tgflowbot.model.FlowNode;

public class ViewNode {
    private static final float NODE_WIDTH = 180f;
    private static final float NODE_HEADER_HEIGHT = 36f;
    private static final float NODE_BODY_HEIGHT = 40f;
    private static final float NODE_TOTAL_HEIGHT = NODE_HEADER_HEIGHT + NODE_BODY_HEIGHT;
    private static final float PORT_RADIUS = 7f;
    private static final float PORT_TOUCH_RADIUS = 18f;
    private static final float PORT_PROTRUDE = 10f;
    private static final float CORNER_RADIUS = 10f;

    private final FlowNode data;
    private final RectF bounds = new RectF();
    private final RectF headerBounds = new RectF();
    private final RectF bodyBounds = new RectF();
    private final RectF inputPortBounds = new RectF();
    private final RectF outputPortBounds = new RectF();

    private final Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint typePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint propPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint portPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint portStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint portTouchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint separatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int headerColor = 0xFF1976D2;
    private int strokeColor = 0xFF1976D2;

    public ViewNode(FlowNode data) {
        this.data = data;
        initPaints();
        updateColors();
        updateBounds();
    }

    private void initPaints() {
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(18);
        shadowPaint.setStyle(Paint.Style.FILL);

        headerPaint.setStyle(Paint.Style.FILL);

        bodyPaint.setStyle(Paint.Style.FILL);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1.5f);

        separatorPaint.setColor(0xFFE0E0E0);
        separatorPaint.setStrokeWidth(0.5f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(12f);
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.LEFT);

        typePaint.setColor(0xFF888888);
        typePaint.setTextSize(10f);
        typePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        typePaint.setTextAlign(Paint.Align.LEFT);

        propPaint.setColor(0xFF555555);
        propPaint.setTextSize(10f);
        propPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        propPaint.setTextAlign(Paint.Align.LEFT);

        portPaint.setColor(Color.WHITE);
        portPaint.setStyle(Paint.Style.FILL);

        portStrokePaint.setStyle(Paint.Style.STROKE);
        portStrokePaint.setStrokeWidth(2.5f);

        portTouchPaint.setStyle(Paint.Style.STROKE);
        portTouchPaint.setStrokeWidth(1.5f);

        iconPaint.setAntiAlias(true);
        iconPaint.setStyle(Paint.Style.FILL);
    }

    private void updateColors() {
        switch (data.getType()) {
            case TRIGGER:
                headerColor = 0xFFFF9800;
                strokeColor = 0xFFFF9800;
                break;
            case ACTION:
                headerColor = 0xFF2196F3;
                strokeColor = 0xFF2196F3;
                break;
            case CONDITION:
                headerColor = 0xFF4CAF50;
                strokeColor = 0xFF4CAF50;
                break;
            case OUTPUT:
                headerColor = 0xFF9C27B0;
                strokeColor = 0xFF9C27B0;
                break;
        }
        headerPaint.setColor(headerColor);
        bodyPaint.setColor(0xFFFFFFFF);
        strokePaint.setColor(strokeColor);
        portStrokePaint.setColor(strokeColor);
        portTouchPaint.setColor(headerColor & 0x26FFFFFF);
    }

    private String formatMethodName(String name) {
        if (name == null || name.isEmpty()) return "";
        String s = name.replace("_", " ");
        s = s.replaceAll("([a-z])([A-Z])", "$1 $2");
        if (s.length() > 0) {
            s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
        return s;
    }

    private String getTypeLabel() {
        switch (data.getType()) {
            case TRIGGER: return "TRIGGER";
            case ACTION: return "ACTION";
            case CONDITION: return "CONDITION";
            case OUTPUT: return "OUTPUT";
            default: return "";
        }
    }

    public void updateBounds() {
        float x = data.getX();
        float y = data.getY();

        bounds.set(x, y, x + NODE_WIDTH, y + NODE_TOTAL_HEIGHT);
        headerBounds.set(x, y, x + NODE_WIDTH, y + NODE_HEADER_HEIGHT);
        bodyBounds.set(x, y + NODE_HEADER_HEIGHT, x + NODE_WIDTH, y + NODE_TOTAL_HEIGHT);

        float portCenterY = y + NODE_TOTAL_HEIGHT / 2f;

        float inputPortCenterX = x - PORT_PROTRUDE;
        inputPortBounds.set(
            inputPortCenterX - PORT_TOUCH_RADIUS,
            portCenterY - PORT_TOUCH_RADIUS,
            inputPortCenterX + PORT_TOUCH_RADIUS,
            portCenterY + PORT_TOUCH_RADIUS
        );

        float outputPortCenterX = x + NODE_WIDTH + PORT_PROTRUDE;
        outputPortBounds.set(
            outputPortCenterX - PORT_TOUCH_RADIUS,
            portCenterY - PORT_TOUCH_RADIUS,
            outputPortCenterX + PORT_TOUCH_RADIUS,
            portCenterY + PORT_TOUCH_RADIUS
        );
    }

    public void draw(Canvas canvas) {
        updateBounds();

        canvas.drawRoundRect(
            bounds.left + 2f, bounds.top + 4f,
            bounds.right + 2f, bounds.bottom + 4f,
            CORNER_RADIUS, CORNER_RADIUS, shadowPaint
        );

        canvas.drawRoundRect(bounds, CORNER_RADIUS, CORNER_RADIUS, bodyPaint);

        canvas.drawRoundRect(headerBounds, CORNER_RADIUS, CORNER_RADIUS, headerPaint);
        canvas.drawRect(
            headerBounds.left, headerBounds.top + CORNER_RADIUS,
            headerBounds.right, headerBounds.bottom,
            headerPaint
        );

        canvas.drawLine(
            headerBounds.left + 2f, headerBounds.bottom,
            headerBounds.right - 2f, headerBounds.bottom,
            separatorPaint
        );

        canvas.drawRoundRect(bounds, CORNER_RADIUS, CORNER_RADIUS, strokePaint);

        float iconX = bounds.left + 12f;
        float iconY = headerBounds.centerY();
        float iconR = 8f;
        iconPaint.setColor(Color.WHITE);
        iconPaint.setAlpha(50);
        canvas.drawCircle(iconX, iconY, iconR, iconPaint);
        iconPaint.setAlpha(90);
        canvas.drawCircle(iconX, iconY, iconR - 2f, iconPaint);

        float textX = bounds.left + 28f;
        float textY = headerBounds.centerY() + textPaint.getTextSize() / 3f;
        String label = data.getLabel();
        if (label.length() > 16) label = label.substring(0, 14) + "..";
        canvas.drawText(label, textX, textY, textPaint);

        String typeLabel = getTypeLabel();
        float bodyX = bounds.left + 28f;
        float typeY = bodyBounds.top + 18f;
        canvas.drawText(typeLabel, bodyX, typeY, typePaint);

        float propY = typeY + 14f;
        String methodName = data.getProperty("_method");
        String propText = null;

        if (data.getType() == com.tgflowbot.model.NodeType.CONDITION) {
            String condType = formatMethodName(methodName);
            if (condType.length() > 14) condType = condType.substring(0, 12) + "..";
            StringBuilder sb = new StringBuilder(condType);
            for (java.util.Map.Entry<String, String> e : data.getProperties().entrySet()) {
                if (e.getKey().equals("_method")) continue;
                String val = e.getValue();
                if (val == null || val.isEmpty()) continue;
                sb.append(" ").append(val.length() > 16 ? val.substring(0, 14) + ".." : val);
                break;
            }
            propText = sb.toString();
            if (propText.length() > 24) propText = propText.substring(0, 22) + "..";
        } else {
            for (java.util.Map.Entry<String, String> e : data.getProperties().entrySet()) {
                if (e.getKey().equals("_method")) continue;
                String val = e.getValue();
                if (val == null || val.isEmpty()) continue;
                propText = e.getKey() + ": " + (val.length() > 20 ? val.substring(0, 18) + ".." : val);
                break;
            }
            if (propText == null && methodName != null && !methodName.isEmpty()) {
                propText = formatMethodName(methodName);
            }
        }

        if (propText != null) {
            canvas.drawText(propText, bodyX, propY, propPaint);
        }

        if (data.getType() != com.tgflowbot.model.NodeType.TRIGGER) {
            float ix = inputPortBounds.centerX();
            float iy = inputPortBounds.centerY();
            canvas.drawCircle(ix, iy, PORT_TOUCH_RADIUS, portTouchPaint);
            canvas.drawCircle(ix, iy, PORT_RADIUS, portPaint);
            canvas.drawCircle(ix, iy, PORT_RADIUS, portStrokePaint);
        }

        if (!"_return".equals(data.getProperty("_method"))) {
            float ox = outputPortBounds.centerX();
            float oy = outputPortBounds.centerY();
            canvas.drawCircle(ox, oy, PORT_TOUCH_RADIUS, portTouchPaint);
            canvas.drawCircle(ox, oy, PORT_RADIUS, portPaint);
            canvas.drawCircle(ox, oy, PORT_RADIUS, portStrokePaint);
        }
    }

    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }

    public boolean containsInputPort(float x, float y) {
        if (data.getType() == com.tgflowbot.model.NodeType.TRIGGER) return false;
        return inputPortBounds.contains(x, y);
    }

    public boolean containsOutputPort(float x, float y) {
        if ("_return".equals(data.getProperty("_method"))) return false;
        return outputPortBounds.contains(x, y);
    }

    public NodePort getInputPort() {
        return new NodePort(data.getId(), NodePort.PortType.INPUT,
                new RectF(inputPortBounds));
    }

    public NodePort getOutputPort() {
        if ("_return".equals(data.getProperty("_method"))) return null;
        return new NodePort(data.getId(), NodePort.PortType.OUTPUT,
                new RectF(outputPortBounds));
    }

    public void offset(float dx, float dy) {
        data.setX(data.getX() + dx);
        data.setY(data.getY() + dy);
        updateBounds();
    }

    public FlowNode getData() { return data; }
    public float getWidth() { return NODE_WIDTH; }
    public float getHeight() { return NODE_TOTAL_HEIGHT; }
}
