package com.altimeter.bdureau.bearconsole;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: BaseActivity
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Common parent of every screen of the application. It centralizes two behaviors that
 *  used to be copied (or missing) in each activity:
 *    - Language: the base context is localized before inflation, using the language chosen
 *      in the application settings (phone language or forced English).
 *    - Edge-to-edge: from Android 15 (API 35) apps targeting the latest SDK are drawn behind
 *      the status bar, the action bar, the navigation bar and the display cutout. The content
 *      view is padded so that no button or text is hidden:
 *        - top: the real overlap between the content and the action bar / status bar is
 *          measured on screen after each layout. It does not rely on assumptions about how
 *          the AppCompat action bar handles the insets, which vary between versions;
 *        - left / right / bottom: system bar, cutout and keyboard insets.
 *      On older Android versions (no edge-to-edge) the measured overlap and the insets are
 *      zero, so nothing changes.
 * ============================================================================
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class BaseActivity extends AppCompatActivity {

    // Last insets received (window coordinates, pixels)
    private Insets systemBars = Insets.NONE;
    private int imeBottom = 0;

    // Content root currently padded and its layout listener (re-installed on setContentView)
    private View paddedContent;
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Read the language directly from the preferences: the application object may not be
        // fully initialized yet when the first activity is attached
        SharedPreferences prefs = newBase.getSharedPreferences("BearConsoleCfg", MODE_PRIVATE);
        int lang = prefs.getInt("AppLanguage", 0);
        super.attachBaseContext(ConsoleApplication.updateBaseContextLocale(newBase, lang));
    }

    /**
     * Called each time setContentView() installs a new layout: (re)installs the insets
     * listener and the layout listener on the content root.
     */
    @Override
    public void onContentChanged() {
        super.onContentChanged();
        applySystemBarInsets();
    }

    @Override
    protected void onDestroy() {
        if (paddedContent != null && layoutListener != null) {
            paddedContent.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        }
        super.onDestroy();
    }

    /** Pads the content root so that it never lies under the bars or the keyboard. */
    private void applySystemBarInsets() {
        final View content = findViewById(android.R.id.content);
        if (content == null) return;

        if (paddedContent != null && layoutListener != null) {
            paddedContent.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        }
        paddedContent = content;

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
            systemBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            imeBottom = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            updatePadding(v);
            // Consumed here: the child layouts must not apply the same insets a second time
            return WindowInsetsCompat.CONSUMED;
        });

        // The position of the action bar is only known after layout: re-check after each one
        layoutListener = () -> updatePadding(content);
        content.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        ViewCompat.requestApplyInsets(content);
    }

    /**
     * Computes and applies the padding of the content root. Only calls setPadding() when the
     * value changes, so that the layout listener cannot loop.
     */
    private void updatePadding(View content) {
        int[] contentPos = new int[2];
        content.getLocationInWindow(contentPos);
        int contentTop = contentPos[1];

        // Lowest bottom edge of what can cover the top of the content:
        // the status bar / cutout, and the action bar when it is displayed
        int coverBottom = systemBars.top;
        View actionBar = findViewById(androidx.appcompat.R.id.action_bar_container);
        if (actionBar != null && actionBar.getVisibility() == View.VISIBLE && actionBar.getHeight() > 0) {
            int[] barPos = new int[2];
            actionBar.getLocationInWindow(barPos);
            coverBottom = Math.max(coverBottom, barPos[1] + actionBar.getHeight());
        }

        int top = Math.max(0, coverBottom - contentTop);
        // When the keyboard is open it covers the navigation bar: use the larger value
        int bottom = Math.max(systemBars.bottom, imeBottom);
        int left = systemBars.left;
        int right = systemBars.right;

        if (content.getPaddingTop() != top || content.getPaddingBottom() != bottom
                || content.getPaddingLeft() != left || content.getPaddingRight() != right) {
            content.setPadding(left, top, right, bottom);
        }
    }
}
