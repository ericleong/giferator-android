package com.dreamynomad.giferator;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Shader;
import android.support.annotation.NonNull;

/**
 * Created by Eric on 9/23/2015.
 */
public class Layer {
    @NonNull
    protected final Bitmap mBitmap;
    @NonNull
    protected final Canvas mCanvas;
    @NonNull
    protected final BitmapShader mShader;

    public Layer(final int width, final int height) {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    }
}
