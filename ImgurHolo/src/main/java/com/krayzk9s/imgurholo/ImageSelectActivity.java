package com.krayzk9s.imgurholo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;

public class ImageSelectActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_select_layout);
        FragmentManager fragmentManager = getSupportFragmentManager();
        ImagesFragment imagesFragment = new ImagesFragment();
        imagesFragment.selecting = true;
        imagesFragment.setImageCall(null, "3/account/me/images/0", null);
        fragmentManager.beginTransaction()
                .replace(R.id.frame_layout, imagesFragment)
                .commit();

    }

    @Override
    public void onNewIntent(Intent intent) {

    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return true;

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.image_select, menu);
        return true;
    }
    
}
