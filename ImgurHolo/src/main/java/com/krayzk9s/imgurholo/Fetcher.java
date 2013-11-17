package com.krayzk9s.imgurholo;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

/**
 * Created by Kurt Zimmer on 11/16/13.
 */
public class Fetcher extends AsyncTask<Void, Void, JSONObject> {
    GetData mlistener;
    String call;
    MainActivity activity;

    public Fetcher(GetData listener, String _call, MainActivity _activity) {
        mlistener = listener;
        call = _call;
        activity = _activity;
    }
    @Override
    protected JSONObject doInBackground(Void... voids) {
        Log.d("imagesData", call);
        return activity.makeCall(call, "get", null);
    }
    @Override
    protected void onPostExecute(JSONObject data) {
        if(mlistener != null) {
            mlistener.onGetObject(data);
        }
    }
}
