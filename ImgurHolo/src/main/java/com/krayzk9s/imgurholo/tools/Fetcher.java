package com.krayzk9s.imgurholo.tools;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Kurt Zimmer on 11/16/13.
 */
public class Fetcher extends AsyncTask<Void, Void, JSONObject> {
    GetData mlistener;
    String call;
    ApiCall apiCall;
    String tag;
    String callType;
    HashMap<String, Object> args;
    Exception exception;

    public Fetcher(GetData listener, String _call, String _callType, HashMap<String, Object> _args, ApiCall _apiCall, String _tag) {
        mlistener = listener;
        call = _call;
        apiCall = _apiCall;
        tag = _tag;
        callType = _callType;
        args = _args;
    }
    @Override
    protected JSONObject doInBackground(Void... voids) {
        Log.d("imagesData", call);
        try {
            return apiCall.makeCall(call, callType, args);
        }
        catch(Exception e) {
            exception = e;
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject data) {
        if(data != null && mlistener != null) {
            mlistener.onGetObject(data, tag);
        }
        else if(data == null && mlistener != null && exception != null) {
            mlistener.handleException(exception, tag);
        }
    }
}
