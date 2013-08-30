package com.krayzk9s.imgurholo;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

/**
 * Created by info on 8/30/13.
 */
public class UploadService extends IntentService {
    public ApiCall apiCall;

    public UploadService() {
        super("UploadService");

    }
    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        apiCall = new ApiCall(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        SendImage sendImage = new SendImage(intent.getData());
        sendImage.execute();
    }

    private class SendImage extends AsyncTask<Void, Void, JSONObject> {
        Uri uri;
        Bitmap photo;

        public SendImage(Uri _uri) {
            uri = _uri;
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            if (uri != null) {
                Log.d("URI", uri.toString());
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();
                final String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                Log.d("Image Upload", filePath);
                photo = BitmapFactory.decodeFile(filePath);
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            if (byteArray == null)
                Log.d("Image Upload", "NULL :(");
            String image = Base64.encodeToString(byteArray, Base64.DEFAULT);
            Log.d("Image Upload", image);
            HashMap<String, Object> hashMap = new HashMap<String, Object>();
            hashMap.put("image", image);
            hashMap.put("type", "binary");
            JSONObject data = apiCall.makeCall("3/image", "post", hashMap);
            Log.d("Image Upload", data.toString());
            try {
                JSONObject returner = apiCall.makeCall("3/image/" + data.getJSONObject("data").getString("id"), "get", null);
                Log.d("returning", returner.toString());
                return returner.getJSONObject("data");
            } catch (Exception e) {
                Log.e("Error!", e.toString());
            }
            return new JSONObject();
        }
    }
}
