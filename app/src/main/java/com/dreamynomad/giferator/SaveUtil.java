package com.dreamynomad.giferator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Eric on 10/25/2015.
 */
public class SaveUtil {

    private static final String TAG = SaveUtil.class.getSimpleName();

    public static void cover(@NonNull final Bitmap bitmap, @NonNull final Canvas canvas,
                             @NonNull final Rect rect) {
        if (canvas.getWidth() > 0 && canvas.getHeight() > 0 && bitmap.getWidth() > 0 &&
                bitmap.getHeight() > 0) {
            final double surfaceRatio = (double) canvas.getWidth() / canvas.getHeight();
            final double drawableRatio = (double) bitmap.getWidth() / bitmap.getHeight();

            if (drawableRatio > surfaceRatio) { // wider
                final int fullWidth = (int) Math.ceil(canvas.getHeight() * drawableRatio);
                rect.set((canvas.getWidth() - fullWidth) / 2, 0,
                        (fullWidth - canvas.getWidth()) / 2 + canvas.getWidth(),
                        canvas.getHeight());
            } else if (drawableRatio < surfaceRatio) { // taller
                final int fullHeight = (int) Math.ceil(canvas.getWidth() / drawableRatio);
                rect.set(0, (canvas.getWidth() - fullHeight) / 2,
                        canvas.getWidth(),
                        (fullHeight - canvas.getHeight()) / 2 + canvas.getHeight());
            } else { // same ratio
                rect.set(0, 0, canvas.getWidth(), canvas.getHeight());
            }
        }
    }

    @Nullable
    public static File writeBitmap(final Context context, final Bitmap bitmap) {
        final File file = getOutputMediaFile(
                context.getResources().getString(R.string.app_name), ".jpg");

        if (file != null) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);

                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);

                fileOutputStream.flush();
                fileOutputStream.close();

                return file;
            } catch (IOException e) {
                Log.e(TAG, "Error creating file", e);
            }
        }

        return null;
    }

    @Nullable
    public static File getOutputMediaFile(String subdir, String extension) {

        // TODO: you should check that the SDCard is mounted using
        // Environment.getExternalStorageState() before doing this.
        final File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), subdir);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "failed to create directory");
            return null;
        }

        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + extension);

        return mediaFile;
    }
}
