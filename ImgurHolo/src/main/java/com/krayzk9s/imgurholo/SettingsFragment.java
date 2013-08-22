package com.krayzk9s.imgurholo;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.ArrayAdapter;

/**
 * Created by Kurt Zimmer on 7/24/13.
 */
public class SettingsFragment extends PreferenceFragment {
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
        if(!autoCopy.isChecked())
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
                if(Boolean.parseBoolean(o.toString()))
                    autoCopyType.setEnabled(true);
                else
                    autoCopyType.setEnabled(false);
                return true;
            }
        });
        final CheckBoxPreference showVotes = (CheckBoxPreference) findPreference("ShowVotes");
        CheckBoxPreference showComments = (CheckBoxPreference) findPreference("ShowComments");
        if(!showComments.isChecked()) {
            showVotes.setEnabled(false);
            commentSort.setEnabled(false);
        }
        showComments.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(Boolean.parseBoolean(o.toString())) {
                    showVotes.setEnabled(true);
                    commentSort.setEnabled(true);
                }
                else {
                    showVotes.setEnabled(false);
                    commentSort.setEnabled(false);
                }
                return true;
            }
        });
    }
}
