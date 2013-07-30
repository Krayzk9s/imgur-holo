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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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

public class MainActivity extends FragmentActivity {

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    public static int HOLO_DARK = 0;
    public static int HOLO_LIGHT = 1;
    public int theme;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    String consumerkey = "4cd3f96f162ac80";
    String secretkey = "9cd3c621a4e064422e60aba4ccf84d6b149b4463";

    private static final Token EMPTY_TOKEN = null;
    public static final String OAUTH_CALLBACK_SCHEME = "imgur-holo";
    public static final String OAUTH_CALLBACK_HOST = "authcallback";
    public static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME + "://" + OAUTH_CALLBACK_HOST;
    public static final String MASHAPE_KEY = "CoV9d8oMmqhy8YdAbCAnB1MroW1xMJpP";
    public static final String PREFS_NAME = "ImgurPrefs";
    public static final String MASHAPE_URL = "https://imgur-apiv3.p.mashape.com/";
    Token accessToken;
    String accessString;
    Verifier verifier;
    Adapter adapter;
    final OAuthService service = new ServiceBuilder().provider(ImgUr3Api.class).apiKey(consumerkey).debug().callback(OAUTH_CALLBACK_URL).apiSecret(secretkey).build();

    boolean loggedin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if(settings.contains("theme")) {
            theme = settings.getInt("theme", HOLO_LIGHT);
        }
        else
            theme = HOLO_LIGHT;

        if(theme == HOLO_LIGHT)
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
            drawerAdapter.setMenu(R.array.imgurMenuListLoggedOut, R.array.imgurMenuListDarkIcons);
        else if (loggedin && theme == HOLO_LIGHT)
            drawerAdapter.setMenu(R.array.imgurMenuListLoggedIn, R.array.imgurMenuListIcons);
        else
            drawerAdapter.setMenu(R.array.imgurMenuListLoggedOut, R.array.imgurMenuListIcons);
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


    private class SendImage extends AsyncTask<Void, Void, Void> {
        Uri uri;
        Bitmap photo;

        public SendImage(Uri _uri) {
            uri = _uri;
        }

        public SendImage(Bitmap _photo) {
            photo = _photo;
        }

        @Override
        protected Void doInBackground(Void... voids) {
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
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
        }
    }


