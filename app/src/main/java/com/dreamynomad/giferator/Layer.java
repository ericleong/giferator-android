package com.dreamynomad.giferator;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Shader;
import android.support.annotation.NonNull;

/**
 * Simple struct that stores the objects needed
 * <p/>
 * Created by Eric on 9/23/2015.
 */
public class Layer {
    @NonNull
    protected final Canvas mCanvas;
    @NonNull
    protected final BitmapShader mShader;

    /**
     * @param width  desired width of the layer.
     * @param height desired height of the layer.
     */
    public Layer(final int width, final int height) {
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(bitmap);
        mShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    }
}
