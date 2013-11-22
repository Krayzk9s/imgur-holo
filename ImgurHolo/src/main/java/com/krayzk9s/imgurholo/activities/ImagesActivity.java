package com.krayzk9s.imgurholo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.ui.ImagesFragment;

/**
 * Created by Kurt Zimmer on 11/22/13.
 */
public class ImagesActivity extends ImgurHoloActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
        if(savedInstanceState == null) {
            Intent intent = getIntent();
            Bundle bundle = intent.getExtras();
            ImagesFragment imagesFragment = new ImagesFragment();
            imagesFragment.setArguments(bundle);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.frame_layout, imagesFragment).commit();
        }
    }
}
