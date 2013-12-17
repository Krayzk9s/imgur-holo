package com.krayzk9s.imgurholo.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.libs.ZoomableImageView;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
class LoadImageAsync extends AsyncTask<Void, Void, Bitmap> {
	private final ZoomableImageView zoomableImageView;
	private final JSONParcelable imageData;

	public LoadImageAsync(ZoomableImageView _zoomableImageView, JSONParcelable _imageData) {
		zoomableImageView = _zoomableImageView;
		imageData = _imageData;
	}

	@Override
	protected Bitmap doInBackground(Void... voids) {
		try {
			URL url;
			if (imageData.getJSONObject().has(ImgurHoloActivity.IMAGE_DATA_COVER))
				url = new URL("http://imgur.com/" + imageData.getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_COVER) + ".png");
			else
				url = new URL(imageData.getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_LINK));
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			return BitmapFactory.decodeStream(input);
		} catch (JSONException e) {
			Log.e("Error!", e.toString());
		} catch (IOException e) {
			Log.e("Error!", e.toString());
		}
		return null;
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {
		Log.e("set zoomable view", "set");
		zoomableImageView.setImageBitmap(bitmap);
	}
}
