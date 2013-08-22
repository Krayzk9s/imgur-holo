package com.krayzk9s.imgurholo;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
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
        Log.d("SettingTitle", username);
        if(username != "me")
            activity.setTitle(username + "'s Account");
        else
            activity.setTitle("My Account");
        View view = inflater.inflate(R.layout.account_layout, container, false);
        mDrawerList = (ListView) view.findViewById(R.id.account_list);
        if(username.equals("me"))
            mMenuList = getResources().getStringArray(R.array.accountMenu);
        else
            mMenuList = getResources().getStringArray(R.array.accountMenuNotMe);
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
        if(username.equals("me"))
            mMenuList = getResources().getStringArray(R.array.accountMenu);
        else
            mMenuList = getResources().getStringArray(R.array.accountMenuNotMe);
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
                    if(username.equals("me")) {
                        JSONObject statsJSON = activity.makeCall("3/account/" + username + "/stats", "get", null);
                        JSONObject settingsJSON = activity.makeCall("3/account/" + username + "/settings", "get", null);
                        JSONObject messageJSON = activity.makeCall("/3/account/me/notifications/messages?new=false", "get", null);
                        statsJSON = statsJSON.getJSONObject("data");
                        accountData.put("total_images", Integer.toString(statsJSON.getInt("total_images")));
                        accountData.put("total_albums", Integer.toString(statsJSON.getInt("total_albums")));
                        accountData.put("disk_used", statsJSON.getString("disk_used"));
                        accountData.put("bandwidth_used", statsJSON.getString("bandwidth_used"));
                        accountData.put("total_messages", Integer.toString(messageJSON.getJSONArray("data").length()));
                        accountData.put("reputation", Integer.toString(accountInfoJSON.getJSONObject("data").getInt("reputation")));

                        settingsJSON = settingsJSON.getJSONObject("data");
                        accountData.put("email", settingsJSON.getString("email"));
                        accountData.put("album_privacy", settingsJSON.getString("album_privacy"));
                        if (settingsJSON.getBoolean("public_images") == false)
                            accountData.put("public_images", "private");
                        else
                            accountData.put("public_images", "public");
                        if (settingsJSON.getBoolean("messaging_enabled") == false)
                            accountData.put("messaging_enabled", "disabled");
                        else
                            accountData.put("messaging_enabled", "enabled");
                    }
                    else {
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

                    if(username.equals("me")) {

                    }
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
            if(username.equals("me"))
                mMenuList = getResources().getStringArray(R.array.accountMenu);
            else
                mMenuList = getResources().getStringArray(R.array.accountMenuNotMe);
            adapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.drawer_list_item, mMenuList);
            mDrawerList.setAdapter(adapter);
            if(username.equals("me")) {
                mMenuList[0] = mMenuList[0] + " (" + accountData.get("total_albums") + ")";
                mMenuList[1] = mMenuList[1] + " (" + accountData.get("total_images") + ")";
                mMenuList[2] = mMenuList[2] + " (" + accountData.get("total_likes") + ")";
                mMenuList[3] = mMenuList[3] + " (" + accountData.get("total_comments") + ")";
                mMenuList[4] = mMenuList[4] + " (" + accountData.get("total_messages") + ")";
                mMenuList[5] = mMenuList[5] + " " + accountData.get("created");
                if (accountData.get("bio") != "null")
                    mMenuList[6] = accountData.get("bio");
                else
                    mMenuList[6] = "No Biography";
                mMenuList[7] = "Your imgur e-mail is " + accountData.get("email");
                mMenuList[8] = "Your albums are " + accountData.get("album_privacy");
                mMenuList[9] = "Your images are " + accountData.get("public_images");
                mMenuList[10] = "Your messaging is " + accountData.get("messaging_enabled");
                mMenuList[11] = mMenuList[11] + " - " + accountData.get("disk_used");
                mMenuList[12] = mMenuList[12] + " - " + accountData.get("bandwidth_used");
                mMenuList[13] = mMenuList[13] + ": " + accountData.get("reputation");
            }
            else if(accountData != null) {
                mMenuList[0] = mMenuList[0] + " (" + accountData.get("total_albums") + ")";
                mMenuList[1] = mMenuList[1] + " (" + accountData.get("total_images") + ")";
                mMenuList[2] = mMenuList[2] + " (" + accountData.get("total_likes") + ")";
                mMenuList[3] = mMenuList[3] + " (" + accountData.get("total_comments") + ")";
                mMenuList[4] = mMenuList[4] + " " + accountData.get("created");
                if (accountData.get("bio") != "null")
                    mMenuList[5] = accountData.get("bio");
                else
                    mMenuList[5] = "No Biography";
                mMenuList[6] = mMenuList[6] + ": " + accountData.get("reputation");
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
            case 4:
                if(username == "me") {
                    activity.setTitle("My Messages");
                    MessagingFragment messagingFragment = new MessagingFragment();
                    activity.changeFragment(messagingFragment);
                }
                break;
            case 8:
                new AlertDialog.Builder(activity).setTitle("Set Album Privacy")
                        .setItems(R.array.albumPrivacy, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                MainActivity activity = (MainActivity) getActivity();
                                SharedPreferences settings = activity.getSettings();
                                SharedPreferences.Editor editor = settings.edit();
                                String privacy = "";
                                switch (whichButton) {
                                    case 0:
                                        privacy = "public";
                                        mMenuList[8] = "Your albums are public";
                                        break;
                                    case 1:
                                        privacy = "hidden";
                                        mMenuList[8] = "Your albums are hidden";
                                        break;
                                    case 2:
                                        privacy = "secret";
                                        mMenuList[8] = "Your albums are secret";
                                        break;
                                    default:
                                        break;
                                }
                                SettingsAsync settingsAsync = new SettingsAsync("album_privacy", privacy, accountData.get("name"));
                                settingsAsync.execute();
                                adapter.notifyDataSetChanged();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                break;
            case 9:
                new AlertDialog.Builder(activity).setTitle("Set Image Privacy")
                        .setItems(R.array.privacy, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                MainActivity activity = (MainActivity) getActivity();
                                SharedPreferences settings = activity.getSettings();
                                SharedPreferences.Editor editor = settings.edit();
                                int privacy = 0;
                                switch (whichButton) {
                                    case 0:
                                        privacy = 0;
                                        mMenuList[9] = "Your images are private";
                                        break;
                                    case 1:
                                        privacy = 1;
                                        mMenuList[9] = "Your images are public";
                                        break;
                                    default:
                                        break;
                                }
                                SettingsAsync settingsAsync = new SettingsAsync("public_images", privacy, accountData.get("name"));
                                settingsAsync.execute();
                                adapter.notifyDataSetChanged();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                break;
            case 10:
                new AlertDialog.Builder(activity).setTitle("Enable/Disable Messaging")
                        .setItems(R.array.messaging, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                MainActivity activity = (MainActivity) getActivity();
                                SharedPreferences settings = activity.getSettings();
                                SharedPreferences.Editor editor = settings.edit();
                                int enable = 0;
                                switch (whichButton) {
                                    case 0:
                                        enable = 1;
                                        mMenuList[10] = "Your messaging is enabled";
                                        break;
                                    case 1:
                                        enable = 0;
                                        mMenuList[10] = "Your messaging is disabled";
                                        break;
                                    default:
                                        break;
                                }
                                SettingsAsync settingsAsync = new SettingsAsync("messaging_enabled", enable, accountData.get("name"));
                                settingsAsync.execute();
                                adapter.notifyDataSetChanged();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                break;
            default:
                break;
        }
    }

    private class SettingsAsync extends AsyncTask<Void, Void, Void> {
        private Object data;
        private String settingName;
        private String username;

        public SettingsAsync(String _settingName, Object _data, String _username) {
            data = _data;
            settingName = _settingName;
            username = _username;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = (MainActivity) getActivity();
            HashMap<String, Object> args = new HashMap<String, Object>();
            args.put(settingName, data);
            activity.makeCall("/3/account/me/settings", "post", args);
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            getAccount();
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
