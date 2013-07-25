package com.krayzk9s.imgurholo;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Kurt Zimmer on 7/24/13.
 */
public class MessagingFragment extends Fragment{
    MessageAdapter messageAdapter;
    ListView mDrawerList;
    public MessagingFragment() {

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
        return view;
    }

    private void addMessages(JSONObject messages)
    {
        try {
            JSONArray data = messages.getJSONArray("data");
            for(int i = 0; i < data.length(); i++) {
                JSONObject message = data.getJSONObject(i);
                messageAdapter.add(message);
            }
        }
        catch (Exception e)
        {
            Log.e("Error!", e.toString());
        }
        mDrawerList.setAdapter(messageAdapter);
        messageAdapter.notifyDataSetChanged();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
    }

    public class MessageAdapter extends ArrayAdapter<JSONObject>
    {
        private LayoutInflater mInflater;
        public MessageAdapter(Context context, int textViewResourceId)
        {
            super(context, textViewResourceId);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder;
            if(convertView == null)
            {
                convertView = mInflater.inflate(R.layout.message_layout, null);
                holder = new ViewHolder();
                holder.body = (TextView)convertView.findViewById(R.id.body);
                holder.title = (TextView)convertView.findViewById(R.id.title);
                holder.header = (TextView)convertView.findViewById(R.id.header);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            JSONObject messageData = this.getItem(position);
            try {
                JSONObject messageContent = messageData.getJSONObject("content");
                holder.body.setText(messageContent.getString("body"));
                holder.title.setText(messageContent.getString("subject"));
                holder.header.setText(messageContent.getString("from") + ", " + messageContent.getString("timestamp"));
                convertView.setTag(holder);
            }
            catch (Exception e) {
                Log.e("Error!", e.toString());
            }
            return convertView;
        }
    }
    private static class ViewHolder {
        public TextView header;
        public TextView body;
        public TextView title;
    }
}
