package com.dreamynomad.giferator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EditorActivity extends AppCompatActivity implements Drawable.Callback,
        SurfaceHolder.Callback {

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
            mEditorAdapter = new EditorPagerAdapter();
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
                renderInternal();
            }
        });
    }

    private void renderInternal() {
        if (mSurface != null && mSurface.getHolder() != null) {
            Canvas canvas = null;
            final SurfaceHolder holder = mSurface.getHolder();

            try {
                synchronized (holder) {
                    canvas = holder.lockCanvas();

                    if (canvas != null) {
                        if (mLayer == null && mDrawables.size() > 1) {
                            mLayer = new Layer(canvas.getWidth(), canvas.getHeight());
                        }

                        draw(canvas, mLayer, mDrawables, mPaint);
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

    private static void draw(@NonNull final Canvas canvas, @NonNull final Layer layer,
                             @NonNull List<GlideDrawable> drawables, @NonNull final Paint paint) {
        for (int i = 0; i < drawables.size(); i++) {
            final GlideDrawable drawable = drawables.get(i);

            ImageUtil.cover(drawable, canvas.getWidth(), canvas.getHeight());

            if (i == 0) {
                drawable.draw(canvas);
            } else {
                layer.mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                drawable.draw(layer.mCanvas);
                paint.setShader(layer.mShader);
                canvas.drawRect(0, 0, layer.mCanvas.getWidth(), layer.mCanvas.getHeight(), paint);
            }
        }
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

    private class EditorPagerAdapter extends PagerAdapter {

        private LinearLayout mLayers;
        private LinearLayout mBlends;

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            final View view;

            switch (position) {
                case 0:
                    view = getLayoutInflater().inflate(R.layout.page_layers, container, false);
                    mLayers = (LinearLayout) view.findViewById(R.id.list_layers);

                    final ImageView add = (ImageView) view.findViewById(R.id.add);
                    add.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType(TYPE_IMAGE);
                                Intent chooserIntent = Intent.createChooser(intent,
                                        getResources().getString(R.string.choose_image));
                                startActivityForResult(chooserIntent, RESULT_GALLERY);
                            } else {
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType(TYPE_IMAGE);
                                startActivityForResult(intent, RESULT_GALLERY_KITKAT);
                            }
                        }
                    });

                    final ImageView clear = (ImageView) view.findViewById(R.id.clear);
                    clear.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            for (GlideDrawable drawable : mDrawables) {
                                drawable.stop();
                            }
                            if (mSurface != null) {
                                mSurface.setVisibility(View.INVISIBLE);
                            }

                            for (Drawable drawable : mDrawables) {
                                if (drawable instanceof GifDrawable) {
                                    ((GifDrawable) drawable).recycle();
                                }
                            }

                            mDrawables.clear();
                            mImageUris.clear();
                            mLayers.removeAllViews();

                            if (mEmpty != null) {
                                mEmpty.setVisibility(View.VISIBLE);
                            }
                        }
                    });

                    break;

                case 1: {
                    final HorizontalScrollView scrollView =
                            new HorizontalScrollView(EditorActivity.this);
                    scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                    view = scrollView;

                    mBlends = new LinearLayout(EditorActivity.this);
                    mBlends.setLayoutParams(new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT));

                    scrollView.addView(mBlends);

                    updateBlends();

                    break;
                }
                case 2:
                    final HorizontalScrollView scrollView =
                            new HorizontalScrollView(EditorActivity.this);
                    scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                    view = scrollView;

                    final LinearLayout linearLayout = new LinearLayout(EditorActivity.this);
                    linearLayout.setLayoutParams(new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT));

                    scrollView.addView(linearLayout);

                    final int size = getResources().getDimensionPixelSize(R.dimen.item_ratio_width);

                    for (final Ratio ratio : Ratio.values()) {
                        final TextView textView = new TextView(EditorActivity.this);
                        textView.setLayoutParams(new LinearLayout.LayoutParams(size,
                                LinearLayout.LayoutParams.MATCH_PARENT));
                        textView.setGravity(Gravity.CENTER);
                        textView.setTextSize(20);
                        textView.setText(ratio.toString());
                        textView.setBackground(getResources().getDrawable(R.drawable.editor_button));

                        textView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (mSurface != null) {
                                    mSurface.setAspectRatio(ratio.getRatio());
                                }
                            }
                        });

                        linearLayout.addView(textView);
                    }

                    break;
                default:
                    view = null;
                    break;
            }

            if (view != null) {
                container.addView(view, position);
            }
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            // no-op
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public void add(final Uri uri) {
            if (mLayers != null) {
                final ImageView imageView = new ImageView(EditorActivity.this);
                final int size = getResources().getDimensionPixelSize(R.dimen.pager_height);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                mLayers.addView(imageView);

                Glide.with(EditorActivity.this).load(uri).asBitmap().into(imageView);

                updateBlends();
            }
        }

        public void updateBlends() {

            final int size = getResources().getDimensionPixelSize(R.dimen.pager_height);

            final Uri[] uris = new Uri[mImageUris.size()];
            mImageUris.toArray(uris);

            new AsyncTask<Uri, Void, List<Bitmap>>() {

                @Override
                protected List<Bitmap> doInBackground(Uri... params) {
                    List<Bitmap> bitmaps = new ArrayList<>(params.length);

                    try {
                        for (Uri uri : params) {
                            final Bitmap bitmap =
                                    Glide.with(EditorActivity.this).load(uri).asBitmap()
                                            .into(size, size).get();

                            if (bitmap != null) {
                                bitmaps.add(bitmap);
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        Log.e(TAG, "Could not download images.", e);
                    }

                    if (bitmaps.size() > 0) {
                        final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                        final Canvas canvas = new Canvas(bitmap);
                        final Rect rect = new Rect();
                        final Paint paint = new Paint();
                        final Xfermode base = paint.getXfermode();

                        final List<Bitmap> modes = new ArrayList<>(PorterDuff.Mode.values().length);

                        for (PorterDuff.Mode mode : PorterDuff.Mode.values()) {

                            final Xfermode test = new PorterDuffXfermode(mode);

                            for (int i = 0; i < bitmaps.size(); i++) {
                                ImageUtil.cover(bitmaps.get(i), canvas, rect);
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
                    mBlends.removeAllViews();

                    if (bitmaps == null) {
                        final int textSize =
                                getResources().getDimensionPixelSize(R.dimen.item_blend_width);

                        for (final PorterDuff.Mode mode : PorterDuff.Mode.values()) {
                            final TextView textView = new TextView(EditorActivity.this);
                            textView.setLayoutParams(new LinearLayout.LayoutParams(
                                    textSize,
                                    LinearLayout.LayoutParams.MATCH_PARENT
                            ));
                            textView.setGravity(Gravity.CENTER);
                            textView.setText(mode.toString());
                            textView.setBackground(getResources().getDrawable(R.drawable.editor_button));
                            textView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (mSurface != null) {
                                        mPaint.setXfermode(new PorterDuffXfermode(mode));
                                        render();
                                    }
                                }
                            });

                            mBlends.addView(textView);
                        }
                    } else {

                        final int textSize =
                                getResources().getDimensionPixelSize(R.dimen.item_blend_width);

                        for (int i = 0;
                             i < bitmaps.size() && i < PorterDuff.Mode.values().length; i++) {
                            final TextView textView = new TextView(EditorActivity.this);
                            textView.setLayoutParams(new LinearLayout.LayoutParams(
                                    textSize,
                                    LinearLayout.LayoutParams.MATCH_PARENT
                            ));
                            textView.setGravity(Gravity.CENTER);
                            final PorterDuff.Mode mode = PorterDuff.Mode.values()[i];
                            textView.setText(mode.toString());
                            textView.setBackground(getResources().getDrawable(R.drawable.editor_button));
                            textView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (mSurface != null) {
                                        mPaint.setXfermode(new PorterDuffXfermode(mode));
                                        render();
                                    }
                                }
                            });
                            final BitmapDrawable drawable =
                                    new BitmapDrawable(getResources(), bitmaps.get(i));
                            drawable.setBounds(0, 0,
                                    bitmaps.get(i).getWidth(), bitmaps.get(i).getHeight());
                            textView.setCompoundDrawables(null, drawable, null, null);

                            mBlends.addView(textView);
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uris);
        }
    }
}
