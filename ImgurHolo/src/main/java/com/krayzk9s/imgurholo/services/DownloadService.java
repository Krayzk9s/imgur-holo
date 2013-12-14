package com.krayzk9s.imgurholo.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.libs.JSONParcelable;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Kurt Zimmer on 11/17/13.
 */
public class DownloadService extends IntentService {

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
		final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		ArrayList<JSONParcelable> ids = intent.getParcelableArrayListExtra("ids");
		try {
			final String type = ids.get(0).getJSONObject().getString("type").split("/")[1];
			final String id = ids.get(0).getJSONObject().getString("id");
			final String link = ids.get(0).getJSONObject().getString("link");
			Log.d("data", ids.get(0).getJSONObject().toString());
			Log.d("type", ids.get(0).getJSONObject().getString("type").split("/")[1]);
			Log.d("id", ids.get(0).getJSONObject().getString("id"));
			Log.d("link", ids.get(0).getJSONObject().getString("link"));
			final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
			notificationBuilder.setContentTitle("Picture Download")
					.setContentText("Download in progress")
					.setSmallIcon(R.drawable.icon_desaturated);
			Ion.with(getApplicationContext(), link)
					.progress(new ProgressCallback() {
						@Override
						public void onProgress(int i, int i2) {
							notificationBuilder.setProgress(i2, i, false);
						}
					})
					.write(new File(android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + id + "." + type))
					.setCallback(new FutureCallback<File>() {
						@Override
						public void onCompleted(Exception e, File file) {
							//notificationManager.cancel(0);
							NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
							NotificationCompat.Builder notificationComplete = new NotificationCompat.Builder(getApplicationContext());
							Intent viewImageIntent = new Intent(Intent.ACTION_VIEW);
							viewImageIntent.setDataAndType(Uri.fromFile(file),"image/*");
							Intent shareIntent = new Intent(Intent.ACTION_SEND);
							shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
							shareIntent.setType("image/*");
							shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
							PendingIntent viewImagePendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), viewImageIntent, 0);
							PendingIntent sharePendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), shareIntent, 0);
							notificationComplete.setContentTitle("Download Complete")
									.setSmallIcon(R.drawable.icon_desaturated)
									.setContentText("Image download finished")
									.setContentIntent(viewImagePendingIntent)
									.addAction(R.drawable.dark_social_share, "Share", sharePendingIntent);
							notificationManager.cancel(0);
							notificationManager.notify(1, notificationComplete.build());
							MediaScannerConnection.scanFile(getApplicationContext(), new String[]{android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + id + "." + type}, null,
									new MediaScannerConnection.OnScanCompletedListener() {
										@Override
										public void onScanCompleted(final String path, final Uri uri) {
											Log.i("Scanning", String.format("Scanned path %s -> URI = %s", path, uri.toString()));
										}
									});
						}
					});
			notificationManager.notify(0, notificationBuilder.build());
		}
		catch (JSONException e) {
			Log.e("Error!", e.toString());
		}
    }
}
