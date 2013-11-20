package com.krayzk9s.imgurholo.tools;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

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

/**
 * Created by Kurt Zimmer on 11/20/13.
 */
public class DownloadAsync extends AsyncTask<Void, Void, Void> {
    Activity activity;
    JSONParcelable imageData;
    public DownloadAsync(Activity _activity, JSONParcelable _imageData) {
        activity = _activity;
        imageData = _imageData;
    }
    @Override
    protected Void doInBackground(Void... voids) {
        try {
            URL url = new URL(imageData.getJSONObject().getString("link"));
            String type = imageData.getJSONObject().getString("link").split("/")[3];
            File file = new File(android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + imageData.getJSONObject().getString("id") + "." + type);
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                    + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))));
        } catch (JSONException e) {
            Log.e("Error!", e.toString());
        } catch (MalformedURLException e) {
            Log.e("Error!", e.toString());
        } catch (IOException e) {
            Log.e("Error!", e.toString());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        int duration = Toast.LENGTH_SHORT;
        Toast toast;
        toast = Toast.makeText(activity, "Downloaded!", duration);
        toast.show();
    }
}
