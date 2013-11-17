package com.krayzk9s.imgurholo;

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
import android.os.AsyncTask;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Kurt Zimmer on 7/24/13.
 */
public class CommentsFragment extends Fragment implements GetData {
    MessageAdapter commentsAdapter;
    ListView mDrawerList;
    ArrayList<JSONParcelable> commentDataArray;
    String username;
    TextView errorText;

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
        MainActivity activity = (MainActivity) getActivity();
        if(username != "me")
            activity.setTitle(username + "'s Comments");
        else
            activity.setTitle("My Comments");
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        MainActivity activity = (MainActivity)getActivity();
        if(activity.theme.equals(activity.HOLO_LIGHT))
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
        MainActivity activity = (MainActivity) getActivity();
        SharedPreferences settings = activity.getSettings();
        if(settings.getString("theme", activity.HOLO_LIGHT).equals(activity.HOLO_LIGHT))
            commentsAdapter = new MessageAdapter(activity, R.layout.comment_layout);
        else
            commentsAdapter = new MessageAdapter(activity, R.layout.comment_layout_dark);
        String[] mMenuList = getResources().getStringArray(R.array.emptyList);
        ArrayAdapter<String> tempAdapter = null;
        if(settings.getString("theme", activity.HOLO_LIGHT).equals(activity.HOLO_LIGHT))
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

    public void onGetObject(Object object) {
        JSONObject comments = (JSONObject) object;
        if(commentsAdapter != null)
            addComments(comments);
    }

    private void getComments() {
        commentsAdapter.clear();
        commentsAdapter.notifyDataSetChanged();
        errorText.setVisibility(View.GONE);
        Fetcher fetcher = new Fetcher(this, "/3/account/" + username + "/comments", (MainActivity) getActivity());
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
        } catch (Exception e) {
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
        public int position;
        public ImageView image;
    }

    public class MessageAdapter extends ArrayAdapter<JSONParcelable> {
        JSONObject commentContent;
        private LayoutInflater mInflater;

        public MessageAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                MainActivity activity = (MainActivity) getActivity();
                SharedPreferences settings = activity.getSettings();
                if(settings.getString("theme", activity.HOLO_LIGHT).equals(activity.HOLO_LIGHT))
                    convertView = mInflater.inflate(R.layout.comment_layout, null);
                else
                    convertView = mInflater.inflate(R.layout.comment_layout_dark, null);
                holder = new ViewHolder();
                holder.body = (TextView) convertView.findViewById(R.id.body);
                holder.header = (TextView) convertView.findViewById(R.id.header);
                holder.delete = (ImageButton) convertView.findViewById(R.id.delete);
                holder.link = (ImageButton) convertView.findViewById(R.id.link);
                holder.image = (ImageView) convertView.findViewById(R.id.comment_image);
                holder.id = "";
                holder.image_id = "";
                holder.position = position;
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
                holder.header.setText(accountCreated + " - " + commentContent.getString("points") + "pts (" + commentContent.getString("ups") + "/" + commentContent.getString("downs") +  ")");
                holder.id = commentContent.getString("id");
                holder.image_id = commentContent.getString("image_id");
                UrlImageViewHelper.setUrlDrawable(holder.image, "http://imgur.com/" + commentContent.getString("image_id") + "t.png", R.drawable.icon);
                if (!username.equals("me"))
                    holder.delete.setVisibility(View.GONE);
                holder.delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinearLayout layout = (LinearLayout) v.getParent().getParent();
                        final ViewHolder dataHolder = (ViewHolder) layout.getTag();
                        MainActivity activity = (MainActivity) getActivity();
                        try {
                            new AlertDialog.Builder(activity).setTitle("Delete Comment").setMessage("Are you sure you want to delete this comment?")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            commentsAdapter.remove(commentsAdapter.getItem(commentPosition));
                                            commentsAdapter.notifyDataSetChanged();
                                            DeleteAsync deleteAsync = new DeleteAsync(dataHolder.id, (MainActivity) getActivity());
                                            deleteAsync.execute();
                                        }
                                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Do nothing.
                                }
                            }).show();
                        } catch (Exception e) {
                            Log.e("Error!", "missing data");
                        }
                    }
                });
                holder.link.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            View convertView = (View) view.getParent().getParent();
                            final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
                            AsyncTask<Void, Void, JSONObject> async = new AsyncTask<Void, Void, JSONObject>() {
                                @Override
                                protected JSONObject doInBackground(Void... voids) {
                                    try {
                                    MainActivity activity = (MainActivity) getActivity();
                                    JSONObject imageData = activity.makeCall("/3/gallery/image/" + viewHolder.image_id, "get", null);
                                    return imageData.getJSONObject("data");
                                    } catch (Exception e) {
                                        Log.e("Error!", "missing data");
                                    }
                                    return null;
                                }
                                protected void onPostExecute(JSONObject imageData) {
                                    MainActivity activity = (MainActivity) getActivity();
                                    SingleImageFragment singleImageFragment = new SingleImageFragment();
                                    Bundle bundle = new Bundle();
                                    bundle.putBoolean("gallery", true);
                                    JSONParcelable data = new JSONParcelable();
                                    data.setJSONObject(imageData);
                                    bundle.putParcelable("imageData", data);
                                    singleImageFragment.setArguments(bundle);
                                    activity.changeFragment(singleImageFragment, true);
                                }
                            };
                            async.execute();
                        }
                        catch (Exception e) {
                            Log.e("Error!", e.toString());
                        }
                    }
                });

                convertView.setTag(holder);
            }
            catch (Exception e) {
                Log.e("Error!", e.toString());
            }
            return convertView;
        }
    }

    private static class DeleteAsync extends AsyncTask<Void, Void, Void> {
        private String id;
        MainActivity activity;

        public DeleteAsync(String _id, MainActivity _activity) {
            id = _id;
            activity = _activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            activity.makeCall("/3/comment/" + id, "delete", null);
            return null;
        }
    }
}
