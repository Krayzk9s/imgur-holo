package com.krayzk9s.imgurholo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.scribe.model.Token;

/**
 * Created by Kurt Zimmer on 7/24/13.
 */
public class SettingsFragment extends Fragment{
        public static final String MASHAPE_URL = "https://imgur-apiv3.p.mashape.com/";
        public static final String MASHAPE_KEY = "CoV9d8oMmqhy8YdAbCAnB1MroW1xMJpP";
        String[] mSettingList;
        ArrayAdapter<String> adapter;
        private Token accessKey;

        public SettingsFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View view = inflater.inflate(R.layout.account_layout, container, false);
            ListView mDrawerList = (ListView) view.findViewById(R.id.account_list);
            mSettingList = getResources().getStringArray(R.array.settings);
            adapter = new ArrayAdapter<String>(view.getContext(),
                    R.layout.drawer_list_item, mSettingList);
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

        private void selectItem(int position) {

        }
}
