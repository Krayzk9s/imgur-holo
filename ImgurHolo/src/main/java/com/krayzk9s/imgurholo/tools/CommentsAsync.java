package com.krayzk9s.imgurholo.tools;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.ui.SingleImageFragment;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Kurt Zimmer on 11/20/13.
 */
public class CommentsAsync extends AsyncTask<Void, Void, Void> {
    MainActivity activity;
    JSONObject galleryAlbumData;
    ApiCall apiCall;
    public CommentsAsync(ApiCall _apiCall, JSONObject _galleryAlbumData) {
        galleryAlbumData = _galleryAlbumData;
        apiCall = _apiCall;
    }
    @Override
    protected Void doInBackground(Void... voids) {
        try {
            JSONObject imageParam = apiCall.makeCall("3/image/" + galleryAlbumData.getString("cover"), "get", null).getJSONObject("data");
            Log.d("Params", imageParam.toString());
            galleryAlbumData.put("width", imageParam.getInt("width"));
            galleryAlbumData.put("type", imageParam.getString("type"));
            galleryAlbumData.put("height", imageParam.getInt("height"));
            galleryAlbumData.put("size", imageParam.getInt("size"));
            Log.d("Params w/ new data", galleryAlbumData.toString());
        } catch (JSONException e) {
            Log.e("Error!", "bad single image call" + e.toString());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if(galleryAlbumData != null) {
            SingleImageFragment fragment = new SingleImageFragment();
            Bundle bundle = new Bundle();
            bundle.putBoolean("gallery", true);
            JSONParcelable data = new JSONParcelable();
            data.setJSONObject(galleryAlbumData);
            bundle.putParcelable("imageData", data);
            fragment.setArguments(bundle);
            if(activity != null)
                activity.changeFragment(fragment, true);
        }
    }
}
