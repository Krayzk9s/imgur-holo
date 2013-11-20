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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.tools.ApiCall;
import com.krayzk9s.imgurholo.tools.Fetcher;
import com.krayzk9s.imgurholo.tools.GetData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Kurt Zimmer on 7/22/13.
 */
public class AccountFragment extends Fragment implements GetData {

    String[] mMenuList;
    ArrayAdapter<String> adapter;
    String username;
    SearchView mSearchView;
    MenuItem searchItem;
    ListView mDrawerList;
    TextView usernameText;
    TextView biography;
    TextView created;
    TextView reputation;
    final static String ACCOUNTDATA = "accountData";
    final static String COUNTDATA = "countData";
    final static String LIKEDATA = "likeData";
    final static String COMMENTDATA = "commentData";
    final static String ALBUMDATA = "albumData";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        username = bundle.getString("username");
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        if(activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
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
                AccountFragment accountFragment = new AccountFragment();
                Bundle bundle = new Bundle();
                bundle.putString("username", mSearchView.getQuery().toString());
                accountFragment.setArguments(bundle);
                activity.changeFragment(accountFragment, true);
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
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        if(!username.equals("me"))
            activity.setTitle(username + "'s Account");
        else
            activity.setTitle("My Account");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d("Username", username);
        MainActivity activity = (MainActivity) getActivity();
        SharedPreferences settings = activity.getSettings();
        Log.d("SettingTitle", username);
        View view = inflater.inflate(R.layout.account_layout, container, false);
        LinearLayout header = (LinearLayout) view.findViewById(R.id.header);
        if(settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
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
        if (savedInstanceState == null) {
            getAccount();
        }
        return view;
    }

    public void onGetObject(Object data, String tag) {
        try {
        if(data == null ) {
            return;
        }
        JSONObject jsonData;

        /*int duration = Toast.LENGTH_SHORT;
                Toast toast;
                MainActivity activity = (MainActivity) getActivity();
                toast = Toast.makeText(activity, "User not found", duration);
                toast.show();
                activity.getFragmentManager().popBackStack();*/

        if(tag.equals(ACCOUNTDATA)) {
            jsonData = ((JSONObject)data).getJSONObject("data");
            if(jsonData.has("error"))
                return;
            Calendar accountCreationDate = Calendar.getInstance();
            accountCreationDate.setTimeInMillis((long) jsonData.getInt("created") * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            String accountcreated = sdf.format(accountCreationDate.getTime());
            created.setText(accountcreated);
            reputation.setText(Integer.toString(jsonData.getInt("reputation")));
            if (jsonData.getString("bio") != null && !jsonData.getString("bio").equals("null") && !jsonData.getString("bio").equals(""))
                biography.setText(jsonData.getString("bio"));
            else
                biography.setText("No Biography");
        } else if(tag.equals(COUNTDATA)) {
            jsonData = ((JSONObject)data).getJSONObject("data");
            if(jsonData.has("error"))
                return;
            if(jsonData.getInt("status") == 200)
                mMenuList[1] = mMenuList[1] + " (" + Integer.toString(jsonData.getInt("data")) + ")";
            else
                mMenuList[1] = mMenuList[1] + " (0)";
        } else if(tag.equals(ALBUMDATA)) {
            jsonData = ((JSONObject)data);
            if(jsonData.has("error"))
                return;
            if(jsonData.getInt("status") == 200)
                mMenuList[0] = mMenuList[0] + " (" + Integer.toString(jsonData.getJSONObject("data").length()) + ")";
            else
                mMenuList[0] = mMenuList[0] + " (0)";
        } else if(tag.equals(LIKEDATA)) {
            JSONArray jsonArray = ((JSONObject)data).getJSONArray("data");
            mMenuList[2] = mMenuList[2] + " (" + String.valueOf(jsonArray.length()) + ")";
        } else if(tag.equals(COMMENTDATA)) {
            jsonData = ((JSONObject)data);
            mMenuList[3] = mMenuList[3] + " (" + String.valueOf(jsonData.getInt("data")) + ")";
        }
        adapter.notifyDataSetChanged();
        }
        catch (JSONException e) {
            Log.e("Error!", e.toString());
        }
    }

    private void getAccount() {
        mMenuList = getResources().getStringArray(R.array.accountMenu);
        adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.drawer_list_item, mMenuList);
        mDrawerList.setAdapter(adapter);
        Fetcher fetcher = new Fetcher(this, "3/account/" + username, ApiCall.GET, null, ((ImgurHoloActivity)getActivity()).getApiCall(), ACCOUNTDATA);
        fetcher.execute();
        fetcher = new Fetcher(this, "3/account/" + username + "/likes", ApiCall.GET, null, ((ImgurHoloActivity)getActivity()).getApiCall(), LIKEDATA);
        fetcher.execute();
        fetcher = new Fetcher(this, "3/account/" + username + "/images/count", ApiCall.GET, null, ((ImgurHoloActivity)getActivity()).getApiCall(), COUNTDATA);
        fetcher.execute();
        fetcher = new Fetcher(this, "3/account/" + username + "/comments/count", ApiCall.GET, null, ((ImgurHoloActivity)getActivity()).getApiCall(), COMMENTDATA);
        fetcher.execute();
        fetcher = new Fetcher(this, "/3/account/" + username + "/albums/ids", ApiCall.GET, null, ((ImgurHoloActivity)getActivity()).getApiCall(), ALBUMDATA);
        fetcher.execute();
    }

    private void selectItem(int position) {
        final MainActivity activity = (MainActivity) getActivity();
        ImagesFragment imagesFragment;
        Bundle bundle;
        switch (position) {
            case 0:
                AlbumsFragment albumsFragment = new AlbumsFragment();
                bundle = new Bundle();
                bundle.putString("username", "me");
                albumsFragment.setArguments(bundle);
                activity.changeFragment(albumsFragment, true);
                break;
            case 1:
                imagesFragment = new ImagesFragment();
                bundle = new Bundle();
                bundle.putString("imageCall", "3/account/" + username + "/images");
                imagesFragment.setArguments(bundle);
                activity.changeFragment(imagesFragment, true);
                break;
            case 2:
                imagesFragment = new ImagesFragment();
                bundle = new Bundle();
                bundle.putString("imageCall", "3/account/" + username + "/likes");
                imagesFragment.setArguments(bundle);
                activity.changeFragment(imagesFragment, true);
                break;
            case 3:
                CommentsFragment commentsFragment = new CommentsFragment();
                bundle = new Bundle();
                bundle.putString("username", username);
                commentsFragment.setArguments(bundle);
                activity.changeFragment(commentsFragment, true);
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

    public void handleException(Exception e, String tag) {
        Log.e("Error!", e.toString());
    }
}
