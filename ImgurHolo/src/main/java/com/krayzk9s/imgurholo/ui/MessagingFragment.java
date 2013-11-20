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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Kurt Zimmer on 7/24/13.
 */
public class MessagingFragment extends Fragment {
    MessageAdapter messageAdapter;
    ListView mDrawerList;
    ArrayList<JSONParcelable> messageDataArray;
    TextView errorText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        activity.setTitle("My Messages");
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        if(activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
            inflater.inflate(R.menu.main, menu);
        else
            inflater.inflate(R.menu.main_dark, menu);
        menu.findItem(R.id.action_message).setVisible(true);
        menu.findItem(R.id.action_refresh).setVisible(true);
        menu.findItem(R.id.action_upload).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.action_refresh:
                getMessages();
                return true;
            case R.id.action_message:
                buildSendMessage(null, null);
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
        Log.d("Theme", settings.getString("theme", MainActivity.HOLO_LIGHT) + "");
        if(settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
            messageAdapter = new MessageAdapter(activity, R.layout.message_layout);
        else
            messageAdapter = new MessageAdapter(activity, R.layout.message_layout_dark);
        String[] mMenuList = getResources().getStringArray(R.array.emptyList);
        ArrayAdapter<String> tempAdapter = null;
        if(settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
            tempAdapter = new ArrayAdapter<String>(activity,
                    R.layout.message_layout, mMenuList);
        else
            tempAdapter = new ArrayAdapter<String>(activity,
                    R.layout.message_layout_dark, mMenuList);

        mDrawerList.setAdapter(tempAdapter);
        if (savedInstanceState == null) {
            getMessages();
        } else {
            messageDataArray = savedInstanceState.getParcelableArrayList("content");
            messageAdapter.addAll(messageDataArray);
            mDrawerList.setAdapter(messageAdapter);
            messageAdapter.notifyDataSetChanged();
        }
        return view;
    }

    private void getMessages() {
        errorText.setVisibility(View.GONE);
        messageAdapter.clear();
        messageAdapter.notifyDataSetChanged();
        MessageAsync messagingAsync = new MessageAsync((MainActivity) getActivity(), this);
        messagingAsync.execute();
    }

    private void addMessages(JSONObject messages) {
        try {
            messageDataArray = new ArrayList<JSONParcelable>();
            JSONArray data = messages.getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject message = data.getJSONObject(i);
                JSONParcelable dataParcel = new JSONParcelable();
                dataParcel.setJSONObject(message);
                messageDataArray.add(dataParcel);
            }
            messageAdapter.addAll(messageDataArray);
        } catch (JSONException e) {
            Log.e("Error!", "adding messages" + e.toString());
            errorText.setVisibility(View.VISIBLE);
        }
        mDrawerList.setAdapter(messageAdapter);
        messageAdapter.notifyDataSetChanged();
    }

    private void buildSendMessage(String username, String title) {
        MainActivity activity = (MainActivity) getActivity();
        final EditText newHeader = new EditText(activity);
        newHeader.setSingleLine();
        final EditText newUsername = new EditText(activity);
        newUsername.setSingleLine();
        newUsername.setHint("Recipient");
        if (username != null)
            newUsername.setText(username);
        if (title != null)
            newHeader.setText("RE: " + title);
        newHeader.setHint("Subject");
        final EditText newBody = new EditText(activity);
        newBody.setHint("Body");
        newBody.setLines(5);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(newUsername);
        linearLayout.addView(newHeader);
        linearLayout.addView(newBody);
        new AlertDialog.Builder(activity).setTitle("Send Message")
                .setView(linearLayout).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                    Log.d("Header", newHeader.getText().toString());
                    MessagingAsync messagingAsync = new MessagingAsync(newHeader.getText().toString(), newBody.getText().toString(), newUsername.getText().toString());
                    messagingAsync.execute();

            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    private static class ViewHolder {
        public TextView header;
        public TextView body;
        public TextView title;
        public ImageButton reply;
        public ImageButton delete;
        public ImageButton report;
        public String from;
        public String id;
    }

    public class MessageAdapter extends ArrayAdapter<JSONParcelable> {
        JSONObject messageData;
        JSONObject messageContent;
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
                if(settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
                    convertView = mInflater.inflate(R.layout.message_layout, null);
                else
                    convertView = mInflater.inflate(R.layout.message_layout_dark, null);
                holder = new ViewHolder();
                holder.body = (TextView) convertView.findViewById(R.id.body);
                holder.title = (TextView) convertView.findViewById(R.id.title);
                holder.header = (TextView) convertView.findViewById(R.id.header);
                holder.reply = (ImageButton) convertView.findViewById(R.id.reply);
                holder.delete = (ImageButton) convertView.findViewById(R.id.delete);
                holder.report = (ImageButton) convertView.findViewById(R.id.report);
                holder.id = "";
                holder.from = "";
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            messageData = this.getItem(position).getJSONObject();
            try {
                final int messagePosition = position;
                messageContent = messageData.getJSONObject("content");
                holder.body.setText(messageContent.getString("body"));
                holder.title.setText(messageContent.getString("subject"));
                holder.header.setText(messageContent.getString("from") + ", " + messageContent.getString("timestamp"));
                holder.from = messageContent.getString("from");
                holder.id = messageContent.getString("id");
                holder.reply.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinearLayout layout = (LinearLayout) v.getParent().getParent();
                        ViewHolder dataHolder = (ViewHolder) layout.getTag();
                            buildSendMessage(dataHolder.from, dataHolder.title.getText().toString());
                    }
                }
                );
                holder.report.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinearLayout layout = (LinearLayout) v.getParent().getParent();
                        final ViewHolder dataHolder = (ViewHolder) layout.getTag();
                        final MainActivity activity = (MainActivity) getActivity();
                        new AlertDialog.Builder(activity).setTitle("Report and Block User").setMessage("Are you sure you want to report this user and block them?")
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                            ReportAsync reportAsync = new ReportAsync(dataHolder.from, activity);
                                            reportAsync.execute();
                                    }
                                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();

                    }
                }
                );
                holder.delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinearLayout layout = (LinearLayout) v.getParent().getParent();
                        final ViewHolder dataHolder = (ViewHolder) layout.getTag();
                            MainActivity activity = (MainActivity) getActivity();
                            new AlertDialog.Builder(activity).setTitle("Send Message").setMessage("Are you sure you want to delete this message?")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            messageAdapter.remove(messageAdapter.getItem(messagePosition));
                                            messageAdapter.notifyDataSetChanged();
                                            DeleteAsync deleteAsync = new DeleteAsync(dataHolder.id);
                                            deleteAsync.execute();
                                        }
                                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Do nothing.
                                }
                            }).show();
                    }
                });
                convertView.setTag(holder);
            } catch (JSONException e) {
                Log.e("Error!", "error in getting view" + e.toString());
            }
            return convertView;
        }
    }

    private class MessagingAsync extends AsyncTask<Void, Void, Void> {
        private String header;
        private String body;
        private String username;

        public MessagingAsync(String _header, String _body, String _username) {
            header = _header;
            body = _body;
            username = _username;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = (MainActivity) getActivity();
            HashMap<String, Object> messageMap = new HashMap<String, Object>();
            messageMap.put("subject", header);
            messageMap.put("body", body);
            messageMap.put("recipient", username);
            ((ImgurHoloActivity)getActivity()).getApiCall().makeCall("/3/message", "post", messageMap);
            return null;
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
            ((ImgurHoloActivity)getActivity()).getApiCall().makeCall("3/message/" + id, "delete", null);
            return null;
        }
    }

    private static class ReportAsync extends AsyncTask<Void, Void, Void> {
        private String username;
        MainActivity activity;
        public ReportAsync(String _username, MainActivity _activity) {
            username = _username;
            activity = _activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            activity.getApiCall().makeCall("3/message/report/" + username, "post", null);
            activity.getApiCall().makeCall("3/message/block/" + username, "post", null);
            return null;
        }
    }

    private static class MessageAsync extends AsyncTask<Void, Void, JSONObject> {
        MainActivity activity;
        MessagingFragment messagingFragment;
        public MessageAsync(MainActivity _activity, MessagingFragment _messagingFragment) {
            activity = _activity;
            messagingFragment = _messagingFragment;
        }
        @Override
        protected JSONObject doInBackground(Void... voids) {
            JSONObject messages = activity.getApiCall().makeCall("/3/account/me/notifications/messages?new=false", "get", null);
            return messages;
        }

        @Override
        protected void onPostExecute(JSONObject messages) {
            if(messagingFragment.messageAdapter != null)
                messagingFragment.addMessages(messages);
        }
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelableArrayList("content", messageDataArray);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
}
