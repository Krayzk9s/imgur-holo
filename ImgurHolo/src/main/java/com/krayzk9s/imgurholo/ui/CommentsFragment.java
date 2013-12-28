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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.tools.ApiCall;
import com.krayzk9s.imgurholo.tools.Fetcher;
import com.krayzk9s.imgurholo.tools.GetData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

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
public class CommentsFragment extends Fragment implements GetData {
	private MessageAdapter commentsAdapter;
	private ListView mDrawerList;
	private ArrayList<JSONParcelable> commentDataArray;
	private String username;
	private TextView errorText;
	private final CommentsFragment commentsFragment = this;
	private final static String DELETE = "delete";
	private final static String COMMENTS = "comments";
	private final static String IMAGE = "image";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = getArguments();
		username = bundle.getString("username");
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
		if (!username.equals("me"))
			activity.setTitle(String.format(
                    activity.getResources().getString(R.string.activity_title_comments_user),
                    username));
		else
			activity.setTitle(R.string.activity_title_my_comments);
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
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// handle item selection
		switch (item.getItemId()) {
			//none right now
			case R.id.action_refresh:
				getComments();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.account_layout, container, false);
		LinearLayout headerLayout = (LinearLayout) view.findViewById(R.id.header);
		headerLayout.setVisibility(View.GONE);
		errorText = (TextView) view.findViewById(R.id.error);
		mDrawerList = (ListView) view.findViewById(R.id.account_list);
		ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
		SharedPreferences settings = activity.getApiCall().settings;
		if (settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
			commentsAdapter = new MessageAdapter(activity, R.layout.comment_layout);
		else
			commentsAdapter = new MessageAdapter(activity, R.layout.comment_layout_dark);
		String[] mMenuList = getResources().getStringArray(R.array.emptyList);
		ArrayAdapter<String> tempAdapter;
		if (settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
			tempAdapter = new ArrayAdapter<String>(activity,
					R.layout.comment_layout, mMenuList);
		else
			tempAdapter = new ArrayAdapter<String>(activity,
					R.layout.comment_layout_dark, mMenuList);
		mDrawerList.setAdapter(tempAdapter);
		if (savedInstanceState == null) {
			getComments();
		} else {
			commentDataArray = savedInstanceState.getParcelableArrayList("content");
			commentsAdapter.addAll(commentDataArray);
			mDrawerList.setAdapter(commentsAdapter);
			commentsAdapter.notifyDataSetChanged();
		}
		return view;
	}

	public void onGetObject(Object object, String tag) {
		if (tag.equals(DELETE))
			getComments();
		else if (tag.equals(IMAGE)) { /*
			SingleImageFragment singleImageFragment = new SingleImageFragment();
            Bundle bundle = new Bundle();
            bundle.putBoolean("gallery", true);
            JSONParcelable data = new JSONParcelable();
            try {
            data.setJSONObject(((JSONObject) object).getJSONObject("data"));
            bundle.putParcelable("imageData", data);
            singleImageFragment.setArguments(bundle);
            ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
            activity.changeFragment(singleImageFragment, true);
            }
            catch(JSONException e) {
                Log.e("Error!", e.toString());
            }*/
		} else if (tag.equals(COMMENTS)) {
			JSONObject comments = (JSONObject) object;
			if (commentsAdapter != null)
				addComments(comments);
		}
	}

	public void handleException(Exception e, String tag) {
		Log.e("Error!", e.toString());
	}

	private void getComments() {
		commentsAdapter.clear();
		commentsAdapter.notifyDataSetChanged();
		errorText.setVisibility(View.GONE);
		Fetcher fetcher = new Fetcher(this, "/3/account/" + username + "/comments", ApiCall.GET, null, ((ImgurHoloActivity) getActivity()).getApiCall(), COMMENTS);
		fetcher.execute();
	}

	private void addComments(JSONObject comments) {
		try {
			commentDataArray = new ArrayList<JSONParcelable>();
			JSONArray data = comments.getJSONArray("data");
			for (int i = 0; i < data.length(); i++) {
				JSONObject message = data.getJSONObject(i);
				JSONParcelable dataParcel = new JSONParcelable();
				dataParcel.setJSONObject(message);
				commentDataArray.add(dataParcel);
			}
			commentsAdapter.addAll(commentDataArray);
		} catch (JSONException e) {
			errorText.setVisibility(View.VISIBLE);
			errorText.setText("Error getting comments");
			Log.e("Error!", "adding messages" + e.toString());
		}
		mDrawerList.setAdapter(commentsAdapter);
		commentsAdapter.notifyDataSetChanged();
	}

	private static class ViewHolder {
		public TextView header;
		public TextView body;
		public ImageButton delete;
		public ImageButton link;
		public String id;
		public String image_id;
    }

	public class MessageAdapter extends ArrayAdapter<JSONParcelable> {
		JSONObject commentContent;
		private final LayoutInflater mInflater;

		public MessageAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
				SharedPreferences settings = activity.getApiCall().settings;
				if (settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
					convertView = mInflater.inflate(R.layout.comment_layout, null);
				else
					convertView = mInflater.inflate(R.layout.comment_layout_dark, null);
				holder = new ViewHolder();
				holder.body = (TextView) convertView.findViewById(R.id.body);
				holder.header = (TextView) convertView.findViewById(R.id.header);
				holder.delete = (ImageButton) convertView.findViewById(R.id.delete);
				holder.link = (ImageButton) convertView.findViewById(R.id.link);
				holder.id = "";
				holder.image_id = "";
                convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			try {
				commentContent = this.getItem(position).getJSONObject();
				final int commentPosition = position;
				Calendar accountCreationDate = Calendar.getInstance();
				accountCreationDate.setTimeInMillis((long) commentContent.getInt("datetime") * 1000);
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
				String accountCreated = sdf.format(accountCreationDate.getTime());
				holder.body.setText(commentContent.getString("comment"));
				holder.header.setText(accountCreated + " - " + commentContent.getString("points") + "pts (" + commentContent.getString("ups") + "/" + commentContent.getString("downs") + ")");
				holder.id = commentContent.getString("id");
				holder.image_id = commentContent.getString("image_id");

				if (!username.equals("me"))
					holder.delete.setVisibility(View.GONE);
				holder.delete.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (view == null || view.getParent() == null || view.getParent().getParent() == null)
							return;
						LinearLayout layout = (LinearLayout) view.getParent().getParent();
						final ViewHolder dataHolder = (ViewHolder) layout.getTag();
						ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
						new AlertDialog.Builder(activity).setTitle("Delete Comment").setMessage("Are you sure you want to delete this comment?")
								.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										commentsAdapter.remove(commentsAdapter.getItem(commentPosition));
										commentsAdapter.notifyDataSetChanged();
										Fetcher fetcher = new Fetcher(commentsFragment, "/3/comment/" + dataHolder.id, ApiCall.DELETE, null, ((ImgurHoloActivity) getActivity()).getApiCall(), DELETE);
										fetcher.execute();
									}
								}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Do nothing.
							}
						}).show();
					}
				});
				holder.link.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (view == null || view.getParent() == null || view.getParent().getParent() == null)
							return;
						View convertView = (View) view.getParent().getParent();
						final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
						Fetcher fetcher = new Fetcher(commentsFragment, "/3/gallery/image/" + viewHolder.image_id, ApiCall.GET, null, ((ImgurHoloActivity) getActivity()).getApiCall(), IMAGE);
						fetcher.execute();
					}
				});

				convertView.setTag(holder);
			} catch (JSONException e) {
				Log.e("Error!", e.toString());
			}
			return convertView;
		}
	}
}
