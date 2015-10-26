package com.dreamynomad.giferator;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

/**
 * Basic image rendering utilities.
 * <p/>
 * Created by Eric on 9/23/2015.
 */
public class ImageUtil {

    private static final String TAG = ImageUtil.class.getSimpleName();

    /**
     * Sets the bounds on the drawable to "cover" the width and height.
     *
     * @param drawable the drawable to render.
     * @param width    the target width.
     * @param height   the target height.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Background_and_Borders/Scaling_background_images#cover">Mozilla Documentation</a>
     */
    public static void cover(@NonNull final Drawable drawable, final int width, final int height) {
        if (width > 0 && height > 0 && drawable.getIntrinsicWidth() > 0 &&
                drawable.getIntrinsicHeight() > 0) {
            final double targetRatio = (double) width / height;
            final double drawableRatio = (double) drawable.getIntrinsicWidth()
                    / drawable.getIntrinsicHeight();

            if (drawableRatio > targetRatio) { // wider
                final int fullWidth = (int) Math.ceil(height * drawableRatio);
                drawable.setBounds((width - fullWidth) / 2, 0,
                        (fullWidth - width) / 2 + width, height);
            } else if (drawableRatio < targetRatio) { // taller
                final int fullHeight = (int) Math.ceil(width / drawableRatio);
                drawable.setBounds(0, (height - fullHeight) / 2,
                        width, (fullHeight - height) / 2 + height);
            } else { // same ratio
                drawable.setBounds(0, 0, width, height);
            }
        }
    }

    /**
     * Renders a list of drawable to a {@link SurfaceView} using the provided {@link Paint}.
     * {@link Layer} is an optimization to reuse offscreen buffers.
     *
     * @param surfaceView the surface to draw on.
     * @param layer       the layer object to reuse.
     * @param drawables   the list of drawables to draw.
     * @param paint       the paint to draw with for the drawables after the first one.
     */
    public static void render(@Nullable final SurfaceView surfaceView, @Nullable final Layer layer,
                              @NonNull final List<? extends Drawable> drawables,
                              @NonNull final Paint paint) {
        if (surfaceView != null && surfaceView.getHolder() != null) {
            Canvas canvas = null;
            final SurfaceHolder holder = surfaceView.getHolder();

            try {
                synchronized (holder) {
                    canvas = holder.lockCanvas();

                    if (canvas != null) {
                        draw(canvas, layer, drawables, paint);
                    } else {
                        Log.e(TAG, "Could not lock canvas!");
                    }
                }
            } finally {
                if (canvas != null && holder != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
        } else {
            Log.e(TAG, "No surface or surface holder!");
        }
    }

    /**
     * @param canvas
     *      the canvas to draw to.
     * @param layer
     *      the layer to reuse.
     * @param drawables
     *      the drawables to draw.
     * @param paint
     *      the paint to use.
     */
    private static void draw(@NonNull final Canvas canvas, @Nullable final Layer layer,
                             @NonNull final List<? extends Drawable> drawables,
                             @NonNull final Paint paint) {
        for (int i = 0; i < drawables.size(); i++) {
            final Drawable drawable = drawables.get(i);

            cover(drawable, canvas.getWidth(), canvas.getHeight());

            if (i == 0) {
                drawable.draw(canvas);
            } else if (layer != null) {
                // Clear the layer canvas.
                layer.mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                // Render drawable to layer canvas.
                drawable.draw(layer.mCanvas);
                // Render layer canvas to the primary canvas.
                paint.setShader(layer.mShader);
                canvas.drawRect(0, 0, layer.mCanvas.getWidth(), layer.mCanvas.getHeight(), paint);
            }
        }
    }
}
