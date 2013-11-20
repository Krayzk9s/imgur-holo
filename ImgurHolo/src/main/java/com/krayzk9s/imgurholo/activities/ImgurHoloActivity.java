package com.krayzk9s.imgurholo.activities;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.tools.ApiCall;

import java.lang.reflect.Field;

/**
 * Created by Kurt Zimmer on 11/20/13.
 */
public class ImgurHoloActivity extends FragmentActivity {
    protected ApiCall apiCall;
    protected String theme;
    protected ActionBarDrawerToggle mDrawerToggle;
    protected CharSequence mDrawerTitle;
    protected CharSequence mTitle;
    protected DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;
    public static String IMAGE_PAGER_INTENT = "com.krayzk9s.imgurholo.IMAGE_PAGER";
    public static String HOLO_DARK = "Holo Dark";
    public static String HOLO_LIGHT = "Holo Light";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (NoSuchFieldException e) {
            // Ignore
            Log.e("Error!", e.toString());
        } catch (IllegalAccessException e) {
            // Ignore
            Log.e("Error!", e.toString());
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        apiCall = new ApiCall();
        if(Integer.parseInt(settings.getString("IconSize", "120")) < 120) { //getting rid of 90 because it may crash the app for large screens
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("IconSize", "120");
            editor.commit();
        }
        theme = settings.getString("theme", MainActivity.HOLO_LIGHT);
        if (theme.equals(MainActivity.HOLO_LIGHT))
            setTheme(R.style.AppTheme);
        else
            setTheme(R.style.AppThemeDark);
        setContentView(R.layout.activity_main);
        apiCall.setSettings(settings);
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
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                getActionBar().show();
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    public ApiCall getApiCall() {
        return apiCall;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //finish();
                Intent intent = NavUtils.getParentActivityIntent(this);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                NavUtils.navigateUpTo(this, intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
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

    /* The click listner for ListView in the navigation drawer */
    protected class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    protected void selectItem(int position) {
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
}
