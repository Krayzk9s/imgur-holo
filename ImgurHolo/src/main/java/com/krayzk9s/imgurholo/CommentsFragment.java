package com.krayzk9s.imgurholo;

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
public class CommentsFragment extends Fragment {
    MessageAdapter commentsAdapter;
    ListView mDrawerList;
    ArrayList<JSONParcelable> commentDataArray;
    String username;

    public CommentsFragment(String _username) {
        username = _username;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        MainActivity activity = (MainActivity)getActivity();
        if(activity.theme == activity.HOLO_LIGHT)
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
        mDrawerList = (ListView) view.findViewById(R.id.account_list);
        MainActivity activity = (MainActivity) getActivity();
        SharedPreferences settings = activity.getSettings();
        if(settings.getInt("theme", activity.HOLO_LIGHT) == activity.HOLO_LIGHT)
            commentsAdapter = new MessageAdapter(activity, R.layout.comment_layout);
        else
            commentsAdapter = new MessageAdapter(activity, R.layout.comment_layout_dark);
        String[] mMenuList = getResources().getStringArray(R.array.emptyList);
        ArrayAdapter<String> tempAdapter = null;
        if(settings.getInt("theme", activity.HOLO_LIGHT) == activity.HOLO_LIGHT)
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

    private void getComments() {
        commentsAdapter.clear();
        commentsAdapter.notifyDataSetChanged();
        AsyncTask<Void, Void, JSONObject> async = new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                JSONObject comments = activity.makeCall("/3/account/" + username + "/comments", "get", null);
                return comments;
            }
            @Override
            protected void onPostExecute(JSONObject comments) {
                if(commentsAdapter != null)
                    addComments(comments);
            }
        };
        async.execute();
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
                if(settings.getInt("theme", activity.HOLO_LIGHT) == activity.HOLO_LIGHT)
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
                                            DeleteAsync deleteAsync = new DeleteAsync(dataHolder.id);
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
                                    singleImageFragment.setGallery(true);
                                    singleImageFragment.setParams(imageData);
                                    activity.changeFragment(singleImageFragment);
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

    private class DeleteAsync extends AsyncTask<Void, Void, Void> {
        private String id;

        public DeleteAsync(String _id) {
            id = _id;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = (MainActivity) getActivity();
            activity.makeCall("/3/comment/" + id, "delete", null);
            return null;
        }
    }
}