    private void loadDefaultPage() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (!loggedin)
            return;
        else if (!settings.contains("DefaultPage") || settings.getString("DefaultPage", "").equals("Gallery")) {
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
            imagesFragment.setImageCall(null, "3/account/me/images/0", null);
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
                SendImage async = new SendImage((Uri) intent.getExtras().get("android.intent.extra.STREAM"));
                async.execute();
                finish();
            }
        }

        else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            Log.d("sending", "sending multiple");
            ArrayList<Parcelable> list =
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                for (Parcelable parcel : list) {
                    Uri uri = (Uri) parcel;
                    Log.d("sending", uri.toString());
                    SendImage async = new SendImage(uri);
                    async.execute();
                /// do things here with each image source path.
            }
            finish();
        }
        else if(Intent.ACTION_VIEW.equals(action) && intent.getData().toString().startsWith("http"))
        {
            String uri = intent.getData().toString();
            final String image = uri.split("/")[3].split("\\.")[0];
            Log.d("image", image);
            AsyncTask<Void, Void, JSONObject> async = new AsyncTask<Void, Void, JSONObject>() {
                @Override
                protected JSONObject doInBackground(Void... voids) {
                    JSONObject imageData = makeGetCall("/3/image/" + image);
                    return imageData;
                }
                @Override
                protected void onPostExecute(JSONObject imageData) {
                    Log.d("data", imageData.toString());
                    try {
                    SingleImageFragment singleImageFragment = new SingleImageFragment();
                    singleImageFragment.setParams(imageData.getJSONObject("data"));
                    changeFragment(singleImageFragment);
                    }
                    catch (Exception e) {
                        Log.e("Error!", e.toString());
                    }
                }
            };
            async.execute();

        }
        else {
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
        if(theme == HOLO_LIGHT)
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
        if(resultCode == -1)
            Log.d("intent", data.toString());
        if (requestCode == 3 && resultCode == -1) {
            SendImage async = new SendImage((Uri) data.getData());
            async.execute(); // Handle single image being sent
            return;
        }
        if (requestCode == 4 && resultCode == -1) {
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

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
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
                if (!loggedin)
                    login();
                else {
                    setTitle("Gallery");
                    GalleryFragment galleryFragment = new GalleryFragment();
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, galleryFragment)
                            .commit();
                }
                break;
            case 1:
                if (loggedin) {
                    setTitle("Your Account");
                    AccountFragment accountFragment = new AccountFragment("me");
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, accountFragment)
                            .commit();
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
                }
                break;
            case 3:
                if (loggedin) {
                    setTitle("Your Images");
                    ImagesFragment imagesFragment = new ImagesFragment();
                    imagesFragment.setImageCall(null, "3/account/me/images/0", null);
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

    public JSONObject makeGetCall(String url) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response = Unirest.get(MASHAPE_URL + url)
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            response = Unirest.get(MASHAPE_URL + url)
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .asJson();
        }
        JSONObject data = response.getBody().getObject();
        Log.d("Got data", data.toString());
        return data;
    }

    public JSONObject makePostCall(String url) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response = Unirest.post(MASHAPE_URL + url)
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            response = Unirest.post(MASHAPE_URL + url)
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .asJson();
        }
        JSONObject data = response.getBody().getObject();
        Log.d("Got data", data.toString());
        return data;
    }

    public JSONObject makeGalleryReply(String imageId, String comment, String commentId) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response;
        if (commentId != null) {
            response = Unirest.post(MASHAPE_URL + "3/comment/")
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .field("comment", comment)
                    .field("image_id", imageId)
                    .field("parent_id", commentId)
                    .asJson();
        } else {
            response = Unirest.post(MASHAPE_URL + "3/comment/")
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .field("comment", comment)
                    .field("image_id", imageId)
                    .asJson();
        }
        Log.d("Getting Code", String.valueOf(response.getCode()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            if (commentId != null) {
                Unirest.post(MASHAPE_URL + "3/comment/")
                        .header("accept", "application/json")
                        .header("X-Mashape-Authorization", MASHAPE_KEY)
                        .header("Authorization", "Bearer " + accessKey.getToken())
                        .field("comment", comment)
                        .field("image_id", imageId)
                        .field("parent_id", commentId)
                        .asJson();
            } else {
                Unirest.post(MASHAPE_URL + "3/comment/")
                        .header("accept", "application/json")
                        .header("X-Mashape-Authorization", MASHAPE_KEY)
                        .header("Authorization", "Bearer " + accessKey.getToken())
                        .field("comment", comment)
                        .field("image_id", imageId)
                        .asJson();
            }
        }
        JSONObject data = response.getBody().getObject();
        Log.d("Got data", data.toString());
        return data;
    }

    public void makeSettingsPost(String accountSetting, Object settingValue, String username) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response = Unirest.post(MASHAPE_URL + "/3/account/me/settings")
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .field(accountSetting, settingValue)
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        Log.d("Response", String.valueOf(response.getBody().getObject().toString()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            Unirest.post(MASHAPE_URL + "/3/account/me/settings")
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .field(accountSetting, settingValue)
                    .asJson();
        }
    }

    public void makeNewAlbum(String title, String description) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response = Unirest.post(MASHAPE_URL + "3/album/")
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .field("title", title)
                .field("description", description)
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        Log.d("Response", String.valueOf(response.getBody().getObject().toString()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            Unirest.post(MASHAPE_URL + "3/album/")
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .field("title", title)
                    .field("description", description)
                    .asJson();
        }
    }

    public void editAlbum(String ids, String id) {
        Log.d("Editing Album", id + " " + ids);
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response;
        response = Unirest.post(MASHAPE_URL + "3/album/" + id)
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .field("ids", ids)
                .field("id", id)
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        Log.d("Response", String.valueOf(response.getBody().getObject().toString()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            Unirest.post(MASHAPE_URL + "3/album/" + id)
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .field("ids", ids)
                    .field("id", id)
                    .asJson();
        }
    }

    public void editImage(String id, String title, String description) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response;
        response = Unirest.post(MASHAPE_URL + "3/image/" + id)
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .field("title", title)
                .field("description", description)
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        Log.d("Response", String.valueOf(response.getBody().getObject().toString()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            Unirest.post(MASHAPE_URL + "3/image/" + id)
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .field("title", title)
                    .field("description", description)
                    .asJson();
        }
    }

    public void deleteImage(String deletehash) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response;
        response = Unirest.delete(MASHAPE_URL + "3/image/" + deletehash)
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        Log.d("Response", String.valueOf(response.getBody().getObject().toString()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            Unirest.delete(MASHAPE_URL + "3/image/" + deletehash)
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .asJson();
        }
    }

    public void deleteImages(ArrayList<String> deleteImages) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response;
        for (int i = 0; i < deleteImages.size(); i++) {
            response = Unirest.get(MASHAPE_URL + "3/image/" + deleteImages.get(i))
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .asJson();
            Log.d("Getting Code", String.valueOf(response.getCode()));
            Log.d("Response", String.valueOf(response.getBody().getObject().toString()));
            int code = response.getCode();
            if (code == 403) {
                accessKey = renewAccessToken();
                Unirest.get(MASHAPE_URL + "3/image/" + deleteImages.get(i))
                        .header("accept", "application/json")
                        .header("X-Mashape-Authorization", MASHAPE_KEY)
                        .header("Authorization", "Bearer " + accessKey.getToken())
                        .asJson();
            }
            try {
                String deletehash = response.getBody().getObject().getJSONObject("data").getString("deletehash");
                response = Unirest.delete(MASHAPE_URL + "3/image/" + deletehash)
                        .header("accept", "application/json")
                        .header("X-Mashape-Authorization", MASHAPE_KEY)
                        .header("Authorization", "Bearer " + accessKey.getToken())
                        .asJson();
                Log.d("Getting Code", String.valueOf(response.getCode()));
                Log.d("Response", String.valueOf(response.getBody().getObject().toString()));
                code = response.getCode();
                if (code == 403) {
                    accessKey = renewAccessToken();
                    Unirest.delete(MASHAPE_URL + "3/image/" + deletehash)
                            .header("accept", "application/json")
                            .header("X-Mashape-Authorization", MASHAPE_KEY)
                            .header("Authorization", "Bearer " + accessKey.getToken())
                            .asJson();
                }
            } catch (Exception e) {
                Log.e("Error!", e.toString());
            }
        }
    }

    public void makeMessagePost(String header, String body, String username) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response = Unirest.post(MASHAPE_URL + "/3/message")
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .field("subject", header)
                .field("body", body)
                .field("recipient", username)
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        Log.d("Response", String.valueOf(response.getBody().getObject().toString()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            Unirest.post(MASHAPE_URL + "/3/message")
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .field("subject", header)
                    .field("body", body)
                    .field("recipient", username)
                    .asJson();
        }
    }

    public void deleteComment(String commentId) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response = Unirest.delete(MASHAPE_URL + "/3/comment/" + commentId)
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        Log.d("Response", String.valueOf(response.getBody().getObject().toString()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            Unirest.delete(MASHAPE_URL + "/3/comment/" + commentId)
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .asJson();
        }
    }

    public void reportPost(String username) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response = Unirest.post(MASHAPE_URL + "3/message/report/" + username)
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        Log.d("Response", String.valueOf(response.getBody().getObject().toString()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            Unirest.post(MASHAPE_URL + "3/message/report/" + username)
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .asJson();
        }
        Unirest.post(MASHAPE_URL + "3/message/block/" + username)
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .asJson();
    }

    public void deletePost(String id) {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response = Unirest.delete(MASHAPE_URL + "3/message/" + id)
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        Log.d("Response", String.valueOf(response.getBody().getObject().toString()));
        int code = response.getCode();
        if (code == 403) {
            accessKey = renewAccessToken();
            Unirest.delete(MASHAPE_URL + "3/message/" + id)
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .asJson();
        }
    }


    public void changeFragment(Fragment newFragment) {
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
}
