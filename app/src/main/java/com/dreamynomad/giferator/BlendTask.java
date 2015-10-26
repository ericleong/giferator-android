package com.dreamynomad.giferator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Updates {@link ViewGroup} with list of blend modes.
 * <p/>
 * Created by Eric on 9/28/2015.
 */
public class BlendTask extends AsyncTask<Uri, Void, List<Bitmap>> {

    private static final String TAG = BlendTask.class.getSimpleName();

    private static final PorterDuff.Mode[] MODES = {PorterDuff.Mode.ADD, PorterDuff.Mode.DARKEN,
            PorterDuff.Mode.LIGHTEN, PorterDuff.Mode.MULTIPLY, PorterDuff.Mode.OVERLAY,
            PorterDuff.Mode.SCREEN, PorterDuff.Mode.SRC_OVER, PorterDuff.Mode.SRC_ATOP};

    @NonNull
    private final Context mContext;
    private final int mSize;
    @NonNull
    private final WeakReference<ViewGroup> mViewGroupWeakRef;
    @NonNull
    private final BlendListener mListener;

    public BlendTask(@NonNull final Context context, final int size,
                     @NonNull final ViewGroup viewGroup,
                     @NonNull final BlendListener listener) {
        mContext = context;
        mSize = size;
        mViewGroupWeakRef = new WeakReference<>(viewGroup);
        mListener = listener;
    }

    @Override
    protected List<Bitmap> doInBackground(Uri... params) {
        List<Bitmap> bitmaps = new ArrayList<>(params.length);

        try {
            for (Uri uri : params) {
                final Bitmap bitmap =
                        Glide.with(mContext).load(uri).asBitmap()
                                .into(mSize, mSize).get();

                if (bitmap != null) {
                    bitmaps.add(bitmap);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "Could not download images.", e);
        }

        if (bitmaps.size() > 0) {
            final Bitmap bitmap = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            final Rect rect = new Rect();
            final Paint paint = new Paint();
            final Xfermode base = paint.getXfermode();

            final List<Bitmap> modes = new ArrayList<>(MODES.length);

            for (PorterDuff.Mode mode : MODES) {

                final Xfermode test = new PorterDuffXfermode(mode);

                for (int i = 0; i < bitmaps.size(); i++) {
                    SaveUtil.cover(bitmaps.get(i), canvas, rect);
                    if (i == 0) {
                        paint.setXfermode(base);
                    } else {
                        paint.setXfermode(test);
                    }
                    canvas.drawBitmap(bitmaps.get(i), null, rect, paint);
                }

                modes.add(bitmap.copy(bitmap.getConfig(), false));
            }

            return modes;
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<Bitmap> bitmaps) {
        if (mViewGroupWeakRef.get() == null) {
            return;
        }

        final ViewGroup viewGroup = mViewGroupWeakRef.get();

        viewGroup.removeAllViews();

        if (bitmaps == null) {
            final int textSize =
                    mContext.getResources().getDimensionPixelSize(R.dimen.item_blend_width);

            for (final PorterDuff.Mode mode : MODES) {
                final TextView textView = new TextView(mContext);
                textView.setLayoutParams(new LinearLayout.LayoutParams(
                        textSize,
                        LinearLayout.LayoutParams.MATCH_PARENT
                ));
                textView.setGravity(Gravity.CENTER);
                textView.setText(mode.toString());
                textView.setBackground(mContext.getResources()
                        .getDrawable(R.drawable.editor_button));
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.setBlendMode(mode);
                    }
                });

                viewGroup.addView(textView);
            }
        } else {

            final int textSize =
                    mContext.getResources().getDimensionPixelSize(R.dimen.item_blend_width);

            for (int i = 0; i < bitmaps.size() && i < MODES.length; i++) {
                final TextView textView = new TextView(mContext);
                textView.setLayoutParams(new LinearLayout.LayoutParams(
                        textSize,
                        LinearLayout.LayoutParams.MATCH_PARENT
                ));
                textView.setGravity(Gravity.CENTER);
                final PorterDuff.Mode mode = MODES[i];
                textView.setText(mode.toString());
                textView.setBackground(
                        mContext.getResources().getDrawable(R.drawable.editor_button));
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.setBlendMode(mode);
                    }
                });
                final BitmapDrawable drawable =
                        new BitmapDrawable(mContext.getResources(), bitmaps.get(i));
                drawable.setBounds(0, 0,
                        bitmaps.get(i).getWidth(), bitmaps.get(i).getHeight());
                textView.setCompoundDrawables(null, drawable, null, null);

                viewGroup.addView(textView);
            }
        }
    }
}
