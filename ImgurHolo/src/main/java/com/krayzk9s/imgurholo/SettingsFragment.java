package com.krayzk9s.imgurholo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by Kurt Zimmer on 7/24/13.
 */
public class SettingsFragment extends Fragment {
    ArrayAdapter<String> adapter;

    public SettingsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.account_layout, container, false);
        ListView mDrawerList = (ListView) view.findViewById(R.id.account_list);
        MainActivity activity = (MainActivity) getActivity();
        SharedPreferences settings = activity.getSettings();
        SharedPreferences.Editor editor = settings.edit();
        if (!settings.contains("MaxComments"))
            editor.putInt("MaxComments", 50);
        if (!settings.contains("DefaultPage"))
            editor.putString("DefaultPage", "");

        editor.commit();
        ArrayList<String> mSettingList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(view.getContext(),
                R.layout.drawer_list_item, mSettingList);


        refreshAdapter();
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        return view;
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void refreshAdapter() {
        MainActivity activity = (MainActivity) getActivity();
        SharedPreferences settings = activity.getSettings();
        adapter.clear();
        if (!settings.getString("DefaultPage", "").equals(""))
            adapter.add("Default Page is " + settings.getString("DefaultPage", ""));
        else
            adapter.add("No Default Page");
        if (settings.getInt("theme", activity.HOLO_LIGHT) == activity.HOLO_LIGHT)
            adapter.add("Theme is Holo Light");
        else
            adapter.add("Theme is Holo Dark");
        adapter.notifyDataSetChanged();
    }

    private void selectItem(int position) {
        MainActivity activity = (MainActivity) getActivity();
        switch (position) {
            case 0:
                new AlertDialog.Builder(activity).setTitle("Set Page as Default")
                        .setItems(R.array.defaultArray, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                MainActivity activity = (MainActivity) getActivity();
                                SharedPreferences settings = activity.getSettings();
                                SharedPreferences.Editor editor = settings.edit();
                                String defaultPage = "";
                                switch (whichButton) {
                                    case 0:
                                        defaultPage = "Gallery";
                                        break;
                                    case 1:
                                        defaultPage = "Your Albums";
                                        break;
                                    case 2:
                                        defaultPage = "Your Images";
                                        break;
                                    case 3:
                                        defaultPage = "Your Favorites";
                                        break;
                                    case 4:
                                        defaultPage = "Your Account";
                                        break;
                                    default:
                                        break;
                                }
                                editor.putString("DefaultPage", defaultPage);
                                editor.commit();
                                refreshAdapter();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                break;
            case 1:
                new AlertDialog.Builder(activity).setTitle("Set Theme")
                        .setItems(R.array.themes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                MainActivity activity = (MainActivity) getActivity();
                                SharedPreferences settings = activity.getSettings();
                                SharedPreferences.Editor editor = settings.edit();
                                switch (whichButton) {
                                    case 0:
                                        activity.theme = activity.HOLO_LIGHT;
                                        break;
                                    case 1:
                                        activity.theme = activity.HOLO_DARK;
                                        break;
                                    default:
                                        break;
                                }
                                editor.putInt("theme", activity.theme);
                                editor.commit();
                                refreshAdapter();
                                new AlertDialog.Builder(activity).setTitle("Set Theme").setMessage("Restart app for changes to take effect").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        //nothing
                                    }
                                }).show();
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
}
