package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: Flight3DView
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  OpenGL surface of the 3D flight view (AltiGPS), driven by a Flight3DRenderer.
 *  Touch gestures:
 *   - one finger drag: rotation around the trajectory;
 *   - pinch: zoom;
 *   - two finger drag: pan on the ground;
 *   - double tap: back to the default view.
 *  The view is inside a ViewPager: while a finger is on the 3D scene, the pager is asked not
 *  to intercept the gestures (otherwise every horizontal rotation would change the tab).
 *  Rendering is on demand (RENDERMODE_WHEN_DIRTY): a frame is drawn only after a change.
 * ============================================================================
 */

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewParent;

public class Flight3DView extends GLSurfaceView {

    /** Degrees of rotation per pixel of finger movement. */
    private static final float ROTATE_DEG_PER_PX = 0.25f;

    private Flight3DRenderer renderer;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private float lastX, lastY;
    private int lastPointerCount;

    public Flight3DView(Context context) {
        super(context);
        init(context);
    }

    public Flight3DView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        // Keep the context when the tab is paused: no re-upload of the geometry on resume
        setPreserveEGLContextOnPause(true);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (renderer != null) {
                    renderer.zoom(detector.getScaleFactor());
                    requestRender();
                }
                return true;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (renderer != null) {
                    renderer.resetCamera();
                    requestRender();
                }
                return true;
            }
        });
    }

    /** Must be called once, before the view is attached to the window. */
    public void setFlightRenderer(Flight3DRenderer r) {
        renderer = r;
        setRenderer(r);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (renderer == null) return false;
        ViewParent parent = getParent();
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN && parent != null) {
            // Keep the gesture for the 3D scene (no tab change while rotating)
            parent.requestDisallowInterceptTouchEvent(true);
        }

        scaleDetector.onTouchEvent(e);
        gestureDetector.onTouchEvent(e);

        // Centroid of the fingers: rotation with one finger, pan with two
        int count = e.getPointerCount();
        float x = 0, y = 0;
        for (int i = 0; i < count; i++) {
            x += e.getX(i);
            y += e.getY(i);
        }
        x /= count;
        y /= count;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                // The centroid jumps when a finger is added or removed: restart from here
                lastX = x;
                lastY = y;
                lastPointerCount = action == MotionEvent.ACTION_POINTER_UP ? count - 1 : count;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastX, dy = y - lastY;
                if (count != lastPointerCount) {
                    lastPointerCount = count;
                } else if (count == 1) {
                    renderer.rotate(-dx * ROTATE_DEG_PER_PX, dy * ROTATE_DEG_PER_PX);
                    requestRender();
                } else if (!scaleDetector.isInProgress() || Math.hypot(dx, dy) > 2) {
                    renderer.pan(dx, dy);
                    requestRender();
                }
                lastX = x;
                lastY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (parent != null) parent.requestDisallowInterceptTouchEvent(false);
                break;
            default:
                break;
        }
        return true;
    }
}
