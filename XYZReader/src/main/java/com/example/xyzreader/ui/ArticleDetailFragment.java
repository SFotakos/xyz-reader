package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.database.Cursor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.transition.Slide;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    private ImageView mPhotoView;
    private boolean mIsCard;
    private Toolbar mToolbar;
    private TextView mTitleView;
    private TextView mBylineView;
    private TextView mBodyView;
    private NestedScrollView mScrollView;
    private FloatingActionButton mFab;
    private View mPhotoProtection;
    private LinearLayout mTextTitleBar;
    private CollapsingToolbarLayout mCollapsingToolbar;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        bindViews();
        return mRootView;
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        mTitleView = (TextView) mRootView.findViewById(R.id.article_title);
        mBylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        mBylineView.setMovementMethod(new LinkMovementMethod());
        mBodyView = (TextView) mRootView.findViewById(R.id.article_body);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.imageView);
        mScrollView = (NestedScrollView) mRootView.findViewById(R.id.scrollview);
        mCollapsingToolbar = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsing_toolbar);
        mToolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
        mFab = (FloatingActionButton) mRootView.findViewById(R.id.share_fab);
        mPhotoProtection = mRootView.findViewById(R.id.imageViewProtection);
        mTextTitleBar = (LinearLayout) mRootView.findViewById(R.id.meta_bar);

        changeContentVisibility(View.INVISIBLE);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor == null || cursor.isClosed() || !cursor.moveToFirst()) {
            return;
        }

        mCursor = cursor;

        final String title = cursor.getString(ArticleLoader.Query.TITLE);
        final String body = Html.fromHtml(cursor.getString(ArticleLoader.Query.BODY)).toString();
        String photo = cursor.getString(ArticleLoader.Query.PHOTO_URL);

        if (mToolbar != null) {
            mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().finish();
                }
            });
            if (mCollapsingToolbar != null) {
                mCollapsingToolbar.setTitle(title);
                mCollapsingToolbar.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));
            }
        }

        mTitleView.setText(title);
        mBodyView.setText(body);

        Date publishedDate = parsePublishedDate();
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            mBylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " " + mCursor.getString(ArticleLoader.Query.AUTHOR)));

        } else {
            // If date is before 1902, just show the string
            mBylineView.setText(Html.fromHtml(outputFormat.format(publishedDate) + " by " +
                    mCursor.getString(ArticleLoader.Query.AUTHOR)));

        }

        Picasso.get().load(photo).into(mPhotoView, new Callback() {
            @Override
            public void onSuccess() {
                Bitmap bitmap = ((BitmapDrawable) mPhotoView.getDrawable()).getBitmap();
                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                    public void onGenerated(Palette palette) {
                        applyPalette(palette);
                    }
                });
            }

            @Override
            public void onError(Exception e) {

            }
        });
        mPhotoView.setVisibility(View.VISIBLE);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(
                        ShareCompat.IntentBuilder.from(getActivity())
                                .setType("text/plain")
                                .setText(mTitleView.getText().toString() + "\n" +
                                        mBylineView.getText().toString() + ".\n" +
                                        "Check it out at our RSS feeds app, XYZreader")
                                .getIntent(), getString(R.string.action_share)));
            }
        });

        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        changeContentVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void applyPalette(Palette palette) {
        int primaryDark = getResources().getColor(R.color.colorPrimaryDark);
        int primary = getResources().getColor(R.color.colorPrimary);
        int lightVibrantColor =
                palette.getLightVibrantColor(getResources().getColor(android.R.color.white));
        int vibrantColor = palette.getVibrantColor(getResources().getColor(R.color.colorAccent));

        mCollapsingToolbar.setContentScrimColor(palette.getDarkMutedColor(primaryDark));
        mTextTitleBar.setBackgroundColor(palette.getDarkMutedColor(primaryDark));
        mFab.setRippleColor(lightVibrantColor);
        mFab.setBackgroundTintList(ColorStateList.valueOf(vibrantColor));
    }

    private void changeContentVisibility(int visibility) {
        mPhotoView.setVisibility(visibility);
        mTextTitleBar.setVisibility(visibility);
        mTitleView.setVisibility(visibility);
        mBylineView.setVisibility(visibility);
        mBodyView.setVisibility(visibility);
        mScrollView.setVisibility(visibility);
        mFab.setVisibility(visibility);
        mPhotoProtection.setVisibility(visibility);


    }
}
