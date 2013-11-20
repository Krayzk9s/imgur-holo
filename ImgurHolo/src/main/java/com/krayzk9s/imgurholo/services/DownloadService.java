package com.krayzk9s.imgurholo.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
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

    private static class DownloadAsync extends AsyncTask<Void, Void, Void> {
        ArrayList<Parcelable> ids;
        Context context;
        public DownloadAsync(ArrayList<Parcelable> _ids, Context _context) {
            ids = _ids;
            context = _context;
        }
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                for (int i = 0; i < ids.size(); i++) {
                    JSONParcelable idget = (JSONParcelable) ids.get(i);
                    String type = idget.getJSONObject().getString("type").split("/")[1];
                    Log.d("URL", "http://i.imgur.com/" + idget.getJSONObject().getString("id") + "." + type);
                    Log.d("IDs", idget.getJSONObject().getString("id"));
                    URL url = new URL("http://i.imgur.com/" + idget.getJSONObject().getString("id") + "." + type);
                    File file = new File(android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + idget.getJSONObject().getString("id") + "." + type);
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
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                    + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))));
        }
    }
}
