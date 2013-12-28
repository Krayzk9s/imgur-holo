package com.krayzk9s.imgurholo.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.services.UploadService;
import com.krayzk9s.imgurholo.tools.ApiCall;
import com.krayzk9s.imgurholo.tools.Fetcher;
import com.krayzk9s.imgurholo.tools.GetData;
import com.krayzk9s.imgurholo.ui.ImagesFragment;
import com.krayzk9s.imgurholo.ui.SingleImageFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Copyright 2013 Kurt Zimmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ImgurLinkActivity extends ImgurHoloActivity implements GetData {

    private final static String IMAGE = "image";
    private final static String ALBUM = "album";
    private String album;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Log.d("New Intent", intent.toString());
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                Toast.makeText(this, R.string.toast_uploading, Toast.LENGTH_SHORT).show();
                Intent serviceIntent = new Intent(this, UploadService.class);
                if(intent.getExtras() == null)
                    finish();
                serviceIntent.setData((Uri) intent.getExtras().get("android.intent.extra.STREAM"));
                startService(serviceIntent);
                finish();
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            Log.d("sending", "sending multiple");
            Toast.makeText(this, R.string.toast_uploading, Toast.LENGTH_SHORT).show();
            ArrayList<Parcelable> list =
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            Intent serviceIntent = new Intent(this, UploadService.class);
            serviceIntent.putParcelableArrayListExtra("images", list);
            startService(serviceIntent);
            finish();
        } else if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null && intent.getData().toString().startsWith("http://imgur.com/a")) {
            String uri = intent.getData().toString();
            album = uri.split("/")[4];
            Log.d("album", album);
            Fetcher fetcher = new Fetcher(this, "/3/album/" + album, ApiCall.GET, null, apiCall, ALBUM);
            fetcher.execute();
        }  else if (Intent.ACTION_VIEW.equals(action) && intent.getData().toString().startsWith("http://imgur.com/gallery/")) {
            String uri = intent.getData().toString();
            final String album = uri.split("/")[4];
            if(album.length() == 5) {
                Log.d("album", album);
                Fetcher fetcher = new Fetcher(this, "/3/album/" + album, ApiCall.GET, null, apiCall, ALBUM);
                fetcher.execute();
            }
            else if(album.length() == 7) {
                Log.d("image", album);
                Fetcher fetcher = new Fetcher(this, "/3/image/" + album, ApiCall.GET, null, apiCall, IMAGE);
                fetcher.execute();
            }
        } else if (Intent.ACTION_VIEW.equals(action) && intent.getData().toString().startsWith("http://i.imgur")) {
            String uri = intent.getData().toString();
            final String image = uri.split("/")[3].split("\\.")[0];
            Log.d("image", image);
            Fetcher fetcher = new Fetcher(this, "/3/image/" + image, ApiCall.GET, null, apiCall, IMAGE);
            fetcher.execute();
        }
    }
    public void onGetObject(Object o, String tag) {
        if(tag.equals(IMAGE)) {
            try {
                JSONObject singleImageData = (JSONObject) o;
                Log.d("data", singleImageData.toString());
                SingleImageFragment singleImageFragment = new SingleImageFragment();
                Bundle bundle = new Bundle();
                bundle.putBoolean("gallery", true);
                JSONParcelable data = new JSONParcelable();
                data.setJSONObject(singleImageData.getJSONObject("data"));
                bundle.putParcelable("imageData", data);
                singleImageFragment.setArguments(bundle);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.frame_layout, singleImageFragment).commit();
            } catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
        }
        else if (tag.equals(ALBUM)) {
            try {
                JSONObject albumData = (JSONObject) o;
                Log.d("data", albumData.toString());
                ImagesFragment fragment = new ImagesFragment();
                Bundle bundle = new Bundle();
                bundle.putString("imageCall", "/3/album/" + album);
                bundle.putString("id", album);
                JSONParcelable data = new JSONParcelable();
                data.setJSONObject(albumData.getJSONObject("data"));
                bundle.putParcelable("albumData", data);
                fragment.setArguments(bundle);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.frame_layout, fragment).commit();
            } catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
        }
    }

    public void handleException(Exception e, String tag) {

    }

    public ApiCall getApiCall() {
        return apiCall;
    }
}
