package com.krayzk9s.imgurholo.tools;

import android.os.AsyncTask;

import java.util.ArrayList;
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
public class AddImagesToAlbumAsync extends AsyncTask<Void, Void, Void> {
    private final ArrayList<String> imageIDsAsync;
	private final String albumId;
	private final ApiCall apiCall;

	public AddImagesToAlbumAsync(ArrayList<String> _imageIDs, ApiCall _apiCall, String _albumId) {
		imageIDsAsync = _imageIDs;
        apiCall = _apiCall;
		albumId = _albumId;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		String albumids = "";
		for (int i = 0; i < imageIDsAsync.size(); i++) {
			if (i != 0)
				albumids += ",";
			albumids += imageIDsAsync.get(i);
		}
		HashMap<String, Object> editMap = new HashMap<String, Object>();
		editMap.put("ids", albumids);
		editMap.put("id", albumId);
		apiCall.makeCall("3/album/" + albumId, "post", editMap);
		return null;
	}
}
