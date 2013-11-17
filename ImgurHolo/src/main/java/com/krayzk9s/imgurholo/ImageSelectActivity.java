package com.krayzk9s.imgurholo;

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
        Bundle bundle = new Bundle();
        bundle.putString("imageCall", "3/account/me/images/0");
        imagesFragment.setArguments(bundle);
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
