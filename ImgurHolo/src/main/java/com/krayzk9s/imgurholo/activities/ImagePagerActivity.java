package com.krayzk9s.imgurholo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.ui.ImagePager;

/**
 * Created by Kurt Zimmer on 11/20/13.
 */
public class ImagePagerActivity extends ImgurHoloActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
        if(savedInstanceState == null) {
        Intent intent = getIntent();
            Bundle bundle = new Bundle();
            bundle.putInt("start", intent.getExtras().getInt("start"));
            bundle.putParcelableArrayList("ids", intent.getExtras().getParcelableArrayList("ids"));
            ImagePager imagePager = new ImagePager();
            imagePager.setArguments(bundle);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.frame_layout, imagePager).commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActionBar().setTitle("Images");
    }
}
