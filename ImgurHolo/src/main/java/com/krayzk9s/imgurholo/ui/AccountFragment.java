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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.tools.ApiCall;
import com.krayzk9s.imgurholo.tools.Fetcher;
import com.krayzk9s.imgurholo.tools.GetData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

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
public class AccountFragment extends Fragment implements GetData, OnRefreshListener {

	private String[] mMenuList;
	private ArrayAdapter<String> adapter;
	private String username;
	private SearchView mSearchView;
	private ListView mDrawerList;
	private TextView biography;
	private TextView created;
	private TextView reputation;
	private PullToRefreshLayout mPullToRefreshLayout;
	private int refreshedCount;
	private final static String ACCOUNTDATA = "accountData";
	private final static String COUNTDATA = "countData";
	private final static String LIKEDATA = "likeData";
	private final static String COMMENTDATA = "commentData";
	private final static String ALBUMDATA = "albumData";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		refreshedCount = 0;
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
		menu.findItem(R.id.action_search).setVisible(true);
		menu.findItem(R.id.action_refresh).setVisible(true);
		MenuItem searchItem = menu.findItem(R.id.action_search);
		mSearchView = (SearchView) searchItem.getActionView();
		mSearchView.setQueryHint("Lookup Users");
		SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String newText) {
				// Do nothing
				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				Log.d("searching", mSearchView.getQuery() + "");
				Intent intent = new Intent();
				intent.putExtra("username", mSearchView.getQuery().toString());
				intent.setAction(ImgurHoloActivity.ACCOUNT_INTENT);
				intent.addCategory(Intent.CATEGORY_DEFAULT);
				startActivity(intent);
				return true;
			}
		};
		mSearchView.setOnQueryTextListener(queryTextListener);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// handle item selection

		switch (item.getItemId()) {
			case R.id.action_refresh:
				getAccount();
			default:
				Log.d("Error!", "no action for that...");
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		Log.d("Username", username);
		ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
		SharedPreferences settings = activity.getApiCall().settings;
		Log.d("SettingTitle", username);
		View view = inflater.inflate(R.layout.account_layout, container, false);
		mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
		ActionBarPullToRefresh.from(getActivity())
				// Mark All Children as pullable
				.allChildrenArePullable()
						// Set the OnRefreshListener
				.listener(this)
						// Finally commit the setup to our PullToRefreshLayout
				.setup(mPullToRefreshLayout);
		LinearLayout header = (LinearLayout) view.findViewById(R.id.header);
		if (settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
			header.setBackgroundColor(0xFFCCCCCC);
		biography = (TextView) view.findViewById(R.id.biography);
		TextView usernameText = (TextView) view.findViewById(R.id.username);
		usernameText.setText(username);
		created = (TextView) view.findViewById(R.id.created);
		reputation = (TextView) view.findViewById(R.id.reputation);
		mDrawerList = (ListView) view.findViewById(R.id.account_list);
		mMenuList = getResources().getStringArray(R.array.accountMenu);
		adapter = new ArrayAdapter<String>(view.getContext(),
				R.layout.drawer_list_item, mMenuList);
		mDrawerList.setAdapter(adapter);
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		if (savedInstanceState == null) {
			getAccount();
		}
		return view;
	}

	public void onGetObject(Object data, String tag) {
		refreshedCount++;
		if (refreshedCount == 5) {
			mPullToRefreshLayout.setRefreshComplete();
		}
		try {
			if (data == null) {
				return;
			}
			JSONObject jsonData;

        /*int duration = Toast.LENGTH_SHORT;
				Toast toast;
                MainActivity activity = (MainActivity) getActivity();
                toast = Toast.makeText(activity, "User not found", duration);
                toast.show();
                activity.getFragmentManager().popBackStack();*/

			if (tag.equals(ACCOUNTDATA)) {
				jsonData = ((JSONObject) data).getJSONObject("data");
				if (jsonData.has("error"))
					return;
				Calendar accountCreationDate = Calendar.getInstance();
				accountCreationDate.setTimeInMillis((long) jsonData.getInt("created") * 1000);
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
				String accountcreated = sdf.format(accountCreationDate.getTime());
				created.setText(accountcreated);
				reputation.setText(Integer.toString(jsonData.getInt("reputation")));
				if (jsonData.getString("bio") != null && !jsonData.getString("bio").equals("null") && !jsonData.getString("bio").equals(""))
					biography.setText(jsonData.getString("bio"));
				else
					biography.setText("No Biography");
			} else if (tag.equals(COUNTDATA)) {
				jsonData = ((JSONObject) data);
				if (jsonData.has("error"))
					return;
				if (jsonData.getInt("status") == 200)
					mMenuList[1] = mMenuList[1] + " (" + Integer.toString(jsonData.getInt("data")) + ")";
				else
					mMenuList[1] = mMenuList[1] + " (0)";
			} else if (tag.equals(ALBUMDATA)) {
				jsonData = ((JSONObject) data);
				if (jsonData.has("error"))
					return;
				if (jsonData.getInt("status") == 200)
					mMenuList[0] = mMenuList[0] + " (" + Integer.toString(jsonData.getJSONArray("data").length()) + ")";
				else
					mMenuList[0] = mMenuList[0] + " (0)";
			} else if (tag.equals(LIKEDATA)) {
				JSONArray jsonArray = ((JSONObject) data).getJSONArray("data");
				mMenuList[2] = mMenuList[2] + " (" + String.valueOf(jsonArray.length()) + ")";
			} else if (tag.equals(COMMENTDATA)) {
				jsonData = ((JSONObject) data);
				mMenuList[3] = mMenuList[3] + " (" + String.valueOf(jsonData.getInt("data")) + ")";
			}
			adapter.notifyDataSetChanged();
		} catch (JSONException e) {
			Log.e("Error!", e.toString());
		}
	}

	@Override
	public void onRefreshStarted(View view) {
		getAccount();
	}

	private void getAccount() {
		refreshedCount = 0;
		mPullToRefreshLayout.setRefreshing(true);
		mMenuList = getResources().getStringArray(R.array.accountMenu);
		adapter = new ArrayAdapter<String>(getActivity(),
				R.layout.drawer_list_item, mMenuList);
		mDrawerList.setAdapter(adapter);
		Fetcher fetcher = new Fetcher(this, "3/account/" + username, ApiCall.GET, null, ((ImgurHoloActivity) getActivity()).getApiCall(), ACCOUNTDATA);
		fetcher.execute();
		fetcher = new Fetcher(this, "3/account/" + username + "/likes", ApiCall.GET, null, ((ImgurHoloActivity) getActivity()).getApiCall(), LIKEDATA);
		fetcher.execute();
		fetcher = new Fetcher(this, "3/account/" + username + "/images/count", ApiCall.GET, null, ((ImgurHoloActivity) getActivity()).getApiCall(), COUNTDATA);
		fetcher.execute();
		fetcher = new Fetcher(this, "3/account/" + username + "/comments/count", ApiCall.GET, null, ((ImgurHoloActivity) getActivity()).getApiCall(), COMMENTDATA);
		fetcher.execute();
		fetcher = new Fetcher(this, "/3/account/" + username + "/albums/ids", ApiCall.GET, null, ((ImgurHoloActivity) getActivity()).getApiCall(), ALBUMDATA);
		fetcher.execute();
	}

	private void selectItem(int position) {
		Intent intent;
		switch (position) {
			case 0:
				intent = new Intent();
				intent.putExtra("username", "me");
				intent.setAction(ImgurHoloActivity.ALBUMS_INTENT);
				intent.addCategory(Intent.CATEGORY_DEFAULT);
				startActivity(intent);
				break;
			case 1:
				intent = new Intent();
				intent.putExtra("imageCall", "3/account/" + username + "/images");
				intent.setAction(ImgurHoloActivity.IMAGES_INTENT);
				intent.addCategory(Intent.CATEGORY_DEFAULT);
				startActivity(intent);
				break;
			case 2:
				intent = new Intent();
				intent.putExtra("imageCall", "3/account/" + username + "/likes");
				intent.setAction(ImgurHoloActivity.IMAGES_INTENT);
				intent.addCategory(Intent.CATEGORY_DEFAULT);
				startActivity(intent);
				break;
			case 3:
				intent = new Intent();
				intent.putExtra("username", username);
				intent.setAction(ImgurHoloActivity.COMMENTS_INTENT);
				intent.addCategory(Intent.CATEGORY_DEFAULT);
				startActivity(intent);
				break;
			default:
				break;
		}
	}


	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);
		}
	}

	public void handleException(Exception e, String tag) {
		Log.e("Error!", e.toString());
	}
}
