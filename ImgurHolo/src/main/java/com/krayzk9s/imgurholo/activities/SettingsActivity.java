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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.krayzk9s.imgurholo.R;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Kurt Zimmer on 7/24/13.
 */
public class SettingsActivity extends Activity {
    public static String HOLO_DARK = "Holo Dark";
    public static String HOLO_LIGHT = "Holo Light";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String theme = settings.getString("theme", HOLO_LIGHT);
        if (theme.equals(HOLO_LIGHT))
            setTheme(R.style.AppTheme);
        else
            setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return(true);
        }

        return(super.onOptionsItemSelected(item));
    }

    public static class SettingsFragment extends PreferenceFragment {
        ArrayAdapter<String> adapter;

        public SettingsFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            ListPreference defaultPage = (ListPreference) findPreference("DefaultPage");
            defaultPage.setSummary(defaultPage.getValue().toString());
            defaultPage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preference.setSummary(o.toString());
                    return true;
                }
            });
            ListPreference theme = (ListPreference) findPreference("theme");
            theme.setSummary(theme.getValue().toString());
            theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preference.setSummary(o.toString());
                    return true;
                }
            });
            ListPreference iconSize = (ListPreference) findPreference("IconSize");
            iconSize.setSummary(iconSize.getValue().toString());
            iconSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preference.setSummary(o.toString());
                    return true;
                }
            });
            ListPreference galleryDefault = (ListPreference) findPreference("DefaultGallery");
            galleryDefault.setSummary(galleryDefault.getValue().toString());
            galleryDefault.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preference.setSummary(o.toString());
                    return true;
                }
            });

            ListPreference iconQuality = (ListPreference) findPreference("IconQuality");
            final ArrayList<String> optionSettings = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.imageQualitiesSettings)));
            final ArrayList<String> options = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.imageQualities)));
            int i = optionSettings.indexOf(iconQuality.getValue().toString());
            if (i > 0)
                iconQuality.setSummary(options.get(i));
            iconQuality.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    int i = optionSettings.indexOf(o.toString());
                    preference.setSummary(options.get(i));
                    return true;
                }
            });

            final ListPreference commentSort = (ListPreference) findPreference("CommentSort");
            commentSort.setSummary(commentSort.getValue().toString());
            commentSort.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preference.setSummary(o.toString());
                    return true;
                }
            });

            CheckBoxPreference autoCopy = (CheckBoxPreference) findPreference("AutoCopy");
            final ListPreference autoCopyType = (ListPreference) findPreference("AutoCopyType");
            autoCopyType.setSummary(autoCopyType.getValue().toString());
            if (!autoCopy.isChecked())
                autoCopyType.setEnabled(false);
            autoCopyType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preference.setSummary(o.toString());
                    return true;
                }
            });
            autoCopy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (Boolean.parseBoolean(o.toString()))
                        autoCopyType.setEnabled(true);
                    else
                        autoCopyType.setEnabled(false);
                    return true;
                }
            });

            CheckBoxPreference widthBoolean = (CheckBoxPreference) findPreference("WidthBoolean");
            final EditTextPreference widthSize = (EditTextPreference) findPreference("WidthSize");
            widthSize.setSummary(widthSize.getText());
            if (!widthBoolean.isChecked())
                widthSize.setEnabled(false);
            widthSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    try
                    {
                        Integer.parseInt(o.toString());
                    }
                    catch(NumberFormatException nfe)
                    {
                        return false;
                    }
                    if(Integer.parseInt(o.toString()) > 10 && Integer.parseInt(o.toString()) <= 1920)
                        preference.setSummary(o.toString());
                    return true;
                }
            });
            widthBoolean.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (Boolean.parseBoolean(o.toString()))
                        widthSize.setEnabled(true);
                    else
                        widthSize.setEnabled(false);
                    return true;
                }
            });

            CheckBoxPreference heightBoolean = (CheckBoxPreference) findPreference("HeightBoolean");
            final EditTextPreference heightSize = (EditTextPreference) findPreference("HeightSize");
            heightSize.setSummary(heightSize.getText());
            if (!heightBoolean.isChecked())
                heightSize.setEnabled(false);
            heightSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    try
                    {
                         Integer.parseInt(o.toString());
                    }
                    catch(NumberFormatException nfe)
                    {
                        return false;
                    }
                    if(Integer.parseInt(o.toString()) > 10 && Integer.parseInt(o.toString()) <= 1080)
                        preference.setSummary(o.toString());
                    return true;
                }
            });
            heightBoolean.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (Boolean.parseBoolean(o.toString()))
                        heightSize.setEnabled(true);
                    else
                        heightSize.setEnabled(false);
                    return true;
                }
            });
            
            final CheckBoxPreference showVotes = (CheckBoxPreference) findPreference("ShowVotes");
            CheckBoxPreference showComments = (CheckBoxPreference) findPreference("ShowComments");
            if (!showComments.isChecked()) {
                showVotes.setEnabled(false);
                commentSort.setEnabled(false);
            }
            showComments.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (Boolean.parseBoolean(o.toString())) {
                        showVotes.setEnabled(true);
                        commentSort.setEnabled(true);
                    } else {
                        showVotes.setEnabled(false);
                        commentSort.setEnabled(false);
                    }
                    return true;
                }
            });

            Preference googlePlus = findPreference("GooglePlus");
            googlePlus.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/communities/115412944377397978285"));
                    startActivity(Intent.createChooser(intent, "Go to Google Plus"));
                    return true;
                }
            });
            Preference email = findPreference("Email");
            email.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/html");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"imgurholo@gmail.com"});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "imgur Holo Feedback");
                    startActivity(Intent.createChooser(intent, "Send Email"));
                    return true;
                }
            });
            Preference reddit = findPreference("Reddit");
            reddit.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("http://www.reddit.com/r/imgurholo/"));
                    startActivity(Intent.createChooser(intent, "Go to Subreddit"));
                    return true;
                }
            });
        }
    }
}
