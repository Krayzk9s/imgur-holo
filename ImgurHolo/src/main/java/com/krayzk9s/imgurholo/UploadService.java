package com.krayzk9s.imgurholo;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

/**
 * Created by info on 8/30/13.
 */
public class UploadService extends IntentService {
    public ApiCall apiCall;
    NotificationManager notificationManager;

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
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        Notification notification = notificationBuilder
                .setSmallIcon(R.drawable.icon_desaturated)
                .setContentText("Now Uploading")
                .setProgress(0, 0, true)
                .setContentTitle("imgur Image Uploader")
                .build();
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
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

        @Override
        protected void onPostExecute(JSONObject data) {
            Log.d("Built", "Notification building");
            NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
            bigPictureStyle.bigPicture(photo);
            Log.d("Built", "Picture set");
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
            try {
            Log.d("data", data.toString());
            Intent viewImageIntent = new Intent();
            viewImageIntent.setAction(Intent.ACTION_VIEW);
            viewImageIntent.setData(Uri.parse(data.getString("link")));
            Intent shareIntent = new Intent();
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String link = "";
            if (settings.getString("AutoCopyType", getResources().getString(R.string.direct_link)) == getResources().getString(R.string.direct_link)) {
               link = "http://imgur.com/" + data.getString("id");
            }
            else if(settings.getString("AutoCopyType", getResources().getString(R.string.direct_link)) == getResources().getString(R.string.link)) {
                link = data.getString("link");
            }
            else if(settings.getString("AutoCopyType", getResources().getString(R.string.direct_link)) == getResources().getString(R.string.html_link)) {
                link = "<a href=\"http://imgur.com/" + data.getString("id") + "\"><img src=\"" + data.getString("link") + "\" title=\"Hosted by imgur.com\"/></a>";
            }
            else if(settings.getString("AutoCopyType", getResources().getString(R.string.direct_link)) == getResources().getString(R.string.bbcode_link)) {
                link = "[IMG]" + data.getString("link") + "[/IMG]";
            }
            else if(settings.getString("AutoCopyType", getResources().getString(R.string.direct_link)) == getResources().getString(R.string.linked_bbcode_link)) {
                link = "[URL=http://imgur.com/" + data.getString("id") + "][IMG]" + data.getString("link") + "[/IMG][/URL]";
            }
            else if(settings.getString("AutoCopyType", getResources().getString(R.string.direct_link)) == getResources().getString(R.string.markdown_link)) {
                link = "[Imgur](http://i.imgur.com/" + data.getString("id") + ")";
            }
                shareIntent.setType("text/plain");
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                try {
                    shareIntent.putExtra(Intent.EXTRA_TEXT, link);
            } catch (Exception e) {
               Log.e("Error!", "bad link to share");
            }
            PendingIntent viewImagePendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), viewImageIntent, 0);
            PendingIntent sharePendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), shareIntent, 0);
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
            catch (Exception e) {
                Log.e("Error!", e.toString());
                notificationManager.cancel(0);
                notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
                Notification notification = notificationBuilder
                        .setSmallIcon(R.drawable.icon_desaturated)
                        .setContentText("Error - Image Upload Failed")
                        .setContentTitle("imgur Image Uploader")
                        .build();
                notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(0, notification);
            }
        }
    }
}
