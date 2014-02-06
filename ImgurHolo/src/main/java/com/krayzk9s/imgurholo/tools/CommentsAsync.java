package com.krayzk9s.imgurholo.tools;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
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
public class CommentsAsync extends AsyncTask<Void, Void, Void> {
	private final ImgurHoloActivity activity;
	private final JSONObject galleryAlbumData;

	public CommentsAsync(ImgurHoloActivity _activity, JSONObject _galleryAlbumData) {
		galleryAlbumData = _galleryAlbumData;
		activity = _activity;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		try {
			JSONObject imageParam = activity.getApiCall().makeCall("3/image/" + galleryAlbumData.getString(ImgurHoloActivity.IMAGE_DATA_COVER), "get", null).getJSONObject("data");
			Log.d("Params", imageParam.toString());
			galleryAlbumData.put(ImgurHoloActivity.IMAGE_DATA_WIDTH, imageParam.getInt(ImgurHoloActivity.IMAGE_DATA_WIDTH));
			galleryAlbumData.put(ImgurHoloActivity.IMAGE_DATA_TYPE, imageParam.getString(ImgurHoloActivity.IMAGE_DATA_TYPE));
			galleryAlbumData.put(ImgurHoloActivity.IMAGE_DATA_HEIGHT, imageParam.getInt(ImgurHoloActivity.IMAGE_DATA_HEIGHT));
			galleryAlbumData.put(ImgurHoloActivity.IMAGE_DATA_SIZE, imageParam.getInt(ImgurHoloActivity.IMAGE_DATA_SIZE));
			Log.d("Params w/ new data", galleryAlbumData.toString());
		} catch (JSONException e) {
			Log.e("Error!", "bad single image call" + e.toString());
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		if (galleryAlbumData != null) {
			SingleImageFragment fragment = new SingleImageFragment();
			Bundle bundle = new Bundle();
			bundle.putBoolean("gallery", true);
			JSONParcelable data = new JSONParcelable();
			data.setJSONObject(galleryAlbumData);
			bundle.putParcelable("imageData", data);
			fragment.setArguments(bundle);
			if (activity != null) {
				FragmentManager fragmentManager = activity.getSupportFragmentManager();
				FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
				fragmentTransaction.replace(R.id.frame_layout, fragment).commit();
			}
		}
	}
}
