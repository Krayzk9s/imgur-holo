package com.krayzk9s.imgurholo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.ui.ImagesFragment;
import com.krayzk9s.imgurholo.ui.SingleImageFragment;

import org.json.JSONException;
import org.json.JSONObject;

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
public class ImageActivity extends ImgurHoloActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getActionBar() != null)
			getActionBar().setDisplayHomeAsUpEnabled(true);
		if(savedInstanceState == null) {
			Intent intent = getIntent();
			JSONObject id = ((JSONParcelable) intent.getExtras().getParcelable("id")).getJSONObject();
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
					FragmentManager fragmentManager = getSupportFragmentManager();
					FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
					fragmentTransaction.replace(R.id.frame_layout, fragment).commitAllowingStateLoss();
				} else {
					SingleImageFragment singleImageFragment = new SingleImageFragment();
					Bundle bundle = new Bundle();
					bundle.putBoolean("gallery", true);
					JSONParcelable data = new JSONParcelable();
					data.setJSONObject(id);
					bundle.putParcelable("imageData", data);
					singleImageFragment.setArguments(bundle);
					FragmentManager fragmentManager = getSupportFragmentManager();
					FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
					fragmentTransaction.replace(R.id.frame_layout, singleImageFragment).commitAllowingStateLoss();
				}
			}
			catch (JSONException e) {
				Log.e("Error!", e.toString());
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		getActionBar().setTitle(R.string.activity_title_images);
	}
}
