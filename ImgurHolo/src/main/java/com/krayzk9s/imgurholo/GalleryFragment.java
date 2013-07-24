package com.krayzk9s.imgurholo;

import android.app.ActionBar;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SpinnerAdapter;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.scribe.model.Token;

import java.util.ArrayList;

/**
 * Created by Kurt Zimmer on 7/23/13.
 */
public class GalleryFragment extends Fragment {

    private ArrayList<String> urls;
    private ArrayList<JSONObject> ids;
    ImageAdapter imageAdapter;
    String sort;
    int page;
    String[] spinnerList;
    AsyncTask<Void, Void, Void> async;

    public GalleryFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        page = 0;
        sort = "hot";
        setupActionBar();
        urls = new ArrayList<String>();
        ids = new ArrayList<JSONObject>();
        View view = inflater.inflate(R.layout.image_layout, container, false);
        GridView gridview = (GridView) view;
        imageAdapter = new ImageAdapter(view.getContext());
        gridview.setAdapter(imageAdapter);
        gridview.setOnItemClickListener(new GridItemClickListener());
        makeGallery();
        return gridview;
    }

    private void makeGallery() {
        urls = new ArrayList<String>();
        ids = new ArrayList<JSONObject>();
        imageAdapter.notifyDataSetChanged();
        async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                Token accessKey = activity.getAccessToken();
                Log.d("Key", accessKey.getToken());
                JSONObject imagesData = activity.makeGetCall("3/gallery/" + sort + "/" + page);
                try {
                    Log.d("URI", imagesData.toString());
                    JSONArray imageArray = imagesData.getJSONArray("data");
                    for (int i = 0; i < 30; i++) {
                        JSONObject imageData = imageArray.getJSONObject(i);
                        if (imageData.getBoolean("is_album"))
                            continue;
                        urls.add("http://imgur.com/" + imageData.getString("id") + "m.png");
                        ids.add(imageData);
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
        JSONObject id = ids.get(position);
        SingleImageFragment fragment = new SingleImageFragment();
        fragment.setGallery(true);
        fragment.setParams(id);
        MainActivity activity = (MainActivity) getActivity();
        activity.changeFragment(fragment);
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity activity = (MainActivity) getActivity();
        ActionBar actionBar = activity.getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupActionBar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        async.cancel(true);
    }

    private void setupActionBar() {
        MainActivity activity = (MainActivity) getActivity();
        ActionBar actionBar = activity.getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(activity, R.array.galleryOptions,
                android.R.layout.simple_spinner_dropdown_item);
        spinnerList = activity.getResources().getStringArray(R.array.galleryOptions);
        ActionBar.OnNavigationListener mNavigationCallback = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int i, long l) {
                MainActivity activity = (MainActivity) getActivity();
                sort = spinnerList[i];
                Log.d("URI", sort);
                Log.d("URI", "" + i);
                Log.d("URI", "" + l);
                makeGallery();
                return true;
            }
        };
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, mNavigationCallback);
    }
}