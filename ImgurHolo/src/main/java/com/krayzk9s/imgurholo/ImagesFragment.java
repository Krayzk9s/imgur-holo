package com.krayzk9s.imgurholo;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Kurt Zimmer on 7/22/13.
 */
public class ImagesFragment extends Fragment {

    private ArrayList<String> urls;
    private ArrayList<JSONParcelable> ids;
    ImageAdapter imageAdapter;
    AsyncTask<Void, Void, Void> async;
    String imageCall;
    boolean isAlbum;

    public ImagesFragment() {

    }

    public void setImageCall(boolean _isAlbum, String _imageCall) {
        isAlbum = _isAlbum;
        imageCall = _imageCall;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        menu.findItem(R.id.action_upload).setVisible(false);
        if (isAlbum) {
            menu.findItem(R.id.action_new).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.action_new:
                //select image
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        urls = new ArrayList<String>();
        ids = new ArrayList<JSONParcelable>();
        View view = inflater.inflate(R.layout.image_layout, container, false);
        GridView gridview = (GridView) view;
        imageAdapter = new ImageAdapter(view.getContext());
        gridview.setAdapter(imageAdapter);
        gridview.setOnItemClickListener(new GridItemClickListener());
        if(savedInstanceState == null)
        {
        async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                JSONObject imagesData = activity.makeGetCall(imageCall);
                try {
                    JSONArray imageArray = imagesData.getJSONArray("data");
                    for (int i = 0; i < imageArray.length(); i++) {
                        JSONObject imageData = imageArray.getJSONObject(i);
                        urls.add("http://imgur.com/" + imageData.getString("id") + "m.png");
                        JSONParcelable dataParcel = new JSONParcelable();
                        dataParcel.setJSONObject(imageData);
                        ids.add(dataParcel);
                    }
                } catch (Exception e) {
                    Log.e("Error!", e.toString());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                imageAdapter.notifyDataSetChanged();
            }
        };
        async.execute();
        }
        else
        {
            urls = savedInstanceState.getStringArrayList("urls");
            ids = savedInstanceState.getParcelableArrayList("ids");
        }
        return gridview;
    }

    public class ImageAdapter extends BaseAdapter {
        private Context mContext;

        public ImageAdapter(Context c) {
            mContext = c;
        }


        public int getCount() {
            return urls.size();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            final ImageView imageView = new SquareImageView(mContext);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            UrlImageViewHelper.setUrlDrawable(imageView, urls.get(position), R.drawable.icon);
            return imageView;
        }

    }

    private class GridItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    public void selectItem(int position) {
        JSONObject id = ids.get(position).getJSONObject();
        SingleImageFragment fragment = new SingleImageFragment();
        fragment.setParams(id);
        MainActivity activity = (MainActivity) getActivity();
        activity.changeFragment(fragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(async != null)
            async.cancel(true);
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putStringArrayList("urls", urls);
        savedInstanceState.putParcelableArrayList("ids", ids);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
}
