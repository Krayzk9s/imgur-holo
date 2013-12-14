package com.krayzk9s.imgurholo.tools;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Kurt Zimmer on 11/20/13.
 */
public class AddImagesToAlbumAsync extends AsyncTask<Void, Void, Void> {
	boolean add;
	private ArrayList<String> imageIDsAsync;
	String albumId;
	ApiCall apiCall;

	public AddImagesToAlbumAsync(ArrayList<String> _imageIDs, boolean _add, ApiCall _apiCall, String _albumId) {
		imageIDsAsync = _imageIDs;
		add = _add;
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
