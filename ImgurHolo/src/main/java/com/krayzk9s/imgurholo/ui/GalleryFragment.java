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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.krayzk9s.imgurholo.BuildConfig;
import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.libs.SquareImageView;
import com.krayzk9s.imgurholo.tools.ApiCall;
import com.krayzk9s.imgurholo.tools.Fetcher;
import com.krayzk9s.imgurholo.tools.GetData;
import com.krayzk9s.imgurholo.tools.ImageUtils;
import com.krayzk9s.imgurholo.tools.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class GalleryFragment extends Fragment implements GetData, OnRefreshListener {

	private ArrayList<String> urls;
	private ArrayList<JSONParcelable> ids;
	private ImageAdapter imageAdapter;
	private String sort;
	private String gallery;
	private int page;
	private String subreddit;
    private String window;
	private SpinnerAdapter mSpinnerAdapter;
	private ActionBar actionBar;
	private int selectedIndex;
	private SearchView mSearchView;
	private MenuItem searchItem;
	private String search;
	private CharSequence spinner;
	private int lastInView = -1;
	private TextView errorText;
	private GridView gridview;
	private int oldwidth = 0;
	private boolean fetchingImages;
    private PullToRefreshLayout mPullToRefreshLayout;
	private CardListView cardListView;
	private final GalleryFragment galleryFragment = this;
    private static final String IMAGES = "images";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		final MainActivity activity = (MainActivity) getActivity();
		final SharedPreferences settings = activity.getSettings();
		if (savedInstanceState != null) {
            Log.d("Restoring state", "restoring");
			gallery = savedInstanceState.getString("gallery");
			sort = savedInstanceState.getString("sort");
			window = savedInstanceState.getString("window");
			subreddit = savedInstanceState.getString(getResources().getString(R.string.subreddit));
			search = savedInstanceState.getString("search");
			urls = savedInstanceState.getStringArrayList("urls");
			ids = savedInstanceState.getParcelableArrayList("ids");
			page = savedInstanceState.getInt("page");
			selectedIndex = savedInstanceState.getInt("selectedIndex");
			spinner = savedInstanceState.getCharSequence("spinner");
		} else {
			page = 0;
			subreddit = "pics";
			gallery = settings.getString("DefaultGallery", getResources().getString(R.string.viral));
			ArrayList<String> galleryOptions = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.galleryOptions)));
			sort = getResources().getString(R.string.viralsort);
			window = getResources().getString(R.string.day);
			urls = new ArrayList<String>();
			ids = new ArrayList<JSONParcelable>();
			selectedIndex = galleryOptions.indexOf(gallery);
		}
	}

    @Override
	public void onResume() {
		super.onResume();
		getActivity().getActionBar().setTitle("");
	}

	@Override
	public void onCreateOptionsMenu(
			Menu menu, MenuInflater inflater) {
		ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
		if (activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
			inflater.inflate(R.menu.main, menu);
		else
			inflater.inflate(R.menu.main_dark, menu);
		menu.findItem(R.id.action_sort).setVisible(true);
		menu.findItem(R.id.subreddit).setVisible(true);
		menu.findItem(R.id.action_upload).setVisible(false);
		menu.findItem(R.id.action_refresh).setVisible(true);

		menu.findItem(R.id.action_search).setVisible(true);

		searchItem = menu.findItem(R.id.action_search);
		mSearchView = (SearchView) searchItem.getActionView();
		SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String newText) {
				// Do nothing
				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				Log.d("searching", mSearchView.getQuery() + "");
				mSpinnerAdapter.add("search: " + mSearchView.getQuery());
				subreddit = null;
				if (mSpinnerAdapter.getCount() > 6) {
					mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
				}
				gallery = "search";
				search = mSearchView.getQuery() + "";
				searchItem.collapseActionView();
				actionBar.setSelectedNavigationItem(5);
				makeGallery();
				return true;
			}
		};
		mSearchView.setOnQueryTextListener(queryTextListener);
		menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
		menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortBest).setVisible(false);
		menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		MainActivity activity = (MainActivity) getActivity();
		page = 0;
		switch (item.getItemId()) {
			case R.id.action_sort:
				Log.d("sorting", "sorting");
				return true;
			case R.id.action_refresh:
				page = 0;
				makeGallery();
				return true;
			case R.id.subreddit:
				gallery = getResources().getString(R.string.subreddit);
				sort = getResources().getString(R.string.time);
				final EditText subredditText = new EditText(activity);
				subredditText.setSingleLine();
				new AlertDialog.Builder(activity).setTitle("Choose SubReddit").setView(subredditText).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if (subredditText.getText() != null)
							subreddit = subredditText.getText().toString();
						mSpinnerAdapter.add("/r/" + subreddit);
						search = null;
						if (mSpinnerAdapter.getCount() > 6) {
							mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
						}
						mSpinnerAdapter.notifyDataSetChanged();
						actionBar.setSelectedNavigationItem(5);
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Do nothing.
					}
				}).show();
				return true;
			case R.id.menuSortPopularity:
				sort = getResources().getString(R.string.viralsort);
				break;
			case R.id.menuSortNewest:
				sort = getResources().getString(R.string.time);
				break;
			case R.id.menuSortTop:
				sort = getResources().getString(R.string.top);
				break;
			case R.id.menuSortDay:
				sort = getResources().getString(R.string.top);
				window = getResources().getString(R.string.day);
				break;
			case R.id.menuSortWeek:
				sort = getResources().getString(R.string.top);
				window = getResources().getString(R.string.week);
				break;
			case R.id.menuSortMonth:
				sort = getResources().getString(R.string.top);
				window = getResources().getString(R.string.month);
				break;
			case R.id.menuSortYear:
				sort = getResources().getString(R.string.top);
				window = getResources().getString(R.string.year);
				break;
			case R.id.menuSortAll:
				sort = getResources().getString(R.string.top);
				window = getResources().getString(R.string.all);
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		makeGallery();
		return true;
	}

	@Override
	public void onRefreshStarted(View view) {
		page = 0;
		makeGallery();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MainActivity activity = (MainActivity) getActivity();
		actionBar = activity.getActionBar();
		SharedPreferences settings = activity.getSettings();
		View view;
		if (settings.getString("GalleryLayout", getString(R.string.card_view)).equals("Grid View")) {
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
							if (imageAdapter.getNumColumns() == 0 || gridview.getWidth() != oldwidth)
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
						if (!gallery.equals("search")) {
							page += 1;
							getImages();
						}
					}
				}
			});
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
                        if (!gallery.equals("search")) {
                            page += 1;
                            getImages();
                        }
                    }
                }
            });
            restoreCards();
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
        setupActionBar();
		if (urls.size() < 1) {
			Log.d("urls", urls.size() + "");
			makeGallery();
		}
		return view;
	}

	private void setNumColumns() {
		Log.d("Setting Columns", "Setting Columns");
		MainActivity activity = (MainActivity) getActivity();
		if (activity != null) {
			oldwidth = gridview.getWidth();
			SharedPreferences settings = activity.getSettings();
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

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		if (sort == null || (sort.equals(getResources().getString(R.string.viralsort)) && !gallery.equals(getResources().getString(R.string.top))))
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setChecked(true);
		else if (sort.equals(getResources().getString(R.string.time)) && !gallery.equals(getResources().getString(R.string.top)))
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortNewest).setChecked(true);
		else if (window.equals(getResources().getString(R.string.day)))
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setChecked(true);
		else if (window.equals(getResources().getString(R.string.week)))
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setChecked(true);
		else if (window.equals(getResources().getString(R.string.month)))
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setChecked(true);
		else if (window.equals(getResources().getString(R.string.year)))
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setChecked(true);
		else if (window.equals(getResources().getString(R.string.all)))
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setChecked(true);

		if (gallery == null || gallery.equals(getResources().getString(R.string.viral)) || gallery.equals(getResources().getString(R.string.user))) {
			menu.findItem(R.id.action_sort).setVisible(true);
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
			//menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(true);
		} else if (gallery.equals(getResources().getString(R.string.top))) {
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(false);
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortNewest).setVisible(false);
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setVisible(true);
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setVisible(true);
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setVisible(true);
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setVisible(true);
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setVisible(true);
		} else if (gallery.equals(getResources().getString(R.string.memes))) {
			//menu.findItem(R.id.action_sort).setVisible(true);
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(true);
			//menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(true);
			if (sort.equals(getResources().getString(R.string.top))) {
				menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
				menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setVisible(true);
				menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setVisible(true);
				menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setVisible(true);
				menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setVisible(true);
				menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setVisible(true);
			}
		} else if (gallery.equals(getResources().getString(R.string.random))) {
			menu.findItem(R.id.action_sort).setVisible(false);
		} else if (gallery.equals(getResources().getString(R.string.subreddit))) {
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(true);
			menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(false);
			//menu.findItem(R.id.action_sort).setVisible(true);
			if (sort.equals(getResources().getString(R.string.top))) {
				menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
				menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setVisible(true);
				menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setVisible(true);
				menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setVisible(true);
				menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setVisible(true);
				menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setVisible(true);
			}
		}
	}

	private void makeGallery() {
		if (mPullToRefreshLayout != null)
			mPullToRefreshLayout.setRefreshing(true);
		urls = new ArrayList<String>();
		ids = new ArrayList<JSONParcelable>();
		if (imageAdapter != null)
			imageAdapter.notifyDataSetChanged();
		MainActivity activity = (MainActivity) getActivity();
		activity.invalidateOptionsMenu();
		getImages();
	}


	private void getImages() {
		fetchingImages = true;
		errorText.setVisibility(View.GONE);
		String call = "";
		if (gallery.equals(getResources().getString(R.string.viral))) {
			call = "3/gallery/hot/" + sort + "/" + window + "/" + page;
		} else if (gallery.equals(getResources().getString(R.string.top))) {
			call = "3/gallery/top/" + sort + "/" + window + "/" + page;
		} else if (gallery.equals(getResources().getString(R.string.user))) {
			call = "3/gallery/user/" + sort + "/" + window + "/" + page;
		} else if (gallery.equals(getResources().getString(R.string.memes))) {
			call = "3/gallery/g/memes/" + sort + "/" + window + "/" + page;
		} else if (gallery.equals(getResources().getString(R.string.random))) {
			call = "3/gallery/random/random/" + page;
		} else if (gallery.equals(getResources().getString(R.string.subreddit))) {
			call = "3/gallery/r/" + subreddit + "/" + sort + "/" + window + "/" + page;
		} else if (gallery.equals("search")) {
			call = "3/gallery/search?q=" + search;
		}
		Fetcher fetcher = new Fetcher(this, call, ApiCall.GET, null, ((ImgurHoloActivity) getActivity()).getApiCall(), IMAGES);
		fetcher.execute();
	}

	public void handleException(Exception e, String tag) {
		Log.e("Error!", e.toString());
		//errorText.setVisibility(View.VISIBLE);
	}

	public void onGetObject(Object object, String tag) {
        if(tag.equals(IMAGES)) {
            ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
            SharedPreferences settings = activity.getApiCall().settings;
            JSONObject data = (JSONObject) object;
            Log.d("imagesData", "checking");
            if (activity == null || data == null) {
                //return;
            }
            Log.d("imagesData", "failed");
            try {
                Log.d("URI", data.toString());
                JSONArray imageArray = data.getJSONArray("data");
                errorText.setVisibility(View.GONE);
                for (int i = 0; i < imageArray.length(); i++) {
                    JSONObject imageData = imageArray.getJSONObject(i);
                    String s = "";
                    if (!settings.getString("GalleryLayout", "Card View").equals("Card View"))
                        s = settings.getString("IconQuality", "m");
                    try {
                        if (imageData.has("is_album") && imageData.getBoolean("is_album")) {
                            if (!urls.contains("http://imgur.com/" + imageData.getString(ImgurHoloActivity.IMAGE_DATA_COVER) + s + ".png")) {
                                urls.add("http://imgur.com/" + imageData.getString(ImgurHoloActivity.IMAGE_DATA_COVER) + s + ".png");
                                JSONParcelable dataParcel = new JSONParcelable();
                                dataParcel.setJSONObject(imageData);
                                ids.add(dataParcel);
                            }
                        } else {
                            if (!urls.contains("http://imgur.com/" + imageData.getString("id") + s + ".png")) {
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
            } catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
            if (settings.getString("GalleryLayout", "Card View").equals("Card View")) {
                restoreCards();
            } else {
                imageAdapter.notifyDataSetChanged();
            }
            if (mPullToRefreshLayout != null)
                mPullToRefreshLayout.setRefreshComplete();
            fetchingImages = false;
            errorText.setVisibility(View.GONE);
        }
	}

    private void restoreCards() {
        try {
            ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
            ArrayList<Card> cards = new ArrayList<Card>();
            for (int i = 0; i < ids.size(); i++) {
                Card card = new Card(getActivity());
                final CustomHeaderInnerCard header = new CustomHeaderInnerCard(getActivity());
                header.setTitle(ids.get(i).getJSONObject().getString("title"));
                header.position = i;
                if(ids.get(i).getJSONObject().has("subreddit"))
                    header.setPopupMenu(R.menu.card_image, new CardHeader.OnClickCardHeaderPopupMenuListener() {
                    @Override
                    public void onMenuItemClick(BaseCard card, MenuItem item) {
                        ImgurHoloActivity imgurHoloActivity = (ImgurHoloActivity) getActivity();
                        if(item.getTitle().equals(getString(R.string.rating_good)))
                            ImageUtils.upVote(galleryFragment, ids.get(header.position), null, null, imgurHoloActivity.getApiCall());
                        if(item.getTitle().equals(getString(R.string.rating_bad)))
                            ImageUtils.downVote(galleryFragment, ids.get(header.position), null, null, imgurHoloActivity.getApiCall());
                        if(item.getTitle().equals(getString(R.string.account)))
                            ImageUtils.gotoUser(galleryFragment, ids.get(header.position));
                        if(item.getTitle().equals(getString(R.string.action_share)))
                            ImageUtils.shareImage(galleryFragment, ids.get(header.position));
                        if(item.getTitle().equals(getString(R.string.action_copy)))
                            ImageUtils.copyImageURL(galleryFragment, ids.get(header.position));
                        if(item.getTitle().equals(getString(R.string.action_download)))
                            ImageUtils.downloadImage(galleryFragment, ids.get(header.position));
                        ImageUtils.updateImageFont(ids.get(header.position), header.scoreText);
                    }
                });
                else
                    header.setPopupMenu(R.menu.card_image_no_account, new CardHeader.OnClickCardHeaderPopupMenuListener() {
                        @Override
                        public void onMenuItemClick(BaseCard card, MenuItem item) {
                            ImgurHoloActivity imgurHoloActivity = (ImgurHoloActivity) getActivity();
                            if(item.getTitle().equals(getString(R.string.rating_good)))
                                ImageUtils.upVote(galleryFragment, ids.get(header.position), null, null, imgurHoloActivity.getApiCall());
                            if(item.getTitle().equals(getString(R.string.rating_bad)))
                                ImageUtils.downVote(galleryFragment, ids.get(header.position), null, null, imgurHoloActivity.getApiCall());
                            if(item.getTitle().equals(getString(R.string.account)))
                                ImageUtils.gotoUser(galleryFragment, ids.get(header.position));
                            if(item.getTitle().equals(getString(R.string.action_share)))
                                ImageUtils.shareImage(galleryFragment, ids.get(header.position));
                            if(item.getTitle().equals(getString(R.string.action_copy)))
                                ImageUtils.copyImageURL(galleryFragment, ids.get(header.position));
                            if(item.getTitle().equals(getString(R.string.action_download)))
                                ImageUtils.downloadImage(galleryFragment, ids.get(header.position));
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
            CardArrayAdapter mCardArrayAdapter = new CardArrayAdapter(getActivity(), cards);
            if (cardListView != null) {
                cardListView.setAdapter(mCardArrayAdapter);
            }
        } catch (JSONException e) {
            Log.e("Error!", e.toString());
        }
    }

	public class ImageAdapter extends BaseAdapter {
		private final Context mContext;
		private int mNumColumns;

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}

		public ImageAdapter(Context c) {
			mContext = c;
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

		public int getCount() {
			return urls.size() + mNumColumns;
		}

		public Object getItem(int position) {
			return position < mNumColumns ?
					null : urls.get(position - mNumColumns);
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
				ImageView imageView;
				if (convertView == null) {
					imageView = new SquareImageView(mContext);
					imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				} else {
					imageView = (ImageView) convertView;
				}
				Ion.with(imageView).load(urls.get(position - mNumColumns));
				return imageView;
			}
		}

	}

	private class GridItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position - imageAdapter.getNumColumns());
		}
	}

	void selectItem(int position) {
		Intent intent = new Intent();
		intent.putExtra("start", position);
		intent.putExtra("ids", new ArrayList<JSONParcelable>(ids));
		intent.setAction(ImgurHoloActivity.IMAGE_PAGER_INTENT);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		startActivity(intent);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		MainActivity activity = (MainActivity) getActivity();
		ActionBar actionBar = activity.getActionBar();
		if (actionBar != null)
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		// Save the user's current state
		savedInstanceState.putString("gallery", gallery);
		savedInstanceState.putString("sort", sort);
		savedInstanceState.putString("window", window);
		savedInstanceState.putString(getResources().getString(R.string.subreddit), subreddit);
		savedInstanceState.putString("search", search);
		savedInstanceState.putStringArrayList("urls", urls);
		savedInstanceState.putParcelableArrayList("ids", ids);
		savedInstanceState.putInt("page", page);
		savedInstanceState.putInt("selectedIndex", selectedIndex);
		if (mSpinnerAdapter != null && mSpinnerAdapter.getCount() > 5)
			savedInstanceState.putCharSequence("spinner", mSpinnerAdapter.getItem(5));
		else
			savedInstanceState.putCharSequence("spinner", null);
		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	public class SpinnerAdapter extends ArrayAdapter<CharSequence> {
		final Context mContext;

		public SpinnerAdapter(Context context, List<CharSequence> options) {
			super(context, android.R.layout.simple_spinner_dropdown_item, options);
			mContext = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater =
					(LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			ViewHolder holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.gallery_spinner, null);
				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.action_bar_title);
				holder.subtitle = (TextView) convertView.findViewById(R.id.action_bar_subtitle);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.title.setText("Gallery");
			holder.subtitle.setText(getItem(position));
			return convertView;
		}

		class ViewHolder {
			TextView title;
			TextView subtitle;
		}
	}

	private void setupActionBar() {
		MainActivity activity = (MainActivity) getActivity();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		Resources res = activity.getResources();
		ArrayList options = new ArrayList(Arrays.asList(res.getStringArray(R.array.galleryOptions)));
		mSpinnerAdapter = new SpinnerAdapter(activity, options);
		if (spinner != null)
			mSpinnerAdapter.add(spinner);
		ActionBar.OnNavigationListener mNavigationCallback = new ActionBar.OnNavigationListener() {
			@Override
			public boolean onNavigationItemSelected(int i, long l) {
				String newGallery = "";
				switch (i) {
					case 0:
						newGallery = getResources().getString(R.string.viral);
						sort = getResources().getString(R.string.viralsort);
						break;
					case 1:
						newGallery = getResources().getString(R.string.top);
						sort = getResources().getString(R.string.day);
						break;
					case 2:
						newGallery = getResources().getString(R.string.user);
						sort = getResources().getString(R.string.viralsort);
						break;
					case 3:
						newGallery = getResources().getString(R.string.memes);
						sort = getResources().getString(R.string.viralsort);
						break;
					case 4:
						newGallery = getResources().getString(R.string.random);
						break;
				}
				if (newGallery.equals(gallery))
					return true;
				else if (i < 5)
					gallery = newGallery;
				selectedIndex = i;
				if (mSpinnerAdapter.getCount() > 5 && !gallery.equals(getResources().getString(R.string.subreddit)) && !gallery.equals("search")) {
					mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
					subreddit = null;
					search = null;
				}
				page = 0;
				makeGallery();
				return true;
			}
		};
		Log.d("gallery", gallery);
		if (selectedIndex == 5) {
			if (subreddit != null)
				mSpinnerAdapter.add("/r/" + subreddit);
			else
				mSpinnerAdapter.add("search: " + search);
		}
		actionBar.setSelectedNavigationItem(selectedIndex);
		Log.d("Setting Item", "Setting Item");
		actionBar.setListNavigationCallbacks(mSpinnerAdapter, mNavigationCallback);
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
			if(ids.get(position).getJSONObject().has(ImgurHoloActivity.IMAGE_DATA_WIDTH)) {
				try {
					Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
					Point size = new Point();
					display.getSize(size);
					float imageWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ids.get(position).getJSONObject().getInt(ImgurHoloActivity.IMAGE_DATA_WIDTH), getResources().getDisplayMetrics());
					float imageHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ids.get(position).getJSONObject().getInt(ImgurHoloActivity.IMAGE_DATA_HEIGHT), getResources().getDisplayMetrics());
					viewImage.getLayoutParams().height = (int) (imageHeight * ((size.x - 32) / imageWidth));
				}
				catch (JSONException e) {
					Log.e("Error!", e.toString());
				}
			}
			else {
				viewImage.getLayoutParams().height = 500;
				((ImageView) viewImage).setScaleType(ImageView.ScaleType.CENTER_CROP);

			}
			if(urls.size() != 0)
				Ion.with(getActivity()).load(urls.get(position)).progressBar(header.progressBar).withBitmap().intoImageView((ImageView) viewImage).setCallback(new FutureCallback<ImageView>() {
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
				if (headerText != null) {
					headerText.setText(this.getTitle());
					ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
					if (!activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
						headerText.setTextColor(Color.WHITE);
				}
				scoreText = (TextView) view.findViewById(R.id.score);
				ImageUtils.updateImageFont(ids.get(position), scoreText);
				TextView infoText = (TextView) view.findViewById(R.id.info);
				ImageUtils.updateInfoFont(ids.get(position), infoText);
			}
		}
	}
}