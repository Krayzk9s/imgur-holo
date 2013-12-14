package com.krayzk9s.imgurholo.tools;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by Kurt Zimmer on 11/17/13.
 */
public class NewAlbumAsync extends AsyncTask<Void, Void, Void> {
	private String title;
	private String description;
	ApiCall apiCallStatic;
	ArrayList<String> imageIds;
	GetData listener;

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
			albumMap.put("cover", imageIds.get(1));
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
