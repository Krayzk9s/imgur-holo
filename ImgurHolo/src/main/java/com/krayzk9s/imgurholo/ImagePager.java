package com.krayzk9s.imgurholo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    public ImagePager(int _start) {
        start = _start;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        if(savedInstanceState != null)
            imageData = savedInstanceState.getParcelableArrayList("imageData");
        View result=inflater.inflate(R.layout.image_viewpager, container, false);
        pager=(ViewPager)result.findViewById(R.id.pager);
        adapter = new ImageAdapter(getActivity(), getChildFragmentManager());
        pager.setAdapter(adapter);
        pager.setCurrentItem(start);

        return(result);
    }

    public void setImageData(ArrayList<JSONParcelable> _ids) {
        imageData = _ids;
    }


    public class ImageAdapter extends FragmentPagerAdapter {

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
                fragment.setImageCall(id.getString("id"), "3/album/" + id.getString("id"), id);
                return fragment;
            } else {
                SingleImageFragment singleImageFragment = new SingleImageFragment();
                singleImageFragment.setGallery(true);
                singleImageFragment.setParams(id);
                return singleImageFragment;
             }
            } catch (Exception e) {
                Log.e("Error!", e.toString());
            }
            return null;
        }
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelableArrayList("imageData", imageData);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
}