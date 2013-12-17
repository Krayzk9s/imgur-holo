package com.krayzk9s.imgurholo.activities;

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
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.services.UploadService;
import com.krayzk9s.imgurholo.tools.ApiCall;
import com.krayzk9s.imgurholo.tools.GetData;
import com.krayzk9s.imgurholo.ui.AccountFragment;
import com.krayzk9s.imgurholo.ui.AlbumsFragment;
import com.krayzk9s.imgurholo.ui.GalleryFragment;
import com.krayzk9s.imgurholo.ui.ImagesFragment;
import com.krayzk9s.imgurholo.ui.MessagingFragment;

import org.scribe.model.Token;
import org.scribe.model.Verifier;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends ImgurHoloActivity implements GetData {
    protected ActionBarDrawerToggle mDrawerToggle;
    protected CharSequence mDrawerTitle;
    protected CharSequence mTitle;
    protected DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;
	int oldChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        updateMenu();
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().show();
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        if(savedInstanceState == null)
            processIntent(getIntent());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Handle the back button
        SharedPreferences settings = getSettings();
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(getActionBar() != null)
                getActionBar().show();
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && settings.getBoolean("ConfirmExit", false) && isTaskRoot() && fragmentManager.getBackStackEntryCount() == 0) {
            //Ask the user if they want to quit
            new AlertDialog.Builder(this)
                    .setTitle("Quit?")
                    .setMessage("Are you sure you want to quit?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            //Stop the activity
                            MainActivity.this.finish();
                        }

                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }

    }

    public void updateMenu() {
        DrawerAdapter drawerAdapter = new DrawerAdapter(this, R.layout.menu_item);
        Log.d("theme", theme);
        Log.d("theme dark?", theme.equals(HOLO_DARK) + "");
        Log.d("theme light?", theme.equals(HOLO_LIGHT) + "");
        if (apiCall.loggedin && theme.equals(HOLO_DARK))
            drawerAdapter.setMenu(R.array.imgurMenuListLoggedIn, R.array.imgurMenuListDarkIcons);
        else if (!apiCall.loggedin && theme.equals(HOLO_DARK))
            drawerAdapter.setMenu(R.array.imgurMenuListLoggedOut, R.array.imgurMenuListDarkIconsLoggedOut);
        else if (apiCall.loggedin && theme.equals(HOLO_LIGHT))
            drawerAdapter.setMenu(R.array.imgurMenuListLoggedIn, R.array.imgurMenuListIcons);
        else
            drawerAdapter.setMenu(R.array.imgurMenuListLoggedOut, R.array.imgurMenuListIconsLoggedOut);
        mDrawerTitle = getTitle();
        if (mTitle == null)
            mTitle = "imgur Holo";
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDrawerList.setSelector(R.drawable.comment_select);
        mDrawerList.setAdapter(drawerAdapter);
        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    /* The click listener for ListView in the navigation drawer */
    protected class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    public class DrawerAdapter extends ArrayAdapter<String> {
        public String[] mMenuList;
        public TypedArray mMenuIcons;
        LayoutInflater mInflater;

        public DrawerAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setMenu(int list, int array) {
            mMenuList = getResources().getStringArray(list);
            mMenuIcons = getResources().obtainTypedArray(array);
        }

        @Override
        public int getCount() {
            return mMenuList.length;
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = View.inflate(this.getContext(), R.layout.menu_item, null);
            TextView menuItem = (TextView) convertView.findViewById(R.id.menu_text);
            ImageView menuIcon = (ImageView) convertView.findViewById(R.id.menu_icon);
            menuItem.setText(mMenuList[position]);
            menuIcon.setImageDrawable(mMenuIcons.getDrawable(position));
            return convertView;
        }
    }

    private void loadDefaultPage() {
        SharedPreferences settings = getSettings();
        if (!apiCall.loggedin || !settings.contains("DefaultPage") || settings.getString("DefaultPage", "").equals("Gallery")) {
            selectItem(0);
        } else if (settings.getString("DefaultPage", "").equals("Albums")) {
            selectItem(4);
        } else if (settings.getString("DefaultPage", "").equals("Images")) {
            selectItem(3);
        } else if (settings.getString("DefaultPage", "").equals("Favorites")) {
            selectItem(5);
        } else if (settings.getString("DefaultPage", "").equals("Account")) {
            selectItem(2);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("Going to process intent", "Processing intent...");
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        Log.d("New Intent", intent.toString());
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                int duration = Toast.LENGTH_SHORT;
                Toast toast;
                toast = Toast.makeText(this, "Uploading Image...", duration);
                toast.show();
                Intent serviceIntent = new Intent(this, UploadService.class);
                if(intent.getExtras() == null)
                    finish();
                serviceIntent.setData((Uri) intent.getExtras().get("android.intent.extra.STREAM"));
                startService(serviceIntent);
                finish();
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            Log.d("sending", "sending multiple");
            int duration = Toast.LENGTH_SHORT;
            Toast toast;
            toast = Toast.makeText(this, "Uploading Images...", duration);
            toast.show();
            ArrayList<Parcelable> list =
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            Intent serviceIntent = new Intent(this, UploadService.class);
            serviceIntent.putParcelableArrayListExtra("images", list);
            startService(serviceIntent);
            finish();
        } else if (Intent.ACTION_VIEW.equals(action) && intent.getData().toString().startsWith("imgur-holo")) {
            Uri uri = intent.getData();
            Log.d("URI", "" + action + "/" + type);
            String uripath = "";
            if (uri != null)
                uripath = uri.toString();
            Log.d("URI", uripath);
            Log.d("URI", "HERE");

            if (uri != null && uripath.startsWith(ApiCall.OAUTH_CALLBACK_URL)) {
                apiCall.verifier = new Verifier(uri.getQueryParameter("code"));
                CallbackAsync callbackAsync = new CallbackAsync(apiCall, this);
                callbackAsync.execute();
            }
        }
        else if (getSupportFragmentManager().getFragments() == null) {
            loadDefaultPage();
        }
    }

    public SharedPreferences getSettings() {
        if(getApplicationContext() != null)
            return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        else
            return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (theme.equals(HOLO_LIGHT))
            inflater.inflate(R.menu.main, menu);
        else
            inflater.inflate(R.menu.main_dark, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("request code", requestCode + "");
        if (resultCode == -1)
            Log.d("intent", data.toString());
        if (requestCode == 3 && resultCode == -1) {
            int duration = Toast.LENGTH_SHORT;
            Toast toast;
            toast = Toast.makeText(this, "Uploading Image...", duration);
            toast.show();
            Intent serviceIntent = new Intent(this, UploadService.class);
            serviceIntent.setAction("com.krayzk9s.imgurholo.services.UploadService");
            serviceIntent.setData(data.getData());
            startService(serviceIntent);
            return;
        }
        if (requestCode == 4 && resultCode == -1) {
            int duration = Toast.LENGTH_SHORT;
            Toast toast;
            toast = Toast.makeText(this, "Uploading Images...", duration);
            toast.show();
            if(data.getExtras() != null)
                Log.d("intent extras", data.getExtras().toString());
            Intent serviceIntent = new Intent(this, UploadService.class);
            serviceIntent.setAction("com.krayzk9s.imgurholo.services.UploadService");
            serviceIntent.setData(data.getData());
            startService(serviceIntent);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void displayUpload() {
        new AlertDialog.Builder(this).setTitle("Upload Options")
                .setItems(R.array.upload_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent;
                        MainActivity activity = MainActivity.this;
                        switch (whichButton) {
                            case 0:
                                final EditText urlText = new EditText(activity);
                                urlText.setSingleLine();
                                new AlertDialog.Builder(activity).setTitle("Enter URL").setView(urlText).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        if(urlText.getText() != null) {
                                            UrlAsync urlAsync = new UrlAsync(urlText.getText().toString(), apiCall);
                                            urlAsync.execute();
                                        }
                                    }
                                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Do nothing.
                                    }
                                }).show();
                                break;
                            case 1:
                                intent = new Intent();
                                intent.setType("image/*");
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                                startActivityForResult(Intent.createChooser(intent,
                                        "Select Picture"), 3);
                                break;
                            case 2:
                                intent = new Intent("android.media.action.IMAGE_CAPTURE");
                                startActivityForResult(intent, 4);
                                break;
                            case 3:
                                new AlertDialog.Builder(activity).setTitle("Image Explanation").setMessage("You can! You just have to do it from the gallery or other app by multiselecting. :(" +
                                        " The ELI5 explanation is that basically the Android API is a bit weird in this area. More explicitly, I cannot determine a way to request multiple files via intent" +
                                        ", if you know a work around feel free to contact me on Google Play.").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        //do nothing
                                    }
                                }).show();
                            default:
                                break;
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    protected void selectItem(int position) {
		mDrawerList.setItemChecked(oldChecked, true);
        mDrawerLayout.closeDrawer(mDrawerList);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        switch (position) {
            case 0:
                mDrawerList.setItemChecked(position, true);
				oldChecked = position;
                GalleryFragment galleryFragment = new GalleryFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.frame_layout, galleryFragment)
                        .commit();
                break;
            case 1:
                if (apiCall.loggedin) {
                    mDrawerList.setItemChecked(position, true);
					oldChecked = position;
                    AccountFragment accountFragment = new AccountFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("username", "me");
                    accountFragment.setArguments(bundle);
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, accountFragment)
                            .commit();
                } else {
                    displayUpload();
                }
                break;
            case 2:
                if(apiCall.loggedin) {
                    displayUpload();
                } else {
                    Intent myIntent = new Intent(this, SettingsActivity.class);
                    startActivity(myIntent);
                    updateMenu();
                }
                break;
            case 3:
                if (apiCall.loggedin) {
                    mDrawerList.setItemChecked(position, true);
					oldChecked = position;
                    ImagesFragment imagesFragment = new ImagesFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("imageCall", "3/account/me/images");
                    imagesFragment.setArguments(bundle);
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, imagesFragment)
                            .commit();
                } else {
                    LoginAsync loginAsync = new LoginAsync(apiCall, this);
                    loginAsync.execute();
                }
                break;
            case 4:
                if (apiCall.loggedin) {
                    mDrawerList.setItemChecked(position, true);
					oldChecked = position;
                    AlbumsFragment albumsFragment = new AlbumsFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("username", "me");
                    albumsFragment.setArguments(bundle);
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, albumsFragment)
                            .commit();
                }
                break;
            case 5:
                if (apiCall.loggedin) {
                    mDrawerList.setItemChecked(position, true);
					oldChecked = position;
                    ImagesFragment imagesFragment = new ImagesFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("imageCall", "3/account/me/likes");
                    imagesFragment.setArguments(bundle);
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, imagesFragment)
                            .commit();
                }
                break;
            case 6:
                if (apiCall.loggedin) {
                    mDrawerList.setItemChecked(position, true);
					oldChecked = position;
                    MessagingFragment messagingFragment = new MessagingFragment();
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, messagingFragment)
                            .commit();
                }
                break;
            case 7:
                if (apiCall.loggedin) {
                    Intent myIntent = new Intent(this, SettingsActivity.class);
                    startActivity(myIntent);
                }
                break;
            case 8:
                if (apiCall.loggedin) {
                    SharedPreferences settings = getSettings();
                    SharedPreferences.Editor editor = settings.edit();
                    editor.remove("AccessToken");
                    editor.remove("RefreshToken");
                    editor.commit();
                    apiCall.loggedin = false;
					updateMenu();
                }
                break;
        }
    }

    public void onGetObject(Object o, String tag) {

    }

    public void handleException(Exception e, String tag) {

    }

    private static class UrlAsync extends AsyncTask<Void, Void, Void> {
        String urlText;
        ApiCall apiCall;
        public UrlAsync(String _urlText, ApiCall _apiCall) {
            urlText = _urlText;
            apiCall = _apiCall;
        }
        @Override
        protected Void doInBackground(Void... voids) {
            HashMap<String, Object> hashMap = new HashMap<String, Object>();
            hashMap.put("image", urlText);
            apiCall.makeCall("3/image", "post", hashMap);
            return null;
        }
    }

    private static class LoginAsync extends android.os.AsyncTask<Void, Void, String> {
        ApiCall apiCall;
        MainActivity activity;

        public LoginAsync(ApiCall _apiCall, MainActivity _activity) {
            apiCall = _apiCall;
            activity = _activity;
        }
            @Override
            protected String doInBackground(Void... voids) {
                String authURL = apiCall.service.getAuthorizationUrl(ApiCall.EMPTY_TOKEN);
                Log.d("AuthURL", authURL);
                return authURL;
            }

            @Override
            protected void onPostExecute(String authURL) {
                activity.startActivity(new Intent("android.intent.action.VIEW",
                        Uri.parse(authURL)).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_NO_HISTORY
                        | Intent.FLAG_FROM_BACKGROUND));
                Log.d("AuthURL2", authURL);
            }
        }
    private static class CallbackAsync extends AsyncTask<Void, Void, Void> {
        ApiCall apiCall;
        MainActivity activity;

        public CallbackAsync(ApiCall _apiCall, MainActivity _activity) {
            apiCall = _apiCall;
            activity = _activity;
        }
        @Override
        protected Void doInBackground(Void... voids) {
            SharedPreferences settings = activity.getSettings();
            SharedPreferences.Editor editor = settings.edit();
            apiCall.accessToken = apiCall.service.getAccessToken(Token.empty(), apiCall.verifier);
            Log.d("URI", apiCall.verifier.toString());
            Log.d("URI", apiCall.accessToken.getToken());
            Log.d("URI", apiCall.accessToken.getSecret());
            editor.putString("RefreshToken", apiCall.accessToken.getSecret());
            editor.putString("AccessToken", apiCall.accessToken.getToken());
            editor.commit();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            apiCall.loggedin = true;
            activity.updateMenu();
        }
    }
    public ApiCall getApiCall() {
        return apiCall;
    }
}
