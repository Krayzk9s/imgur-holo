package com.krayzk9s.imgurholo.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcelable;
import android.util.Log;

import com.krayzk9s.imgurholo.libs.JSONParcelable;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
        DownloadAsync downloadAsync = new DownloadAsync(intent.getParcelableArrayListExtra("ids"), getApplicationContext());
        downloadAsync.execute();
    }

    private static class DownloadAsync extends AsyncTask<Void, Void, String> {
        ArrayList<Parcelable> ids;
        Context context;
        public DownloadAsync(ArrayList<Parcelable> _ids, Context _context) {
            ids = _ids;
            context = _context;
        }
        @Override
        protected String doInBackground(Void... voids) {
            String path = "";
            try {
                for (int i = 0; i < ids.size(); i++) {
                    JSONParcelable idget = (JSONParcelable) ids.get(i);
                    String type = idget.getJSONObject().getString("type").split("/")[1];
                    Log.d("URL", "http://i.imgur.com/" + idget.getJSONObject().getString("id") + "." + type);
                    Log.d("IDs", idget.getJSONObject().getString("id"));
                    URL url = new URL("http://i.imgur.com/" + idget.getJSONObject().getString("id") + "." + type);
                    path = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + idget.getJSONObject().getString("id") + "." + type;
                    File file = new File(path);
                    URLConnection ucon = url.openConnection();
                    InputStream is = ucon.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is);
                    ByteArrayBuffer baf = new ByteArrayBuffer(500);
                    int current = 0;
                    while ((current = bis.read()) != -1) {
                        baf.append((byte) current);
                    }
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(baf.toByteArray());
                    fos.close();
                }
            } catch (MalformedURLException e) {
                Log.e("Error!", e.toString());
            } catch (IOException e) {
                Log.e("Error!", e.toString());
            } catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
            return path;
        }

        @Override
        protected void onPostExecute(String path) {
            MediaScannerConnection.scanFile(context, new String[]{path}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(final String path, final Uri uri) {
                            Log.i("Scanning", String.format("Scanned path %s -> URI = %s", path, uri.toString()));
                        }
                    });
        }
    }
}
