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
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;

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
public class DownloadService extends IntentService {

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
		final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		ArrayList<JSONParcelable> ids = intent.getParcelableArrayListExtra("ids");
		try {
			final String type = ids.get(0).getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_TYPE).split("/")[1];
			final String id = ids.get(0).getJSONObject().getString("id");
			final String link = ids.get(0).getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_LINK);
			Log.d("data", ids.get(0).getJSONObject().toString());
			Log.d(ImgurHoloActivity.IMAGE_DATA_TYPE, ids.get(0).getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_TYPE).split("/")[1]);
			Log.d("id", ids.get(0).getJSONObject().getString("id"));
			Log.d(ImgurHoloActivity.IMAGE_DATA_LINK, ids.get(0).getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_LINK));
			final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
			notificationBuilder.setContentTitle(getString(R.string.picture_download))
					.setContentText(getString(R.string.download_in_progress))
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
							NotificationCompat.Builder notificationComplete = new NotificationCompat.Builder(getApplicationContext());
							Intent viewImageIntent = new Intent(Intent.ACTION_VIEW);
							viewImageIntent.setDataAndType(Uri.fromFile(file),"image/*");
							Intent shareIntent = new Intent(Intent.ACTION_SEND);
							shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
							shareIntent.setType("image/*");
							shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
							PendingIntent viewImagePendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), viewImageIntent, 0);
							PendingIntent sharePendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), shareIntent, 0);
							notificationComplete.setContentTitle(getString(R.string.download_complete))
									.setSmallIcon(R.drawable.icon_desaturated)
									.setContentText(getString(R.string.download_finished))
									.setContentIntent(viewImagePendingIntent)
									.addAction(R.drawable.dark_social_share, getString(R.string.share), sharePendingIntent);
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
