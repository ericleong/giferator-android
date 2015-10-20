package com.dreamynomad.giferator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.view.Gravity;
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

/**
 * Created by Eric on 10/19/2015.
 */
public class EditorPagerAdapter extends PagerAdapter {

    public interface OnInteractionListener {
        void add();
        void clear();
        void ratio(float ratio);
        Uri[] getUris();
    }

    private EditorActivity mActivity;
    private LinearLayout mLayers;
    private LinearLayout mBlends;
    private OnInteractionListener mListener;

    public EditorPagerAdapter(EditorActivity activity, OnInteractionListener listener) {
        mActivity = activity;
        mListener = listener;
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        final View view;

        switch (position) {
            case 0:
                view = mActivity.getLayoutInflater().inflate(R.layout.page_layers, container, false);
                mLayers = (LinearLayout) view.findViewById(R.id.list_layers);

                final ImageView add = (ImageView) view.findViewById(R.id.add);
                add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null) {
                            mListener.add();
                        }
                    }
                });

                final ImageView clear = (ImageView) view.findViewById(R.id.clear);
                clear.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null) {
                            mListener.clear();
                        }

                        mLayers.removeAllViews();
                        updateBlends();
                    }
                });

                break;

            case 1: {
                final HorizontalScrollView scrollView =
                        new HorizontalScrollView(mActivity);
                scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                view = scrollView;

                mBlends = new LinearLayout(mActivity);
                mBlends.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

                scrollView.addView(mBlends);

                updateBlends();

                break;
            }
            case 2:
                final HorizontalScrollView scrollView =
                        new HorizontalScrollView(mActivity);
                scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                view = scrollView;

                final LinearLayout linearLayout = new LinearLayout(mActivity);
                linearLayout.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

                scrollView.addView(linearLayout);

                final int size = mActivity.getResources().getDimensionPixelSize(R.dimen.item_ratio_width);

                for (final Ratio ratio : Ratio.values()) {
                    final TextView textView = new TextView(mActivity);
                    textView.setLayoutParams(new LinearLayout.LayoutParams(size,
                            LinearLayout.LayoutParams.MATCH_PARENT));
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextSize(20);
                    textView.setText(ratio.toString());
                    textView.setBackground(mActivity.getResources().getDrawable(R.drawable.editor_button));

                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mListener != null) {
                                mListener.ratio(ratio.getRatio());
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
            final ImageView imageView = new ImageView(mActivity);
            final int size = mActivity.getResources().getDimensionPixelSize(R.dimen.pager_height);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mLayers.addView(imageView);

            Glide.with(mActivity).load(uri).asBitmap().into(imageView);

            updateBlends();
        }
    }

    public void updateBlends() {

        if (mListener != null) {
            final Uri[] uris = mListener.getUris();

            final int size = mActivity.getResources().getDimensionPixelSize(R.dimen.pager_height);

            new BlendTask(mActivity, size, mBlends, mActivity)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uris);
        }
    }
}
