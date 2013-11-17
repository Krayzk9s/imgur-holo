package com.krayzk9s.imgurholo;

/*
 * Copyright 2013 Kurt Zimmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
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
        apiCall = new ApiCall();
        apiCall.setSettings(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SendImage sendImage = new SendImage(intent.getData(), apiCall, getContentResolver().query(intent.getData(), null, null, null, null), getApplicationContext(), getResources());
        sendImage.execute();
    }

    /* From http://stackoverflow.com/users/1946055/tobiel at http://stackoverflow.com/questions/17839388/creating-a-scaled-bitmap-with-createscaledbitmap-in-android */
    public static Bitmap lessResolution (String filePath, int width, int height)
    {   int reqHeight=width;
        int reqWidth=height;
        BitmapFactory.Options options = new BitmapFactory.Options();

        // First decode with inJustDecodeBounds=true to check dimensions
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        float factor = calculateSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return Bitmap.createScaledBitmap(BitmapFactory.decodeFile(filePath), (int)Math.floor(options.outWidth*factor), (int)Math.floor(options.outHeight*factor), false);
    }

    private static float calculateSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        Log.d("reqHeight", reqHeight + "");
        Log.d("reqWidth", reqWidth + "");
        Log.d("height", height + "");
        Log.d("width", width + "");
        float factor;
        if (height > reqHeight || width > reqWidth) {
            final float heightRatio = (float) reqHeight / (float) height;
            final float widthRatio = (float) reqWidth / (float) width;
            Log.d("heightRatio", heightRatio + "");
            Log.d("widthRatio", widthRatio + "");
            factor = heightRatio < widthRatio ? heightRatio : widthRatio;
            Log.d("factor", factor + "");
        }
        else
            factor = 1;
        return factor;
    }

    private static class SendImage extends AsyncTask<Void, Void, JSONObject> {
        Uri uri;
        Bitmap photo;
        ApiCall apiCallStatic;
        Cursor cursor;
        SharedPreferences settings;
        Context context;
        Resources resources;
        NotificationManager notificationManager;

        public SendImage(Uri _uri, ApiCall _apiCallStatic, Cursor _cursor, Context _context, Resources _resources) {
            uri = _uri;
            apiCallStatic = _apiCallStatic;
            cursor = _cursor;
            context = _context;
            settings = PreferenceManager.getDefaultSharedPreferences(context);
            resources = _resources;
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
            Notification notification = notificationBuilder
                    .setSmallIcon(R.drawable.icon_desaturated)
                    .setContentText("Now Uploading")
                    .setProgress(0, 0, true)
                    .setContentTitle("imgur Image Uploader")
                    .build();
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(0, notification);
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            cursor.moveToFirst();
            final String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
            Log.d("Image Upload", filePath);
            int maxHeight = Integer.MAX_VALUE;
            if(settings.getBoolean("HeightBoolean", false))
                maxHeight = Integer.parseInt(settings.getString("HeightSize", "1080"));
            int maxWidth = Integer.MAX_VALUE;
            if(settings.getBoolean("HeightBoolean", false))
                maxWidth = Integer.parseInt(settings.getString("WidthSize", "1920"));
            photo = lessResolution(filePath, maxWidth, maxHeight);
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
            JSONObject data = apiCallStatic.makeCall("3/image", "post", hashMap);
            Log.d("Image Upload", data.toString());
            try {
                JSONObject returner = apiCallStatic.makeCall("3/image/" + data.getJSONObject("data").getString("id"), "get", null);
                Log.d("returning", returner.toString());
                return returner.getJSONObject("data");
            } catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
            return new JSONObject();
        }

        @Override
        protected void onPostExecute(JSONObject data) {
            Log.d("Built", "Notification building");
            NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
            bigPictureStyle.bigPicture(photo);
            Log.d("Built", "Picture set");
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
            try {
            Log.d("data", data.toString());
            Intent viewImageIntent = new Intent();
            viewImageIntent.setAction(Intent.ACTION_VIEW);
            viewImageIntent.setData(Uri.parse(data.getString("link")));
            Intent shareIntent = new Intent();

            String link = "";
            if (settings.getString("AutoCopyType", resources.getString(R.string.direct_link)).equals(resources.getString(R.string.direct_link))) {
               link = "http://imgur.com/" + data.getString("id");
            }
            else if(settings.getString("AutoCopyType", resources.getString(R.string.direct_link)).equals(resources.getString(R.string.link))) {
                link = data.getString("link");
            }
            else if(settings.getString("AutoCopyType", resources.getString(R.string.direct_link)).equals(resources.getString(R.string.html_link))) {
                link = "<a href=\"http://imgur.com/" + data.getString("id") + "\"><img src=\"" + data.getString("link") + "\" title=\"Hosted by imgur.com\"/></a>";
            }
            else if(settings.getString("AutoCopyType", resources.getString(R.string.direct_link)).equals(resources.getString(R.string.bbcode_link))) {
                link = "[IMG]" + data.getString("link") + "[/IMG]";
            }
            else if(settings.getString("AutoCopyType", resources.getString(R.string.direct_link)).equals(resources.getString(R.string.linked_bbcode_link))) {
                link = "[URL=http://imgur.com/" + data.getString("id") + "][IMG]" + data.getString("link") + "[/IMG][/URL]";
            }
            else if(settings.getString("AutoCopyType", resources.getString(R.string.direct_link)).equals(resources.getString(R.string.markdown_link))) {
                link = "[Imgur](http://i.imgur.com/" + data.getString("id") + ")";
            }
                shareIntent.setType("text/plain");
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, link);
            PendingIntent viewImagePendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), viewImageIntent, 0);
            PendingIntent sharePendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), shareIntent, 0);
            Notification notification = notificationBuilder
                    .setSmallIcon(R.drawable.icon_desaturated)
                    .setContentText("Finished Uploading")
                    .setStyle(bigPictureStyle)
                    .setContentTitle("imgur Image Uploader")
                    .setContentIntent(viewImagePendingIntent)
                    .addAction(R.drawable.dark_social_share, "Share", sharePendingIntent)
                    .build();
            Log.d("Built", "Notification built");
            notificationManager.cancel(0);
            notificationManager.notify(1, notification);
            Log.d("Built", "Notification display");
            }
            catch (JSONException e) {
                Log.e("Error!", e.toString());
                notificationManager.cancel(0);
                notificationBuilder = new NotificationCompat.Builder(context);
                Notification notification = notificationBuilder
                        .setSmallIcon(R.drawable.icon_desaturated)
                        .setContentText("Error - Image Upload Failed")
                        .setContentTitle("imgur Image Uploader")
                        .build();
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(0, notification);
            }
        }
    }
}
