package com.z_iti_271311_u3_e07;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class AutomataView extends View{
    private Paint paintNode, paintActiveNode, paintTransition, paintText;
    private List<Node> nodes;
    private List<Transition> transitions;
    private int activeNode = -1; // Nodo actualmente activo

    public AutomataView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintNode = new Paint();
        paintNode.setColor(Color.LTGRAY);
        paintNode.setStyle(Paint.Style.FILL);

        paintActiveNode = new Paint();
        paintActiveNode.setColor(Color.RED);
        paintActiveNode.setStyle(Paint.Style.FILL);

        paintTransition = new Paint();
        paintTransition.setColor(Color.BLACK);
        paintTransition.setStyle(Paint.Style.STROKE);
        paintTransition.setStrokeWidth(5);

        paintText = new Paint();
        paintText.setColor(Color.BLACK);
        paintText.setTextSize(40);

        nodes = new ArrayList<>();
        transitions = new ArrayList<>();
    }

    public void setAutomaton(List<Node> nodes, List<Transition> transitions) {
        this.nodes = nodes;
        this.transitions = transitions;
        invalidate(); // Redibujar la vista
    }

    public void setActiveNode(int nodeIndex) {
        activeNode = nodeIndex;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Dibujar transiciones
        for (Transition transition : transitions) {
            canvas.drawLine(transition.startX, transition.startY, transition.endX, transition.endY, paintTransition);
            canvas.drawText(transition.value, (transition.startX + transition.endX) / 2,
                    (transition.startY + transition.endY) / 2 - 10, paintText);
        }

        // Dibujar nodos
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            Paint paint = (i == activeNode) ? paintActiveNode : paintNode;
            canvas.drawCircle(node.x, node.y, node.radius, paint);
            canvas.drawText(node.name, node.x - 20, node.y + 10, paintText);
        }
    }

    public static class Node {
        public String name;
        public float x, y, radius;

        public Node(String name, float x, float y, float radius) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }

    public static class Transition {
        public float startX, startY, endX, endY;
        public String value;

        public Transition(float startX, float startY, float endX, float endY, String value) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.value = value;
        }
    }
}
