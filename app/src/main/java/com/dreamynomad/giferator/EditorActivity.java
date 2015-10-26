package com.dreamynomad.giferator;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EditorActivity extends AppCompatActivity implements Drawable.Callback,
        SurfaceHolder.Callback, BlendListener {

    private static final String TAG = EditorActivity.class.getSimpleName();

    /**
     * Used to retrieve the requested image.
     */
    private static final int RESULT_GALLERY = 100;
    /**
     * Used to retrieve the requested image on devices with the Storage Access Framework.
     * <p/>
     * https://developer.android.com/guide/topics/providers/document-provider.html
     */
    private static final int RESULT_GALLERY_KITKAT = 101;
    /**
     * Image uris.
     */
    private static final String INSTANCE_URIS = EditorActivity.class.getPackage() + ".uris";
    /**
     * Blend mode.
     */
    private static final String INSTANCE_BLEND_MODE = EditorActivity.class.getPackage() + ".blend";
    /**
     * Image ratio.
     */
    private static final String INSTANCE_RATIO = EditorActivity.class.getPackage() + ".ratio";
    /**
     * The image mime type.
     */
    public static final String TYPE_IMAGE = "image/*";

    /**
     * Paths to the images.
     */
    @NonNull
    private List<Uri> mImageUris = new ArrayList<>();
    @NonNull
    private List<GlideDrawable> mDrawables = new ArrayList<>();
    /**
     * Surface.
     */
    @Nullable
    private RatioSurfaceView mSurface;
    @Nullable
    private View mEmpty;
    @NonNull
    private Handler mHandler;
    @NonNull
    private Paint mPaint = new Paint();
    @Nullable
    private Layer mLayer;

    @Nullable
    private ViewPager mViewPager;
    @Nullable
    private EditorPagerAdapter mEditorAdapter;

    @Override
    public void setBlendMode(final PorterDuff.Mode mode) {
        mPaint.setXfermode(new PorterDuffXfermode(mode));
        render();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        mSurface = (RatioSurfaceView) findViewById(R.id.surface);
        mSurface.setAspectRatio(1.0f);
        mSurface.getHolder().addCallback(this);

        final HandlerThread thread = new HandlerThread("Gifs");
        thread.start();
        mHandler = new Handler(thread.getLooper());

        mEmpty = findViewById(R.id.no_image);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        if (mViewPager != null) {
            mEditorAdapter = new EditorPagerAdapter(this, new EditorPagerAdapter.OnInteractionListener() {
                @Override
                public void add() {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType(EditorActivity.TYPE_IMAGE);
                        Intent chooserIntent = Intent.createChooser(intent,
                                EditorActivity.this.getResources().getString(R.string.choose_image));
                        EditorActivity.this.startActivityForResult(chooserIntent, EditorActivity.RESULT_GALLERY);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType(EditorActivity.TYPE_IMAGE);
                        EditorActivity.this.startActivityForResult(intent, EditorActivity.RESULT_GALLERY_KITKAT);
                    }
                }

                @Override
                public void clear() {
                    for (GlideDrawable drawable : EditorActivity.this.mDrawables) {
                        drawable.stop();
                    }
                    if (EditorActivity.this.mSurface != null) {
                        EditorActivity.this.mSurface.setVisibility(View.INVISIBLE);
                    }

                    for (Drawable drawable : EditorActivity.this.mDrawables) {
                        if (drawable instanceof GifDrawable) {
                            ((GifDrawable) drawable).recycle();
                        }
                    }

                    EditorActivity.this.mDrawables.clear();
                    EditorActivity.this.mImageUris.clear();

                    if (EditorActivity.this.mEmpty != null) {
                        EditorActivity.this.mEmpty.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void ratio(float ratio) {
                    if (EditorActivity.this.mSurface != null) {
                        EditorActivity.this.mSurface.setAspectRatio(ratio);
                    }
                }

                @Override
                public Uri[] getUris() {
                    final Uri[] uris = new Uri[EditorActivity.this.mImageUris.size()];
                    EditorActivity.this.mImageUris.toArray(uris);

                    return uris;
                }
            });
            mViewPager.setOffscreenPageLimit(2);
            mViewPager.setAdapter(mEditorAdapter);
        }

        final View.OnClickListener editors = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    default:
                    case R.id.layers:
                        mViewPager.setCurrentItem(0, true);
                        break;
                    case R.id.blends:
                        mViewPager.setCurrentItem(1, true);
                        break;
                    case R.id.ratios:
                        mViewPager.setCurrentItem(2, true);
                        break;
                }
            }
        };

        final ImageView layers = (ImageView) findViewById(R.id.layers);
        layers.setOnClickListener(editors);

        final ImageView blends = (ImageView) findViewById(R.id.blends);
        blends.setOnClickListener(editors);

        final ImageView ratios = (ImageView) findViewById(R.id.ratios);
        ratios.setOnClickListener(editors);

        if (savedInstanceState != null) {
            final String[] images = savedInstanceState.getStringArray(INSTANCE_URIS);

            if (images != null && images.length > 0) {
                for (int i = 0; i < images.length; i++) {
                    new ImageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                            Uri.parse(images[i]));
                }
            }

