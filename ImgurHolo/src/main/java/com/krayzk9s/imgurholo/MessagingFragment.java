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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
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

    public MessagingFragment() {

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
        if(activity.theme.equals(activity.HOLO_LIGHT))
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
        Log.d("Theme", settings.getString("theme", activity.HOLO_LIGHT) + "");
        if(settings.getString("theme", activity.HOLO_LIGHT).equals(activity.HOLO_LIGHT))
            messageAdapter = new MessageAdapter(activity, R.layout.message_layout);
        else
            messageAdapter = new MessageAdapter(activity, R.layout.message_layout_dark);
        String[] mMenuList = getResources().getStringArray(R.array.emptyList);
        ArrayAdapter<String> tempAdapter = null;
        if(settings.getString("theme", activity.HOLO_LIGHT).equals(activity.HOLO_LIGHT))
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
        AsyncTask<Void, Void, JSONObject> async = new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                JSONObject messages = activity.makeCall("/3/account/me/notifications/messages?new=false", "get", null);
                return messages;
            }

            @Override
            protected void onPostExecute(JSONObject messages) {
                if(messageAdapter != null)
                    addMessages(messages);
            }
        };
        async.execute();
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
        } catch (Exception e) {
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
                try {
                    Log.d("Header", newHeader.getText().toString());
                    MessagingAsync messagingAsync = new MessagingAsync(newHeader.getText().toString(), newBody.getText().toString(), newUsername.getText().toString());
                    messagingAsync.execute();
                } catch (Exception e) {
                    Log.e("Error!", "oops, some text fields missing values");
                }

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
                if(settings.getString("theme", activity.HOLO_LIGHT).equals(activity.HOLO_LIGHT))
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
                        try {
                            buildSendMessage(dataHolder.from, dataHolder.title.getText().toString());
                        } catch (Exception e) {
                            Log.e("Error!", "missing data");
                        }
                    }
                }
                );
                holder.report.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinearLayout layout = (LinearLayout) v.getParent().getParent();
                        final ViewHolder dataHolder = (ViewHolder) layout.getTag();
                        MainActivity activity = (MainActivity) getActivity();
                        new AlertDialog.Builder(activity).setTitle("Report and Block User").setMessage("Are you sure you want to report this user and block them?")
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        try {
                                            ReportAsync reportAsync = new ReportAsync(dataHolder.from);
                                            reportAsync.execute();
                                        } catch (Exception e) {
                                            Log.e("Error!", "missing data" + dataHolder.toString());
                                        }
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
                        try {
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
                        } catch (Exception e) {
                            Log.e("Error!", "missing data");
                        }
                    }
                });
                convertView.setTag(holder);
            } catch (Exception e) {
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
            activity.makeCall("/3/message", "post", messageMap);
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
            activity.makeCall("3/message/" + id, "delete", null);
            return null;
        }
    }

    private class ReportAsync extends AsyncTask<Void, Void, Void> {
        private String username;

        public ReportAsync(String _username) {
            username = _username;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = (MainActivity) getActivity();
            activity.makeCall("3/message/report/" + username, "post", null);
            activity.makeCall("3/message/block/" + username, "post", null);
            return null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelableArrayList("content", messageDataArray);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
}
