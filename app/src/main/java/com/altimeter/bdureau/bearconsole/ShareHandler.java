package com.altimeter.bdureau.bearconsole;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class ShareHandler {

    /*public static Bitmap takeScreenshot(View rootView) {
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
    }*/

    public static  void takeScreenShot(View view, Context ctx) {
        Date date = new Date();
        CharSequence format = DateFormat.format("MM-dd-yyyy_hh:mm:ss", date);

        try {
            File mainDir = new File(
                    ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FilShare");
            if (!mainDir.exists()) {
                boolean mkdir = mainDir.mkdir();
            }

            String path = mainDir + "/" + "AltiMultiCurve" + "-" + format + ".jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);


            File imageFile = new File(path);
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            shareScreenShot(imageFile, ctx);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Share ScreenShot
    public static  void shareScreenShot(File imageFile,Context ctx ) {

        Log.d("Package Name", "Package Name" + ctx.getPackageName());
        /*Uri uri = FileProvider.getUriForFile(
                ctx,
                ctx.getPackageName() +  ".provider",
                imageFile);*/
        Uri uri = FileProvider.getUriForFile(ctx, BuildConfig.APPLICATION_ID + ".provider", imageFile);

        ctx.grantUriPermission(ctx.getPackageName(), uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("image/*");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "Altimulti has shared with you some info");
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        Intent chooser = Intent.createChooser(intent, "Share File");

        List<ResolveInfo> resInfoList = ctx.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            ctx.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            ctx.startActivity(Intent.createChooser(intent, "Share With"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ctx, "No App Available", Toast.LENGTH_SHORT).show();
        }
    }

}
