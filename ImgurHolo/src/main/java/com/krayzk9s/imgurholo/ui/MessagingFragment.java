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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.krayzk9s.imgurholo.tools.ApiCall;
import com.krayzk9s.imgurholo.tools.Fetcher;
import com.krayzk9s.imgurholo.tools.GetData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

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
public class MessagingFragment extends Fragment implements GetData {
    private MessageAdapter messageAdapter;
    private ListView mDrawerList;
    private TextView errorText;
    private JSONParcelable messageData;
    private static String MESSAGES = "messages";
    private static String CONVERSATION = "conversation";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey("data"))
            messageData = bundle.getParcelable("data");
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
        menu.findItem(R.id.action_refresh).setVisible(true);
        menu.findItem(R.id.action_upload).setVisible(false);
        if (messageData != null) {
            menu.findItem(R.id.action_report).setVisible(true);
            menu.findItem(R.id.action_reply).setVisible(true);
            menu.findItem(R.id.action_delete).setVisible(true);
        } else {
            menu.findItem(R.id.action_message).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        final ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        switch (item.getItemId()) {
            case R.id.action_refresh:
                getMessages();
                return true;
            case R.id.action_message:
                buildSendMessage(null);
                return true;
            case R.id.action_reply:
                try {
                    Log.d("messageData", messageData.getJSONObject().toString());
                    buildSendMessage(messageData.getJSONObject().getString("with_account"));
                } catch (JSONException e) {
                    Log.e("Error!", e.toString());
                }
                return true;
            case R.id.action_report:
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.dialog_report_user_title)
                        .setMessage(R.string.dialog_report_user_summary)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    ReportAsync reportAsync = new ReportAsync(messageData.getJSONObject().getString("with_account"), activity);
                                    reportAsync.execute();
                                } catch (JSONException e) {
                                    Log.e("Error", e.toString());
                                }
                            }
                        }).setNegativeButton(R.string.dialog_answer_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                return true;
            case R.id.action_delete:
                new AlertDialog.Builder(activity).setTitle(R.string.dialog_send_message_title).setMessage(R.string.dialog_delete_message_summary)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    DeleteAsync deleteAsync = new DeleteAsync(messageData.getJSONObject().getString("id"));
                                    deleteAsync.execute();
                                } catch (JSONException e) {
                                    Log.e("Error!", e.toString());
                                }
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
        View view = inflater.inflate(R.layout.account_layout, container, false);
        LinearLayout headerLayout = (LinearLayout) view.findViewById(R.id.header);
        headerLayout.setVisibility(View.GONE);
        errorText = (TextView) view.findViewById(R.id.error);
        mDrawerList = (ListView) view.findViewById(R.id.account_list);
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        SharedPreferences settings = activity.getApiCall().settings;
        Log.d("Theme", settings.getString("theme", MainActivity.HOLO_LIGHT) + "");
        if (settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
            messageAdapter = new MessageAdapter(activity, R.layout.message_layout);
        else
            messageAdapter = new MessageAdapter(activity, R.layout.message_layout_dark);


        if (savedInstanceState == null) {
            getMessages();
        } else {
            ArrayList<JSONParcelable> messageDataArray = savedInstanceState.getParcelableArrayList("content");
            messageAdapter.addAll(messageDataArray);
            messageAdapter.notifyDataSetChanged();
        }
        mDrawerList.setAdapter(messageAdapter);
        if(messageData != null)
            mDrawerList.setClickable(false);
        else
            mDrawerList.setOnItemClickListener(new ItemClickListener());
        return view;
    }

    private void selectItem(int position) {
        Intent intent = new Intent();
        JSONParcelable jsonParcelable = new JSONParcelable();
        jsonParcelable.setJSONObject(messageAdapter.getItem(position).getJSONObject());
        intent.putExtra("data", jsonParcelable);
        intent.setAction(ImgurHoloActivity.MESSAGE_INTENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        startActivity(intent);
    }

    private class ItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void getMessages() {
        errorText.setVisibility(View.GONE);
        messageAdapter.clear();
        messageAdapter.notifyDataSetChanged();
        Fetcher fetcher;
        if (messageData != null) {
            try {
                fetcher = new Fetcher(this, "3/conversations/" + messageData.getJSONObject().getString("id"), ApiCall.GET, null, ((ImgurHoloActivity) getActivity()).getApiCall(), CONVERSATION);
                fetcher.execute();
            } catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
        } else {
            fetcher = new Fetcher(this, "3/conversations", ApiCall.GET, null, ((ImgurHoloActivity) getActivity()).getApiCall(), MESSAGES);
            fetcher.execute();
        }
    }

    public void onGetObject(Object o, String tag) {
        if (tag.equals(MESSAGES))
            addMessages((JSONObject) o);
        else if(tag.equals(CONVERSATION)) {
            try {
            addMessages(((JSONObject) o).getJSONObject("data"));
            }
            catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
        }
    }

    public void handleException(Exception e, String tag) {

    }

    private void addMessages(JSONObject messages) {
        Log.d("messages", messages.toString());
        JSONArray data;
        try {
            if (messages.has("messages"))
                data = messages.getJSONArray("messages");
            else
                data = messages.getJSONArray("data");

            for (int i = 0; i < data.length(); i++) {
                JSONObject message = data.getJSONObject(i);
                JSONParcelable dataParcel = new JSONParcelable();
                dataParcel.setJSONObject(message);
                messageAdapter.add(dataParcel);
            }
        } catch (JSONException e) {
            Log.e("Error!", "adding messages" + e.toString());
            errorText.setVisibility(View.VISIBLE);
        }
        messageAdapter.notifyDataSetChanged();
    }

    private void buildSendMessage(String username) {
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        final EditText newUsername = new EditText(activity);
        newUsername.setSingleLine();
        newUsername.setHint(R.string.body_hint_recipient);
        if (username != null)
            newUsername.setText(username);
        final EditText newBody = new EditText(activity);
        newBody.setHint(R.string.body_hint_body);
        newBody.setLines(5);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(newUsername);
        linearLayout.addView(newBody);
        new AlertDialog.Builder(activity).setTitle(R.string.dialog_send_message_title)
                .setView(linearLayout).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                MessagingAsync messagingAsync = new MessagingAsync(newBody.getText().toString(), newUsername.getText().toString());
                messagingAsync.execute();

            }
        }).setNegativeButton(R.string.dialog_answer_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        ArrayList<JSONParcelable> messagesData = new ArrayList<JSONParcelable>();
        for (int i = 0; i < messageAdapter.getCount(); i++) {
            messagesData.add(messageAdapter.getItem(i));
        }
        savedInstanceState.putParcelableArrayList("content", messagesData);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private static class ViewHolder {
        public TextView header;
        public TextView body;
        public TextView title;
        public String from;
        public String id;
    }

    private static class ReportAsync extends AsyncTask<Void, Void, Void> {
        final ImgurHoloActivity activity;
        private final String username;

        public ReportAsync(String _username, ImgurHoloActivity _activity) {
            username = _username;
            activity = _activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            activity.getApiCall().makeCall("3/conversations/report/" + username, "post", null);
            activity.getApiCall().makeCall("3/conversations/block/" + username, "post", null);
            return null;
        }
    }

    public class MessageAdapter extends ArrayAdapter<JSONParcelable> {
        private final LayoutInflater mInflater;
        JSONObject messageViewData;

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
                    convertView = mInflater.inflate(R.layout.message_layout, null);
                else
                    convertView = mInflater.inflate(R.layout.message_layout_dark, null);
                holder = new ViewHolder();
                holder.body = (TextView) convertView.findViewById(R.id.body);
                holder.title = (TextView) convertView.findViewById(R.id.title);
                holder.header = (TextView) convertView.findViewById(R.id.header);
                holder.id = "";
                holder.from = "";
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (this.getItem(position) == null)
                return convertView;
            messageViewData = this.getItem(position).getJSONObject();
            try {
                Calendar calendar = Calendar.getInstance();
                long now = calendar.getTimeInMillis();
                if (messageViewData.has("message_count")) {
                    holder.body.setText(messageViewData.getString("last_message_preview"));
                    holder.header.setText(messageViewData.getInt("message_count") + " message(s), " + DateUtils.getRelativeTimeSpanString(messageViewData.getLong("datetime") * 1000, now, DateUtils.MINUTE_IN_MILLIS));
                    holder.title.setText(messageViewData.getString("with_account"));
                    holder.title.setVisibility(View.VISIBLE);
                    holder.from = messageViewData.getString("with_account");
                    holder.id = messageViewData.getString("id");
                } else {
                    holder.body.setText(messageViewData.getString("body"));
                    holder.title.setVisibility(View.GONE);
                    holder.header.setText(messageViewData.getString("from") + ", " + DateUtils.getRelativeTimeSpanString(messageViewData.getLong("datetime") * 1000, now, DateUtils.MINUTE_IN_MILLIS));
                    holder.from = messageViewData.getString("from");
                    holder.id = messageViewData.getString("id");
                }
                convertView.setTag(holder);
            } catch (JSONException e) {
                Log.e("Error!", "error in getting view" + e.toString());
            }
            return convertView;
        }
    }

    private class MessagingAsync extends AsyncTask<Void, Void, Void> {
        private final String body;
        private final String username;

        public MessagingAsync(String _body, String _username) {
            body = _body;
            username = _username;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            HashMap<String, Object> messageMap = new HashMap<String, Object>();
            messageMap.put("body", body);
            messageMap.put("recipient", username);
            ((ImgurHoloActivity) getActivity()).getApiCall().makeCall("/3/conversations/" + username, ApiCall.POST, messageMap);
            return null;
        }
    }

    private class DeleteAsync extends AsyncTask<Void, Void, Void> {
        private final String id;

        public DeleteAsync(String _id) {
            id = _id;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ((ImgurHoloActivity) getActivity()).getApiCall().makeCall("3/conversations/" + id, "delete", null);
            return null;
        }
    }
}
