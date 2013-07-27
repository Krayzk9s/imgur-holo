package com.krayzk9s.imgurholo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Kurt Zimmer on 7/24/13.
 */
public class MessagingFragment extends Fragment {
    MessageAdapter messageAdapter;
    ListView mDrawerList;
    ArrayList<JSONParcelable> messageDataArray;

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
        inflater.inflate(R.menu.main, menu);
        menu.findItem(R.id.action_message).setVisible(true);
        menu.findItem(R.id.action_upload).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
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
        mDrawerList = (ListView) view.findViewById(R.id.account_list);
        MainActivity activity = (MainActivity) getActivity();
        messageAdapter = new MessageAdapter(activity, R.layout.message_layout);
        String[] mMenuList = getResources().getStringArray(R.array.emptyList);
        ArrayAdapter<String> tempAdapter = new ArrayAdapter<String>(activity,
                R.layout.message_layout, mMenuList);
        mDrawerList.setAdapter(tempAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        if(savedInstanceState == null)
        {
            AsyncTask<Void, Void, JSONObject> async = new AsyncTask<Void, Void, JSONObject>() {
                @Override
                protected JSONObject doInBackground(Void... voids) {
                    MainActivity activity = (MainActivity) getActivity();
                    JSONObject messages = activity.makeGetCall("/3/account/me/notifications/messages?new=false");
                    return messages;
                }

                @Override
                protected void onPostExecute(JSONObject messages) {
                    addMessages(messages);
                }
            };
            async.execute();
        }
        else
        {
            messageDataArray = savedInstanceState.getParcelableArrayList("content");
            messageAdapter.addAll(messageDataArray);
            mDrawerList.setAdapter(messageAdapter);
            messageAdapter.notifyDataSetChanged();
        }
        return view;
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
        }
        mDrawerList.setAdapter(messageAdapter);
        messageAdapter.notifyDataSetChanged();
    }

    private void selectItem(int position) {
    }

    private void buildSendMessage(String username, String title) {
        MainActivity activity = (MainActivity) getActivity();

        final EditText newHeader = new EditText(activity);
        newHeader.setSingleLine();
        final EditText newUsername = new EditText(activity);
        newUsername.setSingleLine();
        if (username != null)
            newUsername.setText(username);
        if (title != null)
            newHeader.setText("RE: " + title);
        newHeader.setHint("Subject");
        final EditText newBody = new EditText(activity);
        newBody.setHint("Body");
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

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d("clicked!", "clicked!");
            selectItem(position);
        }
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
                convertView = mInflater.inflate(R.layout.message_layout, null);
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
                        new AlertDialog.Builder(activity).setTitle("Send Message").setMessage("Are you sure you want to report this user and block them?")
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
            activity.makeMessagePost(header, body, username);
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
            activity.deletePost(id);
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
            activity.reportPost(username);
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
