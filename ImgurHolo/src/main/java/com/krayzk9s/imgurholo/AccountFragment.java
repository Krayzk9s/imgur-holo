package com.krayzk9s.imgurholo;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by Kurt Zimmer on 7/22/13.
 */
public class AccountFragment extends Fragment {

    String[] mMenuList;
    ArrayAdapter<String> adapter;
    private HashMap<String, String> accountData;

    public AccountFragment() {

    }

    @Override
    public void onCreate(Bundle save)
    {
        super.onCreate(save);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.account_layout, container, false);
        ListView mDrawerList = (ListView) view.findViewById(R.id.account_list);
        mMenuList = getResources().getStringArray(R.array.accountMenu);
        adapter = new ArrayAdapter<String>(view.getContext(),
                R.layout.drawer_list_item, mMenuList);
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        accountData = new HashMap<String, String>();
        AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                JSONObject accountInfoJSON = activity.makeGetCall("3/account/me");
                JSONObject statsJSON = activity.makeGetCall("3/account/me/stats");
                JSONObject likesJSON = activity.makeGetCall("3/account/me/likes");

                try {
                    accountInfoJSON = accountInfoJSON.getJSONObject("data");
                    Log.d("URI", accountInfoJSON.toString());
                    Log.d("URI", Integer.toString(accountInfoJSON.getInt("id")));
                    accountData.put("id", Integer.toString(accountInfoJSON.getInt("id")));
                    accountData.put("reputation", Integer.toString(accountInfoJSON.getInt("reputation")));
                    Calendar accountCreationDate = Calendar.getInstance();
                    accountCreationDate.setTimeInMillis((long) accountInfoJSON.getInt("created") * 1000);
                    Log.d("URI", accountInfoJSON.getInt("created") + "");
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                    String accountcreated = sdf.format(accountCreationDate.getTime());
                    Log.d("URI", accountcreated);
                    accountData.put("created", accountcreated);
                    accountData.put("bio", accountInfoJSON.getString("bio"));
                    accountData.put("name", accountInfoJSON.getString("url"));

                    statsJSON = statsJSON.getJSONObject("data");
                    accountData.put("total_images", Integer.toString(statsJSON.getInt("total_images")));
                    accountData.put("total_albums", Integer.toString(statsJSON.getInt("total_albums")));
                    accountData.put("disk_used", statsJSON.getString("disk_used"));
                    accountData.put("bandwidth_used", statsJSON.getString("bandwidth_used"));

                    JSONArray likesJSONArray = likesJSON.getJSONArray("data");
                    accountData.put("total_likes", String.valueOf(likesJSONArray.length()));

                } catch (Exception e) {
                    Log.e("Error!", e.toString());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mMenuList[0] = mMenuList[0] + " (" + accountData.get("total_albums") + ")";
                mMenuList[1] = mMenuList[1] + " (" + accountData.get("total_images") + ")";
                mMenuList[2] = mMenuList[2] + " (" + accountData.get("total_likes") + ")";
                //mMenuList[3] = mMenuList[3] + " (" + accountData.get("total_comments") + ")";
                //mMenuList[4] = mMenuList[4] + " (" + accountData.get("total_messages") + ")";
                mMenuList[5] = mMenuList[5] + " " + accountData.get("created");
                if(accountData.get("bio") != "null")
                    mMenuList[6] = mMenuList[6] + " " + accountData.get("bio");
                else
                    mMenuList[6] = "No Biography";
                //mMenuList[7] is settings
                mMenuList[8] = mMenuList[8] + " - " + accountData.get("disk_used");
                mMenuList[9] = mMenuList[9] + " - " + accountData.get("bandwidth_used");
                adapter.notifyDataSetChanged();
            }
        };
        async.execute();
        return view;
    }

    private void selectItem(int position) {
        MainActivity activity = (MainActivity) getActivity();
        ImagesFragment imagesFragment;
        switch(position) {
            case 0:
                AlbumsFragment albumsFragment = new AlbumsFragment();
                activity.changeFragment(albumsFragment);
                break;
            case 1:
                imagesFragment = new ImagesFragment();
                imagesFragment.setImageCall("3/account/me/images/0");
                activity.changeFragment(imagesFragment);
                break;
            case 2:
                imagesFragment = new ImagesFragment();
                imagesFragment.setImageCall("3/account/me/likes");
                activity.changeFragment(imagesFragment);
                break;
            default:
                return;
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
}