//            mBlends.setSelection(savedInstanceState.getInt(INSTANCE_BLEND_MODE,
//                    PorterDuff.Mode.SRC_OVER.ordinal()));
//            mRatios.setSelection(savedInstanceState.getInt(INSTANCE_RATIO,
//                    Ratio.ONE_ONE.ordinal()));
        } else {
//            mBlends.setSelection(PorterDuff.Mode.SRC_OVER.ordinal());
//            mRatios.setSelection(Ratio.ONE_ONE.ordinal());

            final Intent intent = getIntent();

            if (intent != null) {
                final String type = intent.getType();

                if (Intent.ACTION_SEND.equals(intent.getAction()) && type.startsWith("image/")) {
                    final Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

                    if (uri != null) {
                        new ImageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uri);
                    }
                } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) &&
                        type.startsWith("image/")) {
                    final ArrayList<Uri> uris =
                            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

                    for (Uri uri : uris) {
                        new ImageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uri);
                    }
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        final String[] images = new String[mImageUris.size()];
        for (int i = 0; i < mImageUris.size(); i++) {
            images[i] = mImageUris.get(i).toString();
        }
        outState.putStringArray(INSTANCE_URIS, images);
//        if (mBlends != null) {
//            outState.putInt(INSTANCE_BLEND_MODE, mBlends.getSelectedItemPosition());
//        }
//        if (mRatios != null) {
//            outState.putInt(INSTANCE_RATIO, mRatios.getSelectedItemPosition());
//        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_save) {

            final Uri[] uris = new Uri[mDrawables.size()];
            mImageUris.toArray(uris);

            new SaveTask(EditorActivity.this,
                    mSurface.getWidth(), mSurface.getHeight(), new Paint(mPaint)).execute(uris);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            new ImageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data.getData());
        }
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        render();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        mHandler.postAtTime(what, who, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        mHandler.removeCallbacks(what, who);
    }

    private void render() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ImageUtil.render(mSurface, mLayer, mDrawables, mPaint);
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mDrawables.size() > 0) {
            for (GlideDrawable drawable : mDrawables) {
                drawable.start();
            }
            if (mSurface != null) {
                mSurface.setVisibility(View.VISIBLE);
            }
            if (mEmpty != null) {
                mEmpty.setVisibility(View.GONE);
            }
        }
        render();
    }

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format,
                               final int width, final int height) {
        mLayer = new Layer(width, height);
        render();
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        for (GlideDrawable drawable : mDrawables) {
            drawable.stop();
        }
    }

    private class ImageTask extends AsyncTask<Uri, Void, Pair<Uri, GlideDrawable>> {

        @Override
        protected Pair<Uri, GlideDrawable> doInBackground(Uri... params) {
            try {
                final GlideDrawable drawable = Glide.with(EditorActivity.this).load(params[0])
                        .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();

                return new Pair<>(params[0], drawable);
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, "Could not load image.", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Pair<Uri, GlideDrawable> pair) {
            if (pair != null) {
                final GlideDrawable glideDrawable = pair.second;
                glideDrawable.setVisible(true, true);

                if (mSurface != null) {
                    mSurface.setVisibility(View.VISIBLE);
                }
                if (mEmpty != null) {
                    mEmpty.setVisibility(View.GONE);
                }

                mDrawables.add(glideDrawable);
                mImageUris.add(pair.first);
                if (mEditorAdapter != null) {
                    mEditorAdapter.add(pair.first);
                }

                if (glideDrawable.isAnimated()) {
                    glideDrawable.setCallback(EditorActivity.this);
                    glideDrawable.start();
                } else {
                    render();
                }
            }
        }
    }
}
