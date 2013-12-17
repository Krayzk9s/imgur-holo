package com.krayzk9s.imgurholo.tools;

import android.os.AsyncTask;
import android.util.Log;

import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
public class NewAlbumAsync extends AsyncTask<Void, Void, Void> {
	private final String title;
	private final String description;
	private final ApiCall apiCallStatic;
	private final ArrayList<String> imageIds;
	private final GetData listener;

	public NewAlbumAsync(String _title, String _description, ApiCall _apiCallStatic, ArrayList<String> _imageIds, GetData _listener) {
		title = _title;
		description = _description;
		imageIds = _imageIds;
		listener = _listener;
		apiCallStatic = _apiCallStatic;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		HashMap<String, Object> albumMap = new HashMap<String, Object>();
		albumMap.put("title", title);
		albumMap.put("description", description);
		if (imageIds != null) {
			albumMap.put("ids", Arrays.asList(imageIds));
			albumMap.put(ImgurHoloActivity.IMAGE_DATA_COVER, imageIds.get(1));
			Log.d("Array", Arrays.asList(imageIds).toString());
		}
		JSONObject albumdata = apiCallStatic.makeCall("/3/album/", "post", albumMap);
		try {
			Log.d("Album Data", albumdata.toString());
			if (listener != null)
				listener.onGetObject(albumdata.getJSONObject("data").getString("id"), "album");
		} catch (JSONException e) {
			Log.e("Error!", e.toString());
		}
		return null;
	}
}
