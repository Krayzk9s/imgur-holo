package com.krayzk9s.imgurholo.activities;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewConfiguration;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.tools.ApiCall;

import java.lang.reflect.Field;

/**
 * Created by Kurt Zimmer on 11/20/13.
 */
public class ImgurHoloActivity extends FragmentActivity {
    protected ApiCall apiCall;
    protected String theme;
    public static String IMAGE_PAGER_INTENT = "com.krayzk9s.imgurholo.IMAGE_PAGER";
    public static String IMAGES_INTENT = "com.krayzk9s.imgurholo.IMAGES";
    public static String COMMENTS_INTENT = "com.krayzk9s.imgurholo.COMMENTS";
    public static String ALBUMS_INTENT = "com.krayzk9s.imgurholo.ALBUMS";
    public static String ACCOUNT_INTENT = "com.krayzk9s.imgurholo.ACCOUNT";
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
        setContentView(R.layout.activity_other);
        apiCall.setSettings(settings);
        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    public ApiCall getApiCall() {
        return apiCall;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                /*
                Intent intent = NavUtils.getParentActivityIntent(this);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                NavUtils.navigateUpTo(this, intent);*/
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
