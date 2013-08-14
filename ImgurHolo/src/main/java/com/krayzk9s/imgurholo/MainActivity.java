package com.krayzk9s.imgurholo;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Base64;
import android.util.DisplayMetrics;
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

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.ImgUr3Api;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends FragmentActivity {

    public static final String OAUTH_CALLBACK_SCHEME = "imgur-holo";
    public static final String OAUTH_CALLBACK_HOST = "authcallback";
    public static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME + "://" + OAUTH_CALLBACK_HOST;
    public static final String MASHAPE_KEY = "CoV9d8oMmqhy8YdAbCAnB1MroW1xMJpP";
    public static final String PREFS_NAME = "ImgurPrefs";
    public static final String MASHAPE_URL = "https://imgur-apiv3.p.mashape.com/";
    private static final String CLIENTID = "4cd3f96f162ac80";
    private static final String SECRETID = "9cd3c621a4e064422e60aba4ccf84d6b149b4463";
    private static final Token EMPTY_TOKEN = null;
    public static int HOLO_DARK = 0;
    public static int HOLO_LIGHT = 1;
    final OAuthService service = new ServiceBuilder().provider(ImgUr3Api.class).apiKey(CLIENTID).debug().callback(OAUTH_CALLBACK_URL).apiSecret(SECRETID).build();
    public int theme;
    Token accessToken;
    Verifier verifier;
    boolean loggedin;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (settings.contains("theme")) {
            theme = settings.getInt("theme", HOLO_LIGHT);
        } else
            theme = HOLO_LIGHT;

        if (theme == HOLO_LIGHT)
            setTheme(R.style.AppTheme);
        else
            setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (settings.contains("RefreshToken")) {
            loggedin = true;
        }
        updateMenu();

        // enable ActionBar app icon to behave as action to toggle nav drawer
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

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
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                getActionBar().show();
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        if (savedInstanceState == null && getCallingActivity() == null)
            loadDefaultPage();
    }

    public void updateMenu() {
        DrawerAdapter drawerAdapter = new DrawerAdapter(this, R.layout.menu_item);
        if (loggedin && theme == HOLO_DARK)
            drawerAdapter.setMenu(R.array.imgurMenuListLoggedIn, R.array.imgurMenuListDarkIcons);
        else if (!loggedin && theme == HOLO_DARK)
            drawerAdapter.setMenu(R.array.imgurMenuListLoggedOut, R.array.imgurMenuListDarkIconsLoggedOut);
        else if (loggedin && theme == HOLO_LIGHT)
            drawerAdapter.setMenu(R.array.imgurMenuListLoggedIn, R.array.imgurMenuListIcons);
        else
            drawerAdapter.setMenu(R.array.imgurMenuListLoggedOut, R.array.imgurMenuListIconsLoggedOut);
        mDrawerTitle = getTitle();
        if (mTitle == null)
            mTitle = "imgur Holo";
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(drawerAdapter);
        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (!mDrawerLayout.isDrawerOpen(GravityCompat.START))
                mDrawerLayout.openDrawer(GravityCompat.START);
            else
                mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Handle the back button
        SharedPreferences settings = getSettings();
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (keyCode == KeyEvent.KEYCODE_BACK) {
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

    private void loadDefaultPage() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (!loggedin || !settings.contains("DefaultPage") || settings.getString("DefaultPage", "").equals("Gallery")) {
            setTitle("Gallery");
            GalleryFragment galleryFragment = new GalleryFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.frame_layout, galleryFragment)
                    .commit();
        } else if (settings.getString("DefaultPage", "").equals("Your Albums")) {
            setTitle("Your Albums");
            AlbumsFragment albumsFragment = new AlbumsFragment("me");
            fragmentManager.beginTransaction()
                    .add(R.id.frame_layout, albumsFragment)
                    .commit();
        } else if (settings.getString("DefaultPage", "").equals("Your Images")) {
            setTitle("Your Images");
            ImagesFragment imagesFragment = new ImagesFragment();
            imagesFragment.setImageCall(null, "3/account/me/images", null);
            fragmentManager.beginTransaction()
                    .add(R.id.frame_layout, imagesFragment)
                    .commit();
        } else if (settings.getString("DefaultPage", "").equals("Your Favorites")) {
            setTitle("Your Favorites");
            ImagesFragment imagesFragment = new ImagesFragment();
            imagesFragment.setImageCall(null, "3/account/me/likes", null);
            fragmentManager.beginTransaction()
                    .add(R.id.frame_layout, imagesFragment)
                    .commit();
        } else if (settings.getString("DefaultPage", "").equals("Your Account")) {
            setTitle("Your Account");
            AccountFragment accountFragment = new AccountFragment("me");
            fragmentManager.beginTransaction()
                    .add(R.id.frame_layout, accountFragment)
                    .commit();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("New Intent", intent.toString());
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                int duration = Toast.LENGTH_SHORT;
                Toast toast;
                toast = Toast.makeText(this, "Uploading Image...", duration);
                toast.show();
                SendImage async = new SendImage((Uri) intent.getExtras().get("android.intent.extra.STREAM"));
                async.execute();
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            Log.d("sending", "sending multiple");
            int duration = Toast.LENGTH_SHORT;
            Toast toast;
            toast = Toast.makeText(this, "Uploading Images...", duration);
            toast.show();
            ArrayList<Parcelable> list =
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            for (Parcelable parcel : list) {
                Uri uri = (Uri) parcel;
                Log.d("sending", uri.toString());
                SendImage async = new SendImage(uri);
                async.execute();
                /// do things here with each image source path.
            }
        } else if (Intent.ACTION_VIEW.equals(action) && intent.getData().toString().startsWith("http")) {
            String uri = intent.getData().toString();
            final String image = uri.split("/")[3].split("\\.")[0];
            Log.d("image", image);
            AsyncTask<Void, Void, JSONObject> async = new AsyncTask<Void, Void, JSONObject>() {
                @Override
                protected JSONObject doInBackground(Void... voids) {
                    JSONObject imageData = makeCall("/3/image/" + image, "get", null);
                    return imageData;
                }

                @Override
                protected void onPostExecute(JSONObject imageData) {
                    Log.d("data", imageData.toString());
                    try {
                        SingleImageFragment singleImageFragment = new SingleImageFragment();
                        singleImageFragment.setParams(imageData.getJSONObject("data"));
                        singleImageFragment.setGallery(true);
                        changeFragment(singleImageFragment);
                    } catch (Exception e) {
                        Log.e("Error!", e.toString());
                    }
                }
            };
            async.execute();

        } else {
            Uri uri = intent.getData();
            Log.d("URI", "resumed2!");
            String uripath = "";
            if (uri != null)
                uripath = uri.toString();
            Log.d("URI", uripath);
            Log.d("URI", "HERE");

            if (uri != null && uripath.startsWith(OAUTH_CALLBACK_URL)) {
                verifier = new Verifier(uri.getQueryParameter("code"));

                AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        accessToken = service.getAccessToken(Token.empty(), verifier);
                        Log.d("URI", verifier.toString());
                        Log.d("URI", accessToken.getToken());
                        Log.d("URI", accessToken.getSecret());
                        editor.putString("RefreshToken", accessToken.getSecret());
                        editor.putString("AccessToken", accessToken.getToken());
                        editor.commit();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        loggedin = true;
                        updateMenu();
                    }
                };
                async.execute();
            }
        }
    }

    public Token renewAccessToken() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        accessToken = service.refreshAccessToken(accessToken);
        Log.d("URI", accessToken.getRawResponse());
        editor.putString("AccessToken", accessToken.getToken());
        editor.commit();
        return accessToken;
    }

    public Token getAccessToken() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (settings.contains("RefreshToken")) {
            accessToken = new Token(settings.getString("AccessToken", ""), settings.getString("RefreshToken", ""));
            loggedin = true;
            Log.d("URI", accessToken.toString());
        } else {
            loggedin = false;
            login();
        }
        return accessToken;
    }

    public SharedPreferences getSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        return settings;
    }

    public void login() {

        AsyncTask<Void, Void, String> async = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                String authURL = service.getAuthorizationUrl(EMPTY_TOKEN);
                Log.d("AuthURL", authURL);
                return authURL;
            }

            @Override
            protected void onPostExecute(String authURL) {
                startActivity(new Intent("android.intent.action.VIEW",
                        Uri.parse(authURL)).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_NO_HISTORY
                        | Intent.FLAG_FROM_BACKGROUND));
                Log.d("AuthURL2", authURL);
            }
        };
        async.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (theme == HOLO_LIGHT)
            inflater.inflate(R.menu.main, menu);
        else
            inflater.inflate(R.menu.main_dark, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        return super.onPrepareOptionsMenu(menu);
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
            SendImage async = new SendImage((Uri) data.getData());
            async.execute(); // Handle single image being sent
            return;
        }
        if (requestCode == 4 && resultCode == -1) {
            int duration = Toast.LENGTH_SHORT;
            Toast toast;
            toast = Toast.makeText(this, "Uploading Images...", duration);
            toast.show();
            Log.d("intent extras", data.getExtras().toString());
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            SendImage async = new SendImage(photo);
            async.execute(); // Handle single image being sent
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

    private void selectItem(int position) {
        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        switch (position) {
            case 0:
                setTitle("Gallery");
                GalleryFragment galleryFragment = new GalleryFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.frame_layout, galleryFragment)
                        .commit();
                break;
            case 1:
                if (loggedin) {
                    setTitle("Your Account");
                    AccountFragment accountFragment = new AccountFragment("me");
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, accountFragment)
                            .commit();
                } else {
                    setTitle("Your Settings");
                    SettingsFragment settingsFragment = new SettingsFragment();
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, settingsFragment)
                            .commit();
                    updateMenu();
                }
                break;
            case 2:
                if (loggedin) {
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
                                                    AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                                                        @Override
                                                        protected Void doInBackground(Void... voids) {
                                                            MainActivity activity = MainActivity.this;
                                                            activity.uploadURL(urlText.getText().toString());
                                                            return null;
                                                        }
                                                    };
                                                    async.execute();
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
                } else
                    login();
                break;
            case 3:
                if (loggedin) {
                    setTitle("Your Images");
                    ImagesFragment imagesFragment = new ImagesFragment();
                    imagesFragment.setImageCall(null, "3/account/me/images", null);
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, imagesFragment)
                            .commit();
                    updateMenu();
                }
                break;
            case 4:
                if (loggedin) {
                    setTitle("Your Albums");
                    AlbumsFragment albumsFragment = new AlbumsFragment("me");
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, albumsFragment)
                            .commit();
                    updateMenu();
                }
                break;
            case 5:
                if (loggedin) {
                    setTitle("Your Favorites");
                    ImagesFragment imagesFragment = new ImagesFragment();
                    imagesFragment.setImageCall(null, "3/account/me/likes", null);
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, imagesFragment)
                            .commit();
                    updateMenu();
                }
                break;
            case 6:
                if (loggedin) {
                    setTitle("Your Messages");
                    MessagingFragment messagingFragment = new MessagingFragment();
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, messagingFragment)
                            .commit();
                    updateMenu();
                }
                break;
            case 7:
                if (loggedin) {
                    setTitle("Your Settings");
                    SettingsFragment settingsFragment = new SettingsFragment();
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, settingsFragment)
                            .commit();
                    updateMenu();
                }
                break;
            case 8:
                if (loggedin) {
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.remove("AccessToken");
                    editor.remove("RefreshToken");
                    editor.commit();
                    loggedin = false;
                    updateMenu();
                }
                break;
            default:
                return;
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        Log.d("new title", title.toString());
        getActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    public JSONObject uploadURL(String url) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response = Unirest.post(MASHAPE_URL + "3/image")
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .field("image", url)
                .field("type", "URL")
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            response = Unirest.post(MASHAPE_URL + "3/image")
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .field("image", url)
                    .field("type", "URL")
                    .asJson();
        }
        JSONObject data = response.getBody().getObject();
        Log.d("Got data", data.toString());
        return data;
    }

    public JSONObject makeCall(String url, String method, HashMap<String, Object> args) {
        Log.d("Call", url);
        try {
            JSONObject data = null;
            String methodString = null;
            if (url.contains("?"))
                methodString = "&_method=" + method;
            else
                methodString = "?_method=" + method;
            if (loggedin) {
                Token accessKey = getAccessToken();
                Log.d("Making Call", accessKey.toString());
                HttpResponse<JsonNode> response = Unirest.post(MASHAPE_URL + url + methodString)
                        .header("accept", "application/json")
                        .header("X-Mashape-Authorization", MASHAPE_KEY)
                        .header("Authorization", "Bearer " + accessKey.getToken())
                        .fields(args)
                        .asJson();
                Log.d("Getting Code", String.valueOf(response.getCode()));
                int code = response.getCode();
                if (code == 403) {
                    accessKey = renewAccessToken();
                    response = Unirest.post(MASHAPE_URL + url + methodString)
                            .header("accept", "application/json")
                            .header("X-Mashape-Authorization", MASHAPE_KEY)
                            .header("Authorization", "Bearer " + accessKey.getToken())
                            .fields(args)
                            .asJson();
                }
                if (code == 200) {
                    data = response.getBody().getObject();
                    Log.d("Got data", data.toString());
                }
            } else {
                HttpResponse<JsonNode> response = Unirest.post(MASHAPE_URL + url + methodString)
                        .header("accept", "application/json")
                        .header("X-Mashape-Authorization", MASHAPE_KEY)
                        .header("Authorization", "Client-ID " + CLIENTID)
                        .fields(args)
                        .asJson();
                Log.d("Getting Code", String.valueOf(response.getCode()));
                int code = response.getCode();
                if (code == 403) {
                    response = Unirest.post(MASHAPE_URL + url + methodString)
                            .header("accept", "application/json")
                            .header("X-Mashape-Authorization", MASHAPE_KEY)
                            .header("Authorization", "Client-ID " + CLIENTID)
                            .fields(args)
                            .asJson();
                }
                if (code == 200) {
                    data = response.getBody().getObject();
                    Log.d("Got data", data.toString());
                }
            }
            return data;
        } catch (Exception e) {
            Log.e("Error getting data!", e.toString());
            return null;
        }
    }

    public void changeFragment(Fragment newFragment) {
        getActionBar().show();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, newFragment).addToBackStack("tag").commit();
        updateMenu();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
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

    private class SendImage extends AsyncTask<Void, Void, JSONObject> {
        Uri uri;
        Bitmap photo;

        public SendImage(Uri _uri) {
            uri = _uri;
        }

        public SendImage(Bitmap _photo) {
            photo = _photo;
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            Token accessKey = getAccessToken();
            if (uri != null) {
                Log.d("URI", uri.toString());
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();
                final String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                Log.d("Image Upload", filePath);
                photo = BitmapFactory.decodeFile(filePath);
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            if (byteArray == null)
                Log.d("Image Upload", "NULL :(");
            String image = Base64.encodeToString(byteArray, Base64.DEFAULT);
            Log.d("Image Upload", image);
            HttpResponse<JsonNode> response = Unirest.post(MASHAPE_URL + "3/image")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .field("image", image)
                    .field("type", "binary")
                    .asJson();
            Log.d("Getting Code", String.valueOf(response.getCode()));
            JSONObject data = response.getBody().getObject();
            Log.d("Image Upload", data.toString());
            try {
                JSONObject returner = makeCall("3/image/" + data.getJSONObject("data").getString("id"), "get", null);
                Log.d("returning", returner.toString());
                return returner.getJSONObject("data");
            } catch (Exception e) {
                Log.e("Error!", e.toString());
            }
            return new JSONObject();
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            Log.d("Loading Fragment", "In Post Execute");
            SingleImageFragment singleImageFragment = new SingleImageFragment();
            singleImageFragment.setParams(jsonObject);
            singleImageFragment.setGallery(false);
            changeFragment(singleImageFragment);
        }
    }

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
}
