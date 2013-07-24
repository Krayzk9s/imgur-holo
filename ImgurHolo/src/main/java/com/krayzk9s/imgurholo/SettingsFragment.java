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
public class SettingsFragment extends Fragment{
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
            if(!settings.contains("MaxComments"))
                editor.putInt("MaxComments", 50);

            editor.commit();
            ArrayList<String> mSettingList= new ArrayList<String>();
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

        private void refreshAdapter()
        {
            MainActivity activity = (MainActivity) getActivity();
            SharedPreferences settings = activity.getSettings();
            adapter.clear();
            if(settings.getInt("MaxComments", 50) != 0)
                adapter.add("Load " + settings.getInt("MaxComments", 50) + " Comments");
            else
                adapter.add("Load All Comments");
            adapter.notifyDataSetChanged();
        }

        private void selectItem(int position) {
            switch(position) {
                case 0:
                    // 1. Instantiate an AlertDialog.Builder with its constructor
                    MainActivity activity = (MainActivity) getActivity();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    // 2. Chain together various setter methods to set the dialog characteristics
                    new AlertDialog.Builder(activity).setTitle("Set Maximum Comments to Load")
                            .setItems(R.array.commentCounts, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    MainActivity activity = (MainActivity) getActivity();
                                    SharedPreferences settings = activity.getSettings();
                                    SharedPreferences.Editor editor = settings.edit();
                                    int commentNumber = 0;
                                    switch (whichButton) {
                                        case 0:
                                            commentNumber = 50;
                                            break;
                                        case 1:
                                            commentNumber = 100;
                                            break;
                                        case 2:
                                            commentNumber = 200;
                                            break;
                                        default:
                                            break;
                                    }
                                    editor.putInt("MaxComments", commentNumber);
                                    editor.commit();
                                    refreshAdapter();
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                        }
                    }).show();

                    // 3. Get the AlertDialog from create()
                    AlertDialog dialog = builder.create();
                    break;
                default:
                    break;
            }
        }
}
