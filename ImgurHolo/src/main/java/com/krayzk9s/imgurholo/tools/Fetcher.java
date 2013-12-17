package com.krayzk9s.imgurholo.tools;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

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
public class Fetcher extends AsyncTask<Void, Void, JSONObject> {
	private final GetData mlistener;
	private final String call;
	private final ApiCall apiCall;
	private final String tag;
	private final String callType;
	private final HashMap<String, Object> args;
	private Exception exception;

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
		} catch (Exception e) {
			exception = e;
			return null;
		}
	}

	@Override
	protected void onPostExecute(JSONObject data) {
		if (data != null && mlistener != null) {
			mlistener.onGetObject(data, tag);
		} else if (data == null && mlistener != null && exception != null) {
			mlistener.handleException(exception, tag);
		}
	}
}
