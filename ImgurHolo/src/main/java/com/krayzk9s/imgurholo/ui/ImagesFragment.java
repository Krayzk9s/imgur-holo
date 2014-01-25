package com.krayzk9s.imgurholo.ui;

/*
 * Copyright 2013 Kurt Zimmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.ActionBar;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.krayzk9s.imgurholo.BuildConfig;
import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImageSelectActivity;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.libs.SquareImageView;
import com.krayzk9s.imgurholo.services.DownloadService;
import com.krayzk9s.imgurholo.tools.AddImagesToAlbumAsync;
import com.krayzk9s.imgurholo.tools.ApiCall;
import com.krayzk9s.imgurholo.tools.CommentsAsync;
import com.krayzk9s.imgurholo.tools.Fetcher;
import com.krayzk9s.imgurholo.tools.GetData;
import com.krayzk9s.imgurholo.tools.ImageUtils;
import com.krayzk9s.imgurholo.tools.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import it.gmariotti.cardslib.library.internal.base.BaseCard;
import it.gmariotti.cardslib.library.view.CardListView;
import it.gmariotti.cardslib.library.view.CardView;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * Copyright 2013 Kurt Zimmer
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ImagesFragment extends Fragment implements GetData, OnRefreshListener {

    protected final static String DELETE = "delete";
    protected final static String IMAGES = "images";
    public boolean selecting = false;
    protected ImageAdapter imageAdapter;
    protected String imageCall;
    protected GridView gridview;
    protected ArrayList<String> intentReturn;
    protected String albumId;
    protected JSONObject galleryAlbumData;
    protected TextView noImageView;
    protected int page;
    protected int lastInView = -1;
    protected ArrayList<String> urls;
    protected ArrayList<JSONParcelable> ids;
    protected TextView errorText;
    protected CardListView cardListView;
    protected ArrayList<Card> cards;
    protected CardArrayAdapter mCardArrayAdapter;
    protected boolean fetchingImages;
    protected PullToRefreshLayout mPullToRefreshLayout;
    protected ActionBar actionBar;
    protected int oldWidth;
    protected boolean isGridView;

    public ImagesFragment() {
        page = 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        actionBar = activity.getActionBar();
        SharedPreferences settings = activity.getApiCall().settings;
        Bundle bundle = getArguments();
        if(bundle != null && bundle.containsKey("id"))
            isGridView = settings.getString("GalleryLayout", getString(R.string.card_view)).equals(getString(R.string.grid_view));
        else {
            isGridView = settings.getString("ImagesLayout", getString(R.string.grid_view)).equals(getString(R.string.grid_view));
        }
        if (savedInstanceState != null) {
            urls = savedInstanceState.getStringArrayList("urls");
            ids = savedInstanceState.getParcelableArrayList("ids");
            page = savedInstanceState.getInt("page");
        } else {
            urls = new ArrayList<String>();
            ids = new ArrayList<JSONParcelable>();
            page = 0;
        }

        if(bundle == null) {
            return;
        }
        if (bundle.containsKey("id"))
            albumId = bundle.getString("id");
        else
            albumId = null;
        if(bundle.containsKey("imageCall"))
            imageCall = bundle.getString("imageCall");
        if (bundle.containsKey("albumData")) {
            JSONParcelable dataParcel = bundle.getParcelable("albumData");
            if (dataParcel != null)
                galleryAlbumData = dataParcel.getJSONObject();
        } else
            galleryAlbumData = null;
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        if (activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
            inflater.inflate(R.menu.main, menu);
        else
            inflater.inflate(R.menu.main_dark, menu);
        menu.findItem(R.id.action_upload).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);
        menu.findItem(R.id.action_download).setVisible(true);
        if (albumId != null && galleryAlbumData == null) {
            menu.findItem(R.id.action_new).setVisible(true);
        }
        if (albumId != null && galleryAlbumData != null) {
            menu.findItem(R.id.action_comments).setVisible(true);
        }
        if (albumId != null) {
            menu.findItem(R.id.action_copy).setVisible(true);
            menu.findItem(R.id.action_share).setVisible(true);
        }
    }

    @Override
    public void onRefreshStarted(View view) {
        page = 0;
        makeGallery();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        final Activity activity = getActivity();
        switch (item.getItemId()) {
            case R.id.action_download:
                 Toast.makeText(activity, String.format(
                            getActivity().getResources().getString(R.string.toast_downloading),
                            urls.size()),
                         Toast.LENGTH_SHORT)
                         .show();
                Intent serviceIntent = new Intent(activity, DownloadService.class);
                serviceIntent.putParcelableArrayListExtra("ids", ids);
                activity.startService(serviceIntent);
                return true;
            case R.id.action_refresh:
                urls = new ArrayList<String>();
                ids = new ArrayList<JSONParcelable>();
                page = 0;
                imageAdapter.notifyDataSetChanged();
                makeGallery();
                return true;
            case R.id.action_copy:
                ClipboardManager clipboard = (ClipboardManager)
                        activity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("imgur Link", "http://imgur.com/a/" + albumId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(activity, R.string.toast_copied, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_share:
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.putExtra(Intent.EXTRA_TEXT, "http://imgur.com/a/" + albumId);
                startActivity(intent);
                return true;
            case R.id.action_new:
                Intent i = new Intent(this.getActivity().getApplicationContext(), ImageSelectActivity.class);
                startActivityForResult(i, 1);
                //select image
                return true;
            case R.id.action_comments:
                CommentsAsync commentsAsync = new CommentsAsync(((ImgurHoloActivity) getActivity()), galleryAlbumData);
                commentsAsync.execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("requestcode", requestCode + "");
        if (data.getExtras() == null)
            return;
        Object bundle = data.getExtras().get("data");
        Log.d("HELO", bundle.getClass().toString());
        switch (requestCode) {
            case 1:
                super.onActivityResult(requestCode, resultCode, data);
                ArrayList<String> imageIds = data.getStringArrayListExtra("data");
                if (imageIds != null)
                    Log.d("Ids!", imageIds.toString());
                AddImagesToAlbumAsync imageAsync = new AddImagesToAlbumAsync(imageIds, ((ImgurHoloActivity) getActivity()).getApiCall(), albumId);
                imageAsync.execute();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        SharedPreferences settings = activity.getApiCall().settings;
        View view;
        if (isGridView) {
            view = inflater.inflate(R.layout.image_layout, container, false);
            errorText = (TextView) view.findViewById(R.id.error);
            gridview = (GridView) view.findViewById(R.id.grid_layout);
            gridview.setColumnWidth(Utils.dpToPx(Integer.parseInt(settings.getString(getString(R.string.icon_size), getString(R.string.onetwenty))), getActivity()));
            imageAdapter = new ImageAdapter(view.getContext());
            gridview.setAdapter(imageAdapter);
            gridview.setOnItemClickListener(new GridItemClickListener());
            gridview.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (imageAdapter.getNumColumns() == 0 || gridview.getWidth() != oldWidth)
                                setNumColumns();
                        }
                    });
            gridview.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (lastInView == -1)
                        lastInView = firstVisibleItem;
                    else if (lastInView > firstVisibleItem) {
                        actionBar.show();
                        lastInView = firstVisibleItem;
                    } else if (lastInView < firstVisibleItem) {
                        actionBar.hide();
                        lastInView = firstVisibleItem;
                    }
                    int lastInScreen = firstVisibleItem + visibleItemCount;
                    if ((lastInScreen == totalItemCount) && urls != null && urls.size() > 0 && !fetchingImages) {
                            page += 1;
                            getImages();
                    }
                }
            });
            if(ids != null && ids.size() > 0) {
                imageAdapter.notifyDataSetChanged();
            }
            else {
                makeGallery();
            }
        } else {
            view = inflater.inflate(R.layout.image_layout_card_list, container, false);
            errorText = (TextView) view.findViewById(R.id.error);
            cardListView = (CardListView) view.findViewById(R.id.grid_layout);
            cardListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (lastInView == -1)
                        lastInView = firstVisibleItem;
                    else if (lastInView > firstVisibleItem) {
                        lastInView = firstVisibleItem;
                    } else if (lastInView < firstVisibleItem) {
                        lastInView = firstVisibleItem;
                    }
                    int lastInScreen = firstVisibleItem + visibleItemCount;
                    if ((lastInScreen == totalItemCount) && urls != null && urls.size() > 0 && !fetchingImages) {
                            page += 1;
                            getImages();
                    }
                }
            });
            if(ids != null && ids.size() > 0) {
                restoreCards();
            }
            else {
                makeGallery();
            }
        }

        mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
        // Now setup the PullToRefreshLayout
        ActionBarPullToRefresh.from(getActivity())
                // Mark All Children as pullable
                .allChildrenArePullable()
                        // Set the OnRefreshListener
                .listener(this)
                        // Finally commit the setup to our PullToRefreshLayout
                .setup(mPullToRefreshLayout);
        noImageView = (TextView) view.findViewById(R.id.no_images);
        return view;
    }

    public void onGetObject(Object object, String tag) {
        if (tag.equals(IMAGES)) {
            ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
            if (activity == null)
                return;
            SharedPreferences settings = activity.getApiCall().settings;
            JSONObject data = (JSONObject) object;
            Log.d("imagesData", "checking");
            Log.d("imagesData", "failed");
            JSONArray imageArray = new JSONArray();
            try {
                Log.d("URI", data.toString());
                imageArray = data.getJSONArray("data");
            }
            catch (JSONException e) {
                try {
                imageArray = data.getJSONObject("data").getJSONArray("images");
                }
                catch (JSONException e2) {
                    Log.e("Error!", e2.toString() + data.toString());
                }
            }
            try {
                errorText.setVisibility(View.GONE);
                Boolean changed = false;
                for (int i = 0; i < imageArray.length(); i++) {
                    JSONObject imageData = imageArray.getJSONObject(i);
                    String s = "";
                    if (isGridView)
                        s = settings.getString("IconQuality", "m");
                    try {
                        if (imageData.has("is_album") && imageData.getBoolean("is_album")) {
                            if (!urls.contains("http://imgur.com/" + imageData.getString(ImgurHoloActivity.IMAGE_DATA_COVER) + s + ".png")) {
                                changed = true;
                                urls.add("http://imgur.com/" + imageData.getString(ImgurHoloActivity.IMAGE_DATA_COVER) + s + ".png");
                                JSONParcelable dataParcel = new JSONParcelable();
                                dataParcel.setJSONObject(imageData);
                                ids.add(dataParcel);
                            }
                        } else {
                            if (!urls.contains("http://imgur.com/" + imageData.getString("id") + s + ".png")) {
                                changed = true;
                                urls.add("http://imgur.com/" + imageData.getString("id") + s + ".png");
                                JSONParcelable dataParcel = new JSONParcelable();
                                dataParcel.setJSONObject(imageData);
                                ids.add(dataParcel);
                            }
                        }
                    } catch (RejectedExecutionException e) {
                        Log.e("Rejected", e.toString());
                    }
                }
                fetchingImages = !changed;
            } catch (JSONException e) {
                Log.e("Error!", e.toString() + "here");
            }
            if (!isGridView && urls.size() > 0) {
                restoreCards();
            } else if (urls.size() > 0) {
                imageAdapter.notifyDataSetChanged();
            } else if (urls.size() == 0 && noImageView != null)
                noImageView.setVisibility(View.VISIBLE);
            else {
                fetchingImages = true;
            }
            if (mPullToRefreshLayout != null)
                mPullToRefreshLayout.setRefreshComplete();
            errorText.setVisibility(View.GONE);
        }
    }

    public void handleException(Exception e, String tag) {
        Log.e("Error!", e.toString());
        noImageView.setVisibility(View.VISIBLE);
    }

    protected void makeGallery() {
        errorText.setVisibility(View.GONE);
        urls = new ArrayList<String>();
        ids = new ArrayList<JSONParcelable>();
        if (imageAdapter != null)
            imageAdapter.notifyDataSetChanged();
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        if(!isGridView) {
            cards = new ArrayList<Card>();
            mCardArrayAdapter = new CardArrayAdapter(getActivity(), cards);
            cardListView.setAdapter(mCardArrayAdapter);
        }
        activity.invalidateOptionsMenu();
        getImages();
    }

    private void getImages() {
        fetchingImages = true;
        Log.d("refreshing", "refreshing");
        if(mPullToRefreshLayout != null)
            mPullToRefreshLayout.setRefreshing(true);
        errorText.setVisibility(View.GONE);
        Fetcher fetcher = new Fetcher(this, imageCall + "/" + page, ApiCall.GET, null, ((ImgurHoloActivity) getActivity()).getApiCall(), IMAGES);
        fetcher.execute();
    }

    private void restoreCards() {
        try {
            ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
            int start = 0;
            if(cardListView != null && cardListView.getAdapter() != null)
                start = cardListView.getAdapter().getCount();
            if(cards == null) {
                cards = new ArrayList<Card>();
                mCardArrayAdapter = new CardArrayAdapter(getActivity(), cards);
                cardListView.setAdapter(mCardArrayAdapter);
            }
            for (int i = start; i < ids.size(); i++) {
                Card card = new Card(getActivity());
                final CustomHeaderInnerCard header = new CustomHeaderInnerCard(getActivity());
                header.setTitle(ids.get(i).getJSONObject().getString("title"));
                header.position = i;
                final ImagesFragment fragment = this;
                if (ids.get(i).getJSONObject().has("subreddit"))
                    header.setPopupMenu(R.menu.card_image, new CardHeader.OnClickCardHeaderPopupMenuListener() {
                        @Override
                        public void onMenuItemClick(BaseCard card, MenuItem item) {
                            ImgurHoloActivity imgurHoloActivity = (ImgurHoloActivity) getActivity();
                            if (item.getTitle().equals(getString(R.string.rating_good)))
                                ImageUtils.upVote(fragment, ids.get(header.position), null, null, imgurHoloActivity.getApiCall());
                            if (item.getTitle().equals(getString(R.string.rating_bad)))
                                ImageUtils.downVote(fragment, ids.get(header.position), null, null, imgurHoloActivity.getApiCall());
                            if (item.getTitle().equals(getString(R.string.account)))
                                ImageUtils.gotoUser(fragment, ids.get(header.position));
                            if (item.getTitle().equals(getString(R.string.action_share)))
                                ImageUtils.shareImage(fragment, ids.get(header.position));
                            if (item.getTitle().equals(getString(R.string.action_copy)))
                                ImageUtils.copyImageURL(fragment, ids.get(header.position));
                            if (item.getTitle().equals(getString(R.string.action_download)))
                                ImageUtils.downloadImage(fragment, ids.get(header.position));
                            ImageUtils.updateImageFont(ids.get(header.position), header.scoreText);
                        }
                    });
                else
                    header.setPopupMenu(R.menu.card_image_no_account, new CardHeader.OnClickCardHeaderPopupMenuListener() {
                        @Override
                        public void onMenuItemClick(BaseCard card, MenuItem item) {
                            ImgurHoloActivity imgurHoloActivity = (ImgurHoloActivity) getActivity();
                            if (item.getTitle().equals(getString(R.string.rating_good)))
                                ImageUtils.upVote(fragment, ids.get(header.position), null, null, imgurHoloActivity.getApiCall());
                            if (item.getTitle().equals(getString(R.string.rating_bad)))
                                ImageUtils.downVote(fragment, ids.get(header.position), null, null, imgurHoloActivity.getApiCall());
                            if (item.getTitle().equals(getString(R.string.account)))
                                ImageUtils.gotoUser(fragment, ids.get(header.position));
                            if (item.getTitle().equals(getString(R.string.action_share)))
                                ImageUtils.shareImage(fragment, ids.get(header.position));
                            if (item.getTitle().equals(getString(R.string.action_copy)))
                                ImageUtils.copyImageURL(fragment, ids.get(header.position));
                            if (item.getTitle().equals(getString(R.string.action_download)))
                                ImageUtils.downloadImage(fragment, ids.get(header.position));
                            ImageUtils.updateImageFont(ids.get(header.position), header.scoreText);
                        }
                    });
                card.addCardHeader(header);
                CustomThumbCard thumb = new CustomThumbCard(getActivity());
                thumb.setHeader(header);
                thumb.setExternalUsage(true);
                thumb.position = i;
                final int position = i;
                card.addCardThumbnail(thumb);
                if (!activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
                    card.setBackgroundResourceId(R.drawable.dark_card_background);
                CardView cardView = (CardView) getActivity().findViewById(R.id.list_cardId);
                card.setCardView(cardView);
                card.setOnClickListener(new Card.OnCardClickListener() {
                    @Override
                    public void onClick(Card card, View view) {
                        selectItem(position);
                    }
                });
                cards.add(card);
            }
            mCardArrayAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            Log.e("Error!", e.toString());
        }
    }

    void selectItem(int position) {
        if (!selecting) {
            Intent intent = new Intent();
            intent.putExtra("id", ids.get(position));
            intent.setAction(ImgurHoloActivity.IMAGE_INTENT);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivity(intent);
        }
    }

    ImagesFragment getOuter() {
        return this;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putStringArrayList("urls", urls);
        savedInstanceState.putParcelableArrayList("ids", ids);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public class ImageAdapter extends BaseAdapter {
        private final Context mContext;
        CheckableLayout l;
        SquareImageView i;
        private int mNumColumns;

        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getNumColumns() {
            return mNumColumns;
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }

        @Override
        public long getItemId(int position) {
            return position < mNumColumns ? 0 : position - mNumColumns;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (position < mNumColumns) ? 1 : 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public int getCount() {
            if (urls != null)
                return urls.size() + mNumColumns;
            else
                return 0;
        }

        public Object getItem(int position) {
            return position < mNumColumns ?
                    null : ids.get(position - mNumColumns);
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position < mNumColumns) {
                if (convertView == null) {
                    convertView = new View(mContext);
                }
                if (getActivity().getActionBar() != null)
                    convertView.setLayoutParams(new AbsListView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, getActivity().getActionBar().getHeight()));
                return convertView;
            } else {
                if (convertView == null) {
                    i = new SquareImageView(mContext);
                    i.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    l = new CheckableLayout(getActivity());
                    l.setPadding(2, 2, 2, 2);
                    l.addView(i);
                } else {
                    l = (CheckableLayout) convertView;
                    i = (SquareImageView) l.getChildAt(0);
                }

                Ion.with(i).load(urls.get(position - mNumColumns));
                return l;
            }
        }
    }

    private class GridItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position - imageAdapter.getNumColumns());
        }
    }

    public class MultiChoiceModeListener implements GridView.MultiChoiceModeListener {
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle(R.string.choice_mode_items);
            mode.setSubtitle(R.string.choice_mode_one_item);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.images_multi, menu);
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    if (albumId == null) {
                        getChecked();
                        for (String anIntentReturn : intentReturn) {
                            Fetcher fetcher = new Fetcher(getOuter(), "3/image/" + anIntentReturn, ApiCall.DELETE, null, ((ImgurHoloActivity) getActivity()).getApiCall(), DELETE);
                            fetcher.execute();
                        }
                    }
                    break;
                default:
                    break;
            }
            mode.finish();
            return false;
        }

        public void onDestroyActionMode(ActionMode mode) {
            if (selecting) {
                Intent intent = new Intent();
                getChecked();
                intent.putExtra("data", intentReturn);
                ImageSelectActivity imageSelectActivity = (ImageSelectActivity) getActivity();
                imageSelectActivity.setResult(ImageSelectActivity.RESULT_OK, intent);
                imageSelectActivity.finish();
            }
        }

        private void getChecked() {
            intentReturn = new ArrayList<String>();
            try {
                for (int i = 0; i < gridview.getCount(); i++) {
                    if (gridview.isItemChecked(i)) {
                        JSONParcelable imageData = (JSONParcelable) imageAdapter.getItem(i);
                        intentReturn.add(imageData.getJSONObject().getString("id"));
                        Log.d("checkedid", imageData.getJSONObject().getString("id"));
                    }
                }
            } catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                              boolean checked) {
            int selectCount = gridview.getCheckedItemCount();
            Log.d("count", "" + selectCount);
            switch (selectCount) {
                case 1:
                    mode.setSubtitle(R.string.choice_mode_one_item);
                    break;
                default:
                    mode.setSubtitle("" + selectCount + " items selected");
                    break;
            }
        }
    }

    public class CheckableLayout extends FrameLayout implements Checkable {
        private boolean mChecked;

        public CheckableLayout(Context context) {
            super(context);
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void setChecked(boolean checked) {
            mChecked = checked;
            setBackgroundDrawable(checked ?
                    getResources().getDrawable(R.drawable.select_background)
                    : null);
        }

        public void toggle() {
            setChecked(!mChecked);
        }

    }

    private void setNumColumns() {
        Log.d("Setting Columns", "Setting Columns");
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        if (activity != null) {
            oldWidth = gridview.getWidth();
            SharedPreferences settings = activity.getApiCall().settings;
            Log.d("numColumnsWidth", gridview.getWidth() + "");
            Log.d("numColumnsIconWidth", Utils.dpToPx((Integer.parseInt(settings.getString(getString(R.string.icon_size), getString(R.string.onetwenty)))), getActivity()) + "");
            final int numColumns = (int) Math.floor(
                    gridview.getWidth() / (Utils.dpToPx((Integer.parseInt(settings.getString(getString(R.string.icon_size), getString(R.string.onetwenty)))), getActivity()) + Utils.dpToPx(4, getActivity())));
            if (numColumns > 0) {
                imageAdapter.setNumColumns(numColumns);
                if (BuildConfig.DEBUG) {
                    Log.d("NUMCOLS", "onCreateView - numColumns set to " + numColumns);
                }
                imageAdapter.notifyDataSetChanged();
            }
        }
    }

    public class CustomThumbCard extends CardThumbnail {
        public int position;
        public CustomHeaderInnerCard header;

        public CustomThumbCard(Context context) {
            super(context);
        }

        public void setHeader(CustomHeaderInnerCard _header) {
            header = _header;
        }

        @Override
        public void setupInnerViewElements(ViewGroup parent, View viewImage) {
            if (ids.size() == 0)
                return;
            if (ids.get(position).getJSONObject().has(ImgurHoloActivity.IMAGE_DATA_WIDTH)) {
                try {
                    Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    float imageWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ids.get(position).getJSONObject().getInt(ImgurHoloActivity.IMAGE_DATA_WIDTH), getResources().getDisplayMetrics());
                    float imageHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ids.get(position).getJSONObject().getInt(ImgurHoloActivity.IMAGE_DATA_HEIGHT), getResources().getDisplayMetrics());
                    viewImage.getLayoutParams().height = (int) (imageHeight * ((size.x - 32) / imageWidth));
                } catch (JSONException e) {
                    Log.e("Error!", e.toString());
                }
            } else {
                viewImage.getLayoutParams().height = 500;
                ((ImageView) viewImage).setScaleType(ImageView.ScaleType.CENTER_CROP);

            }
            Ion.with(getActivity(),urls.get(position))
                    .progressBar(header.progressBar)
                    .withBitmap()
                    .intoImageView((ImageView) viewImage)
                    .setCallback(new FutureCallback<ImageView>() {
                        @Override
                        public void onCompleted(Exception e, ImageView imageView) {
                            header.progressBar.setVisibility(View.INVISIBLE);
                        }
                    });
        }
    }

    public class CustomHeaderInnerCard extends CardHeader {
        public int position;
        public TextView scoreText;
        public ProgressBar progressBar;

        public CustomHeaderInnerCard(Context context) {
            super(context, R.layout.card_header);
        }

        @Override
        public void setupInnerViewElements(ViewGroup parent, View view) {
            if (view != null && ids.size() != 0) {
                progressBar = (ProgressBar) view.findViewById(R.id.image_progress);
                progressBar.setVisibility(View.VISIBLE);
                TextView headerText = (TextView) view.findViewById(R.id.header);
                if (headerText != null && this.getTitle() != "null") {
					headerText.setVisibility(View.VISIBLE);
                    headerText.setText(this.getTitle());
                    ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
                    if (!activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
                        headerText.setTextColor(Color.WHITE);
                }
				else {
					headerText.setVisibility(View.GONE);
				}
                scoreText = (TextView) view.findViewById(R.id.score);
                ImageUtils.updateImageFont(ids.get(position), scoreText);
                TextView infoText = (TextView) view.findViewById(R.id.info);
                ImageUtils.updateInfoFont(ids.get(position), infoText);
				TextView descriptionText = (TextView) view.findViewById(R.id.description);
				try {
					if(ids.get(position).getJSONObject().has("description") && ids.get(position).getJSONObject().getString("description") != "null" ) {
						descriptionText.setVisibility(View.VISIBLE);
						descriptionText.setText(ids.get(position).getJSONObject().getString("description"));
					}
					else {
						descriptionText.setVisibility(View.GONE);
					}
				}
				catch (JSONException e) {
					Log.e("Error!", e.toString());
				}
            }
        }
    }
}
