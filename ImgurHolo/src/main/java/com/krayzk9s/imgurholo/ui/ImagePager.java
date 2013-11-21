package com.krayzk9s.imgurholo.ui;

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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.libs.JSONParcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Kurt Zimmer on 7/30/13.
 */
public class ImagePager extends Fragment {
    public ViewPager pager;
    public ImageAdapter adapter;
    ArrayList<JSONParcelable> imageData;
    int start;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            imageData = savedInstanceState.getParcelableArrayList("imageData");
            start = savedInstanceState.getInt("start");
        }
        else {
            Bundle bundle = getArguments();
            start = bundle.getInt("start");
            imageData = bundle.getParcelableArrayList("ids");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getActivity().getActionBar() != null)
            getActivity().getActionBar().setTitle("Images");
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View result=inflater.inflate(R.layout.image_viewpager, container, false);
        pager=(ViewPager)result.findViewById(R.id.pager);
        adapter = new ImageAdapter(getActivity(), getChildFragmentManager());
        pager.setAdapter(adapter);
        if(savedInstanceState == null)
            pager.setCurrentItem(start);
        return(result);
    }

    public class ImageAdapter extends android.support.v4.app.FragmentPagerAdapter {

        Context ctxt=null;

        public ImageAdapter(Context ctxt, FragmentManager mgr) {
            super(mgr);
            this.ctxt=ctxt;
        }

        @Override
        public int getCount() {
            return imageData.size();
        }

        @Override
        public Fragment getItem(int position) {
            JSONObject id = imageData.get(position).getJSONObject();
            try {
            if (id.has("is_album") && id.getBoolean("is_album")) {
                ImagesFragment fragment = new ImagesFragment();
                Bundle bundle = new Bundle();
                bundle.putString("imageCall", "3/album/" + id.getString("id"));
                bundle.putString("id", id.getString("id"));
                JSONParcelable data = new JSONParcelable();
                data.setJSONObject(id);
                bundle.putParcelable("albumData", data);
                fragment.setArguments(bundle);
                return fragment;
            } else {
                SingleImageFragment singleImageFragment = new SingleImageFragment();
                Bundle bundle = new Bundle();
                bundle.putBoolean("gallery", true);
                JSONParcelable data = new JSONParcelable();
                data.setJSONObject(id);
                bundle.putParcelable("imageData", data);
                singleImageFragment.setArguments(bundle);
                return singleImageFragment;
             }
            } catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
            return null;
        }
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelableArrayList("imageData", imageData);
        savedInstanceState.putInt("start", pager.getCurrentItem());
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
}