package com.krayzk9s.imgurholo.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.libs.ZoomableImageView;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Kurt Zimmer on 11/20/13.
 */
public class LoadImageAsync extends AsyncTask<Void, Void, Bitmap> {
    ZoomableImageView zoomableImageView;
    JSONParcelable imageData;
    public LoadImageAsync(ZoomableImageView _zoomableImageView, JSONParcelable _imageData) {
        zoomableImageView = _zoomableImageView;
        imageData = _imageData;
    }
    @Override
    protected Bitmap doInBackground(Void... voids) {
        try {
            URL url;
            if (imageData.getJSONObject().has("cover"))
                url = new URL("http://imgur.com/" + imageData.getJSONObject().getString("cover") + ".png");
            else
                url = new URL(imageData.getJSONObject().getString("link"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (JSONException e) {
            Log.e("Error!", e.toString());
        } catch (IOException e) {
            Log.e("Error!", e.toString());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        Log.e("set zoomable view", "set");
        zoomableImageView.setImageBitmap(bitmap);
    }
}
