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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;
import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.libs.SquareImageView;
import com.krayzk9s.imgurholo.tools.ApiCall;
import com.krayzk9s.imgurholo.tools.Fetcher;
import com.krayzk9s.imgurholo.tools.GetData;
import com.krayzk9s.imgurholo.tools.NewAlbumAsync;
import com.krayzk9s.imgurholo.tools.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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
public class AlbumsFragment extends Fragment implements GetData, OnRefreshListener {

	private ImageAdapter imageAdapter;
	private String username;
	private TextView noImageView;
	private ArrayList<String> urls;
	private ArrayList<String> ids;
	private int lastInView = -1;
	private TextView errorText;
	private PullToRefreshLayout mPullToRefreshLayout;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = getArguments();
		username = bundle.getString("username");
		setHasOptionsMenu(true);
	}


	@Override
	public void onCreateOptionsMenu(
			Menu menu, MenuInflater inflater) {
		ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
		if (activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
			inflater.inflate(R.menu.main, menu);
		else
			inflater.inflate(R.menu.main_dark, menu);
		if (username.equals("me"))
			menu.findItem(R.id.action_new).setVisible(true);
		menu.findItem(R.id.action_upload).setVisible(false);
		menu.findItem(R.id.action_refresh).setVisible(true);
	}

	@Override
	public void onRefreshStarted(View view) {
		urls = new ArrayList<String>();
		imageAdapter.notifyDataSetChanged();
		getImages();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// handle item selection
		Activity activity = getActivity();
		switch (item.getItemId()) {
			case R.id.action_refresh:
				urls = new ArrayList<String>();
				imageAdapter.notifyDataSetChanged();
				getImages();
				return true;
			case R.id.action_new:
				final EditText newTitle = new EditText(activity);
				newTitle.setSingleLine();
				newTitle.setHint(R.string.hint_album_title);
				final EditText newDescription = new EditText(activity);
				newDescription.setHint(R.string.body_hint_description);
				LinearLayout linearLayout = new LinearLayout(activity);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.addView(newTitle);
				linearLayout.addView(newDescription);
				new AlertDialog.Builder(activity).setTitle(R.string.dialog_new_album_title)
						.setView(linearLayout).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						NewAlbumAsync messagingAsync = new NewAlbumAsync(newTitle.getText().toString(), newDescription.getText().toString(), ((ImgurHoloActivity) getActivity()).getApiCall(), null, null);
						messagingAsync.execute();
					}
				}).setNegativeButton(R.string.dialog_answer_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Do nothing.
					}
				}).show();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		boolean newData = false;
		if (savedInstanceState == null && urls == null) {
			urls = new ArrayList<String>();
			ids = new ArrayList<String>();
			newData = true;
		} else if (savedInstanceState != null) {
			urls = savedInstanceState.getStringArrayList("urls");
			ids = savedInstanceState.getStringArrayList("ids");
		}
		View view = inflater.inflate(R.layout.image_layout, container, false);
		errorText = (TextView) view.findViewById(R.id.error);
		mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
		ActionBarPullToRefresh.from(getActivity())
				// Mark All Children as pullable
				.allChildrenArePullable()
						// Set the OnRefreshListener
				.listener(this)
						// Finally commit the setup to our PullToRefreshLayout
				.setup(mPullToRefreshLayout);
		ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) errorText.getLayoutParams();
		mlp.setMargins(0, 0, 0, 0);
		view.setPadding(0, getActivity().getActionBar().getHeight(), 0, 0);
		noImageView = (TextView) view.findViewById(R.id.no_images);
		GridView gridview = (GridView) view.findViewById(R.id.grid_layout);
		ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
		SharedPreferences settings = activity.getApiCall().settings;
		gridview.setColumnWidth(Utils.dpToPx(Integer.parseInt(settings.getString(getString(R.string.icon_size), getString(R.string.onetwenty))), getActivity()));
		imageAdapter = new ImageAdapter(view.getContext());
		gridview.setAdapter(imageAdapter);
		gridview.setOnItemClickListener(new GridItemClickListener());
		if (newData) {
			getImages();
		}
		gridview.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView absListView, int i) {

			}

			@Override
			public void onScroll(AbsListView absListView, int firstVisibleItem, int i2, int i3) {
				if (lastInView == -1)
					lastInView = firstVisibleItem;
				else if (lastInView > firstVisibleItem) {
					getActivity().getActionBar().show();
					lastInView = firstVisibleItem;
				} else if (lastInView < firstVisibleItem) {
					getActivity().getActionBar().hide();
					lastInView = firstVisibleItem;
				}
			}
		});
		return view;
	}

	public void handleException(Exception e, String tag) {

	}

	public void onGetObject(Object object, String tag) {
		Boolean hasImages = false;
		JSONObject imagesData = (JSONObject) object;
		try {
			JSONArray imageArray = imagesData.getJSONArray("data");
			for (int i = 0; i < imageArray.length(); i++) {
				JSONObject imageData = imageArray.getJSONObject(i);
				Log.d("adding album...", imageData.getString("id"));
				if (!imageData.getString(ImgurHoloActivity.IMAGE_DATA_COVER).equals("[[")) {
					urls.add("http://imgur.com/" + imageData.getString(ImgurHoloActivity.IMAGE_DATA_COVER) + "m.png");
					ids.add(imageData.getString("id"));
				}
			}
			hasImages = imageArray.length() > 0;

		} catch (JSONException e) {
			Log.e("Error!", e.toString());
			imagesData = null;
		}
		if (hasImages)
			imageAdapter.notifyDataSetChanged();
		else if (imagesData != null)
			noImageView.setVisibility(View.VISIBLE);
		else
			errorText.setVisibility(View.VISIBLE);
		if (mPullToRefreshLayout != null)
			mPullToRefreshLayout.setRefreshComplete();
	}

	void getImages() {
		if (mPullToRefreshLayout != null)
			mPullToRefreshLayout.setRefreshing(true);
		Fetcher fetcher = new Fetcher(this, "3/account/" + username + "/albums", ApiCall.GET, null, ((ImgurHoloActivity) getActivity()).getApiCall(), "images");
		fetcher.execute();
	}

	void selectItem(int position) {
		Intent intent = new Intent();
		String id = ids.get(position);
		intent.putExtra("imageCall", "3/album/" + id);
		intent.putExtra("id", id);
		intent.setAction(ImgurHoloActivity.IMAGES_INTENT);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		startActivity(intent);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putStringArrayList("urls", urls);
		savedInstanceState.putStringArrayList("ids", ids);
		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	public class ImageAdapter extends BaseAdapter {
		private final Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
		}

		public int getCount() {
			return urls.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {
			final ImageView imageView = new SquareImageView(mContext);
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			Ion.with(imageView).load(urls.get(position));
			return imageView;
		}

	}

	private class GridItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);
		}
	}
}
