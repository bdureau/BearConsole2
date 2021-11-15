package com.altimeter.bdureau.bearconsole;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class ShareHandler {

    public static Bitmap takeScreenshot(View rootView) {
        //View rootView = view.findViewById(android.R.id.content).getRootView();
        rootView.setDrawingCacheEnabled(true);
        return rootView.getDrawingCache();
    }


    public static void share(Bitmap bitmap, Context ctx){
        String pathofBmp=
                MediaStore.Images.Media.insertImage(ctx.getContentResolver(),
                        bitmap,"BearConsole", null);
        Uri uri = Uri.parse(pathofBmp);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Star App");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        ctx.startActivity(Intent.createChooser(shareIntent, "BearConsole has shared you some info"));
    }
}
