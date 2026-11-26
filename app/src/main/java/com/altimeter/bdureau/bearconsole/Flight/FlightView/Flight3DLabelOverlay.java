package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: Flight3DLabelOverlay
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Transparent view drawn above the 3D flight view: text labels of the events (pad,
 *  apogee, main, landing) and of the north direction. Text is much simpler and sharper to
 *  draw with the Android canvas than in OpenGL. The renderer sends the screen position of
 *  each label after every frame (GL thread); this view stores them and redraws itself on the
 *  UI thread.
 *
 *  Placement without overlap: labels are placed one after the other, in priority order
 *  (apogee, main, landing, pad, north). For each one, a list of candidate positions around
 *  its point is tried (right / left, above / below, then further away in steps of one label
 *  height); the first one that stays inside the view and touches neither an already placed
 *  label, nor the point of another event, nor an information box drawn over the scene
 *  (setObstacles) is kept. When no free position exists, the one with the smallest overlap
 *  is used. A thin leader line joins a label to its point when it had to be moved away.
 * ============================================================================
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class Flight3DLabelOverlay extends View implements Flight3DRenderer.LabelListener {

    /** Placement order: the most important labels get the best positions. */
    private static final int[] PRIORITY = {
            Flight3DRenderer.LABEL_APOGEE,
            Flight3DRenderer.LABEL_MAIN,
            Flight3DRenderer.LABEL_LANDING,
            Flight3DRenderer.LABEL_PAD,
            Flight3DRenderer.LABEL_NORTH
    };
    /** Farthest vertical shift tried, in label heights. */
    private static final int MAX_SHIFT_STEPS = 8;

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    private final String[] texts = new String[Flight3DRenderer.LABEL_COUNT];
    private volatile float[] positions;
    private volatile boolean[] visible;

    /** Views drawn over the scene that labels must not cover (information boxes). */
    private View[] obstacleViews = new View[0];

    // Work objects reused at each frame
    private final RectF[] placed = new RectF[Flight3DRenderer.LABEL_COUNT];
    private final List<RectF> blockers = new ArrayList<>();
    private final RectF candidate = new RectF();
    private final RectF best = new RectF();

    public Flight3DLabelOverlay(Context context) {
        this(context, null);
    }

    public Flight3DLabelOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        textPaint.setColor(Color.rgb(0x26, 0x32, 0x38));
        textPaint.setTextSize(12f * density);
        boxPaint.setColor(Color.argb(0xE6, 0xFF, 0xFF, 0xFF));
        linePaint.setColor(Color.argb(0xB0, 0x37, 0x47, 0x4F));
        linePaint.setStrokeWidth(Math.max(1f, density));
        for (int k = 0; k < placed.length; k++) placed[k] = new RectF();
        // Labels are informative only: touches go to the 3D view below
        setClickable(false);
        setFocusable(false);
    }

    /** Text of one label (Flight3DRenderer.LABEL_*), null to hide it. */
    public void setLabelText(int label, String text) {
        texts[label] = text;
        postInvalidateOnAnimation();
    }

    /**
     * Views of the same parent drawn over the scene (information boxes): labels avoid them.
     * Hidden views are ignored.
     */
    public void setObstacles(View... views) {
        obstacleViews = views != null ? views : new View[0];
        postInvalidateOnAnimation();
    }

    @Override
    public void onLabelPositions(float[] xy, boolean[] vis) {
        positions = xy;
        visible = vis;
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float[] xy = positions;
        boolean[] vis = visible;
        if (xy == null || vis == null) return;

        float pad = 4f * density;
        float gap = 4f * density;
        float markerHalf = 9f * density;
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float boxH = (fm.descent - fm.ascent) + pad;

        // ---- Fixed obstacles: information boxes and the points of every visible event ----
        blockers.clear();
        for (View v : obstacleViews) {
            if (v != null && v.getVisibility() == View.VISIBLE && v.getWidth() > 0) {
                blockers.add(new RectF(v.getLeft() - gap, v.getTop() - gap, v.getRight() + gap, v.getBottom() + gap));
            }
        }
        for (int k = 0; k < Flight3DRenderer.LABEL_COUNT; k++) {
            if (texts[k] == null || !vis[k] || k == Flight3DRenderer.LABEL_NORTH) continue;
            float x = xy[k * 2], y = xy[k * 2 + 1];
            blockers.add(new RectF(x - markerHalf, y - markerHalf, x + markerHalf, y + markerHalf));
        }
        int fixedCount = blockers.size();

        // ---- Labels, in priority order ----
        for (int k : PRIORITY) {
            String text = texts[k];
            if (text == null || !vis[k]) continue;
            float ax = xy[k * 2], ay = xy[k * 2 + 1];
            float boxW = textPaint.measureText(text) + 2 * pad;

            if (k == Flight3DRenderer.LABEL_NORTH) {
                // North letter: centered on its point, moved only if it collides
                findPlace(ax - boxW / 2f, ay - boxH / 2f, boxW, boxH, ax, ay, true);
            } else {
                findPlace(ax + markerHalf + gap, ay - boxH - gap, boxW, boxH, ax, ay, false);
            }
            RectF r = placed[k];
            r.set(best);
            blockers.add(new RectF(r));

            // Leader line when the label is not next to its point
            float cx = Math.max(r.left, Math.min(ax, r.right));
            float cy = Math.max(r.top, Math.min(ay, r.bottom));
            if (Math.hypot(cx - ax, cy - ay) > markerHalf + 2 * gap) {
                canvas.drawLine(ax, ay, cx, cy, linePaint);
            }
        }

        // ---- Boxes and texts, drawn after the leader lines so that lines pass under ----
        for (int k : PRIORITY) {
            if (texts[k] == null || !vis[k]) continue;
            RectF r = placed[k];
            canvas.drawRoundRect(r, 4f * density, 4f * density, boxPaint);
            canvas.drawText(texts[k], r.left + pad, r.top + pad / 2 - fm.ascent, textPaint);
        }
        // The fixed obstacles are rebuilt at the next frame
        while (blockers.size() > fixedCount) blockers.remove(blockers.size() - 1);
    }

    /**
     * Chooses the position of a label of size w x h and leaves it in {@code best}.
     *
     * @param prefX    left of the preferred position
     * @param prefY    top of the preferred position
     * @param ax       x of the label point
     * @param ay       y of the label point
     * @param centered true for a label centered on its point (north letter)
     */
    private void findPlace(float prefX, float prefY, float w, float h, float ax, float ay, boolean centered) {
        float markerHalf = 9f * density;
        float gap = 4f * density;
        float rightX = ax + markerHalf + gap;
        float leftX = ax - markerHalf - gap - w;
        float bestOverlap = Float.MAX_VALUE;
        boolean found = false;

        // Candidates: preferred, then right / left of the point, moving up and down one step
        // of label height at a time (above first: the sky is usually free)
        for (int step = 0; step <= MAX_SHIFT_STEPS && !found; step++) {
            float[][] tries;
            if (step == 0) {
                tries = centered
                        ? new float[][]{{prefX, prefY}}
                        : new float[][]{{prefX, prefY}, {rightX, ay + gap}, {leftX, ay - h - gap}, {leftX, ay + gap},
                        {rightX, ay - h / 2f}, {leftX, ay - h / 2f}};
            } else {
                float up = ay - h / 2f - step * (h + gap);
                float down = ay - h / 2f + step * (h + gap);
                float cx = centered ? prefX : rightX;
                float cx2 = centered ? prefX : leftX;
                tries = new float[][]{{cx, up}, {cx, down}, {cx2, up}, {cx2, down}};
            }
            for (float[] t : tries) {
                float x = Math.max(0, Math.min(getWidth() - w, t[0]));
                float y = Math.max(0, Math.min(getHeight() - h, t[1]));
                candidate.set(x, y, x + w, y + h);
                float overlap = overlapArea(candidate);
                if (overlap < bestOverlap) {
                    bestOverlap = overlap;
                    best.set(candidate);
                }
                if (overlap <= 0f) {
                    found = true;
                    break;
                }
            }
        }
    }

    /** Total area of the intersections of r with the obstacles and placed labels. */
    private float overlapArea(RectF r) {
        float sum = 0f;
        for (int i = 0, n = blockers.size(); i < n; i++) {
            RectF b = blockers.get(i);
            float w = Math.min(r.right, b.right) - Math.max(r.left, b.left);
            float h = Math.min(r.bottom, b.bottom) - Math.max(r.top, b.top);
            if (w > 0 && h > 0) sum += w * h;
        }
        return sum;
    }
}
