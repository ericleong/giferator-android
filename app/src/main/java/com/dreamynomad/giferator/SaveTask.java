package com.dreamynomad.giferator;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.gifencoder.AnimatedGifEncoder;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.target.Target;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by Eric on 9/23/2015.
 */
public class SaveTask extends AsyncTask<Uri, Float, File> implements DialogInterface.OnCancelListener {

    private static final String TAG = SaveTask.class.getSimpleName();

    private static final int PROGRESS_MAX = 100;

    private ProgressDialog mProgressDialog;

    private final Context mContext;
    private final int mWidth, mHeight;
    private final Paint mPaint;
    @NonNull
    private final Rect mRect = new Rect();

    public SaveTask(final Context context,
                    final int width, final int height, final Paint paint) {
        mContext = context;
        mWidth = width;
        mHeight = height;
        mPaint = paint;
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMax(PROGRESS_MAX);
        mProgressDialog.show();
        mProgressDialog.setOnCancelListener(this);
    }

    @Override
    protected File doInBackground(Uri... params) {
        final List<GlideDrawable> drawables = new ArrayList<>();

        final RequestManager requestManager = Glide.with(mContext);

        int maxNumFrames = 0;

        for (int i = 0; i < params.length; i++) {
            synchronized (requestManager) {
                try {
                    final GlideDrawable glideDrawable =
                            requestManager.load(params[i])
                                    .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();

                    if (glideDrawable != null) {
                        drawables.add(glideDrawable);

                        if (glideDrawable instanceof GifDrawable) {
                            GifDrawable gifDrawable = (GifDrawable) glideDrawable;
                            final int numFrames = gifDrawable.getDecoder().getFrameCount();

                            if (numFrames > maxNumFrames) {
                                maxNumFrames = numFrames;
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "Could not load image.", e);
                }
            }
        }

        final Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        final Layer layer = new Layer(mWidth, mHeight);

        final File file;

        if (drawables.size() == params.length) {
            if (maxNumFrames <= 1) {
                draw(canvas, layer, drawables);

                publishProgress(0.5f);

                file = ImageUtil.writeBitmap(mContext, bitmap);

                publishProgress(1.0f);
            } else {
                file = ImageUtil.getOutputMediaFile(
                        mContext.getResources().getString(R.string.app_name), ".jpg");

                if (file != null) {
                    OutputStream os = null;

                    try {
                        os = new BufferedOutputStream(new FileOutputStream(file));
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "Error creating file.", e);
                    }

                    if (os != null) {
                        final AnimatedGifEncoder encoder = new AnimatedGifEncoder();

                        encoder.start(os);

                        for (int frame = 0; frame < maxNumFrames; frame++) {
                            final int delay = draw(canvas, layer, drawables);
                            encoder.addFrame(bitmap);
                            encoder.setDelay(delay > 0 ? delay : 50);

                            for (GlideDrawable drawable : drawables) {
                                if (drawable instanceof GifDrawable) {
                                    final GifDrawable gifDrawable = (GifDrawable) drawable;
                                    gifDrawable.getDecoder().advance();
                                }
                            }

                            publishProgress((float) frame / maxNumFrames);
                        }

                        encoder.finish();

                        try {
                            os.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error saving file.", e);
                        }

                        publishProgress(1.0f);
                    }
                }
            }
        } else {
            file = null;
        }

        for (Drawable drawable : drawables) {
            if (drawable instanceof GifDrawable) {
                ((GifDrawable) drawable).recycle();
            }
        }

        return file;
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        mProgressDialog.setProgress((int) (values[0] * PROGRESS_MAX));
    }

    @Override
    protected void onPostExecute(File file) {
        if (file != null) {
            final Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri uri = Uri.fromFile(file);
            mediaScanIntent.setData(uri);
            mContext.sendBroadcast(mediaScanIntent);
            Toast.makeText(mContext, "Saved to: " + uri, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext, "Save Failed", Toast.LENGTH_SHORT).show();
        }

        mProgressDialog.hide();
    }

    private int draw(final Canvas canvas, final Layer layer,
                     final List<GlideDrawable> drawables) {

        int minDelay = 0;

        for (int i = 0; i < drawables.size(); i++) {
            final GlideDrawable drawable = drawables.get(i);

            if (drawable instanceof GlideBitmapDrawable) {
                ImageUtil.cover(drawable, canvas.getWidth(), canvas.getHeight());

                if (i == 0) {
                    drawable.draw(canvas);
                } else {
                    layer.mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    drawable.draw(layer.mCanvas);
                    mPaint.setShader(layer.mShader);
                    canvas.drawRect(0, 0, layer.mCanvas.getWidth(),
                            layer.mCanvas.getHeight(), mPaint);
                }
            } else if (drawable instanceof GifDrawable) {
                final GifDrawable gifDrawable = (GifDrawable) drawable;
                gifDrawable.getDecoder().advance();
                final Bitmap gifBitmap = gifDrawable.getDecoder().getNextFrame();

                if (gifBitmap != null) {
                    ImageUtil.cover(gifBitmap, canvas, mRect);
                    mPaint.setShader(null);
                    canvas.drawBitmap(gifBitmap, null, mRect, mPaint);

                    Glide.get(mContext).getBitmapPool().put(gifBitmap);

                    if (minDelay == 0 ||
                            gifDrawable.getDecoder().getNextDelay() < minDelay) {
                        minDelay = gifDrawable.getDecoder().getNextDelay();
                    }
                }
            }
        }

        return minDelay;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        cancel(true);
    }
}
