package com.altimeter.bdureau.bearconsole;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: ShareHandler
 * Author: Boris du Reau
 *
 * @description:
 *  Utility class for capturing high-resolution screenshots of active UI views
 *  and sharing them across third-party communication applications (Email, WhatsApp, etc.)
 *  using Android's secure FileProvider mechanism.
 *
 *  Key architectural enhancements:
 *    - Replaces deprecated View drawing cache with Canvas software rendering.
 *    - Sanitizes filename date patterns (avoids illegal colon ':' characters on disk).
 *    - Offloads image compression and file I/O to a background thread to prevent UI freezing.
 *    - Cleans up and delegates URI read permissions automatically to the Intent chooser.
 *    - Explicitly recycles in-memory Bitmaps to avoid native memory leaks.
 *
 * @author: boris.dureau@neuf.fr
 * ============================================================================
 */

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShareHandler {

    private static final String TAG = "ShareHandler";

    // Dedicated background worker thread for disk operations
    private static final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();

    /**
     * Captures a screenshot of the target View and triggers sharing asynchronously.
     *
     * @param view The UI view to capture.
     * @param ctx  The calling Context or Activity.
     */
    public static void takeScreenShot(View view, Context ctx) {
        if (view == null || view.getWidth() <= 0 || view.getHeight() <= 0) {
            Log.e(TAG, "Cannot take screenshot: View is null or has zero dimensions.");
            return;
        }

        // 1. Allocate Bitmap and render the view hierarchy using Canvas (replaces getDrawingCache())
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        // 2. Format filename timestamp using hyphens instead of colons for filesystem safety
        Date date = new Date();
        CharSequence format = DateFormat.format("yyyy-MM-dd_HH-mm-ss", date);

        // 3. Compress and write to disk on background executor to avoid freezing the UI thread
        diskExecutor.execute(() -> {
            File mainDir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FileShare");
            if (!mainDir.exists() && !mainDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory structure: " + mainDir.getAbsolutePath());
                bitmap.recycle();
                return;
            }

            File imageFile = new File(mainDir, "AltiMultiCurve-" + format + ".png");

            try (FileOutputStream fileOutputStream = new FileOutputStream(imageFile)) {
                // Compress Bitmap to lossless PNG format
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.flush();

                // Post share intent back to the UI thread
                new Handler(Looper.getMainLooper()).post(() -> shareScreenShot(imageFile, ctx));
            } catch (IOException e) {
                Log.e(TAG, "Failed to write screenshot image file to disk", e);
            } finally {
                // Free native Bitmap memory
                bitmap.recycle();
            }
        });
    }

    /**
     * Creates an ACTION_SEND intent with temporary FileProvider read permissions
     * and displays the application chooser dialog.
     *
     * @param imageFile The screenshot file on disk to share.
     * @param ctx       The Context launching the Intent.
     */
    public static void shareScreenShot(File imageFile, Context ctx) {
        if (imageFile == null || !imageFile.exists()) {
            Toast.makeText(ctx, "Screenshot file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build FileProvider content URI using applicationId
        Uri uri = FileProvider.getUriForFile(
                ctx,
                BuildConfig.APPLICATION_ID + ".provider",
                imageFile
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.bearconsole_has_shared4));
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        // Grant temporary read permission to any target application handling this intent
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(intent, ctx.getString(R.string.share_with4));
        if (!(ctx instanceof Activity)) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            ctx.startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ctx, ctx.getString(R.string.no_app_available4), Toast.LENGTH_SHORT).show();
        }
    }
}