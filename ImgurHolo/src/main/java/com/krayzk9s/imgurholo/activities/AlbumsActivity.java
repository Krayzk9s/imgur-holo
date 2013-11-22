package com.krayzk9s.imgurholo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.ui.AlbumsFragment;

/**
 * Created by Kurt Zimmer on 11/22/13.
 */
public class AlbumsActivity extends ImgurHoloActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
        if(savedInstanceState == null) {
            Intent intent = getIntent();
            Bundle bundle = intent.getExtras();
            AlbumsFragment albumsFragment = new AlbumsFragment();
            albumsFragment.setArguments(bundle);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.frame_layout, albumsFragment).commit();
        }
    }
}
