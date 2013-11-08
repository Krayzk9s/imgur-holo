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

import android.support.v4.app.Fragment;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.Toast;

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
    String username;
    SearchView mSearchView;
    MenuItem searchItem;
    ListView mDrawerList;
    TextView usernameText;
    TextView biography;
    TextView created;
    TextView reputation;

    public AccountFragment(String _username) {
        username = _username;
    }

    @Override
    public void onCreate(Bundle save) {
        super.onCreate(save);
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
        menu.findItem(R.id.action_search).setVisible(true);
        menu.findItem(R.id.action_refresh).setVisible(true);
        searchItem = menu.findItem(R.id.action_search);
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
                MainActivity activity = (MainActivity) getActivity();
                AccountFragment accountFragment = new AccountFragment(mSearchView.getQuery().toString());
                activity.changeFragment(accountFragment);
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
        MainActivity activity = (MainActivity) getActivity();
        SharedPreferences settings = activity.getSettings();

        Log.d("SettingTitle", username);
        if(username != "me")
            activity.setTitle(username + "'s Account");
        else
            activity.setTitle("My Account");
        View view = inflater.inflate(R.layout.account_layout, container, false);
        LinearLayout header = (LinearLayout) view.findViewById(R.id.header);
        if(settings.getString("theme", activity.HOLO_LIGHT).equals(activity.HOLO_LIGHT))
            header.setBackgroundColor(0xFFCCCCCC);
        biography = (TextView) view.findViewById(R.id.biography);
        usernameText = (TextView) view.findViewById(R.id.username);
        usernameText.setText(username);
        created = (TextView) view.findViewById(R.id.created);
        reputation = (TextView) view.findViewById(R.id.reputation);
        mDrawerList = (ListView) view.findViewById(R.id.account_list);
        mMenuList = getResources().getStringArray(R.array.accountMenu);
        adapter = new ArrayAdapter<String>(view.getContext(),
                R.layout.drawer_list_item, mMenuList);
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        if (savedInstanceState == null && accountData == null) {
            getAccount();

        } else if(savedInstanceState != null) {
            Bundle extras = savedInstanceState.getBundle("accountData");
            accountData = (HashMap<String, String>) extras.getSerializable("HashMap");
            updateData();
        } else
        {
            updateData();
        }
        return view;
    }

    private void getAccount() {
        mMenuList = getResources().getStringArray(R.array.accountMenu);
        adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.drawer_list_item, mMenuList);
        mDrawerList.setAdapter(adapter);
        AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    MainActivity activity = (MainActivity) getActivity();
                    JSONObject accountInfoJSON = activity.makeCall("3/account/" + username, "get", null);
                    if(accountInfoJSON.getJSONObject("data").has("error")) {
                        return null;
                    }
                    accountData = new HashMap<String, String>();
                    JSONObject likesJSON = activity.makeCall("3/account/" + username + "/likes", "get", null);
                    JSONObject commentJSON = activity.makeCall("3/account/" + username + "/comments/count", "get", null);
                        try {
                            JSONObject imagesJSON = activity.makeCall("3/account/" + username + "/images/count", "get", null);
                            if(imagesJSON.getInt("status") == 200)
                                accountData.put("total_images", Integer.toString(imagesJSON.getInt("data")));
                            else
                                accountData.put("total_images", "0");
                            JSONObject albumsJSON = activity.makeCall("/3/account/" + username + "/albums/ids", "get", null);
                            if(albumsJSON.getInt("status") == 200)
                                accountData.put("total_albums", Integer.toString(albumsJSON.getJSONArray("data").length()));
                            else
                                accountData.put("total_albums", "0");
                        }
                        catch (Exception e) {
                            Log.e("Account get error", e.toString());
                        }
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
                    accountData.put("reputation", Integer.toString(accountInfoJSON.getInt("reputation")));

                    JSONArray likesJSONArray = likesJSON.getJSONArray("data");
                    accountData.put("total_likes", String.valueOf(likesJSONArray.length()));
                    accountData.put("total_comments", String.valueOf(commentJSON.getInt("data")));
                } catch (Exception e) {
                    Log.e("Error!", e.toString());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if(mMenuList != null && accountData != null)
                    updateData();
            }
        };
        async.execute();
    }

    private void updateData() {
        if(isAdded()) {
            if (accountData.get("bio") != null && !accountData.get("bio").equals("null") && !accountData.get("bio").equals(""))
                biography.setText(accountData.get("bio"));
            else
                biography.setText("No Biography");
            reputation.setText(accountData.get("reputation"));
            created.setText(accountData.get("created"));
            mMenuList = getResources().getStringArray(R.array.accountMenu);
            adapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.drawer_list_item, mMenuList);
            mDrawerList.setAdapter(adapter);
            if(accountData != null) {
                mMenuList[0] = mMenuList[0] + " (" + accountData.get("total_albums") + ")";
                mMenuList[1] = mMenuList[1] + " (" + accountData.get("total_images") + ")";
                mMenuList[2] = mMenuList[2] + " (" + accountData.get("total_likes") + ")";
                mMenuList[3] = mMenuList[3] + " (" + accountData.get("total_comments") + ")";
            }
            else {
                int duration = Toast.LENGTH_SHORT;
                Toast toast;
                MainActivity activity = (MainActivity) getActivity();
                toast = Toast.makeText(activity, "User not found", duration);
                toast.show();
                activity.getFragmentManager().popBackStack();
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void selectItem(int position) {
        final MainActivity activity = (MainActivity) getActivity();
        ImagesFragment imagesFragment;
        switch (position) {
            case 0:
                if(username != "me")
                    activity.setTitle(username + "'s Albums");
                else
                    activity.setTitle("My Albums");
                AlbumsFragment albumsFragment = new AlbumsFragment(username);
                activity.changeFragment(albumsFragment);
                break;
            case 1:
                if(username != "me")
                    activity.setTitle(username + "'s Images");
                else
                    activity.setTitle("My Images");
                imagesFragment = new ImagesFragment();
                imagesFragment.setImageCall(null, "3/account/" + username + "/images", null);
                activity.changeFragment(imagesFragment);
                break;
            case 2:
                if(username != "me")
                    activity.setTitle(username + "'s Favorites");
                else
                    activity.setTitle("My Favorites");
                imagesFragment = new ImagesFragment();
                imagesFragment.setImageCall(null, "3/account/" + username + "/likes", null);
                activity.changeFragment(imagesFragment);
                break;
            case 3:
                if(username != "me")
                    activity.setTitle(username + "'s Comments");
                else
                    activity.setTitle("My Comments");
                CommentsFragment commentsFragment = new CommentsFragment(username);
                activity.changeFragment(commentsFragment);
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

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Bundle extras = new Bundle();
        extras.putSerializable("HashMap", accountData);
        savedInstanceState.putBundle("accountData", extras);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
}
