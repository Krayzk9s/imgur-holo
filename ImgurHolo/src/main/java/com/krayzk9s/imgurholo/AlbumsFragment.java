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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Kurt Zimmer on 7/23/13.
 */
public class AlbumsFragment extends Fragment implements GetData {

    ImageAdapter imageAdapter;
    String username;
    TextView noImageView;
    private ArrayList<String> urls;
    private ArrayList<String> ids;
    int lastInView = -1;
    TextView errorText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        username = bundle.getString("username");
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        if(username != "me")
            activity.setTitle(username + "'s Albums");
        else
            activity.setTitle("My Albums");
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        MainActivity activity = (MainActivity)getActivity();
        if(activity.theme.equals(activity.HOLO_LIGHT))
            inflater.inflate(R.menu.main, menu);
        else
            inflater.inflate(R.menu.main_dark, menu);
        if (username.equals("me"))
            menu.findItem(R.id.action_new).setVisible(true);
        menu.findItem(R.id.action_upload).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        MainActivity activity = (MainActivity) getActivity();
        switch (item.getItemId()) {
            case R.id.action_refresh:
                urls = new ArrayList<String>();
                imageAdapter.notifyDataSetChanged();
                getImages();
                return true;
            case R.id.action_new:
                final EditText newTitle = new EditText(activity);
                newTitle.setSingleLine();
                newTitle.setHint("Album Title");
                final EditText newDescription = new EditText(activity);
                newDescription.setHint("Description");
                LinearLayout linearLayout = new LinearLayout(activity);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                linearLayout.addView(newTitle);
                linearLayout.addView(newDescription);
                new AlertDialog.Builder(activity).setTitle("New Album")
                        .setView(linearLayout).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        NewAlbumAsync messagingAsync = new NewAlbumAsync(newTitle.getText().toString(), newDescription.getText().toString(), (MainActivity) getActivity());
                        messagingAsync.execute();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        boolean newData = false;
        if (savedInstanceState == null && urls == null) {
            urls = new ArrayList<String>();
            ids = new ArrayList<String>();
            newData = true;
        } else if (savedInstanceState != null) {
            urls = savedInstanceState.getStringArrayList("urls");
            ids = savedInstanceState.getStringArrayList("ids");
        }
        View view = inflater.inflate(R.layout.image_layout, container, false);
        errorText = (TextView) view.findViewById(R.id.error);
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) errorText.getLayoutParams();
        mlp.setMargins(0, 0, 0, 0);
        view.setPadding(0, getActivity().getActionBar().getHeight(), 0, 0);
        noImageView = (TextView) view.findViewById(R.id.no_images);
        GridView gridview = (GridView) view.findViewById(R.id.grid_layout);
        MainActivity activity = (MainActivity) getActivity();
        SharedPreferences settings = activity.getSettings();
        gridview.setColumnWidth(activity.dpToPx(Integer.parseInt(settings.getString("IconSize", "90"))));
        imageAdapter = new ImageAdapter(view.getContext());
        gridview.setAdapter(imageAdapter);
        gridview.setOnItemClickListener(new GridItemClickListener());
        if (newData) {
            getImages();
        }
        gridview.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int i2, int i3) {
                if(lastInView == -1)
                    lastInView = firstVisibleItem;
                else if (lastInView > firstVisibleItem) {
                    getActivity().getActionBar().show();
                    lastInView = firstVisibleItem;
                }
                else if (lastInView < firstVisibleItem) {
                    getActivity().getActionBar().hide();
                    lastInView = firstVisibleItem;
                }
            }
        });
        return view;
    }

    public void onGetObject(Object object) {
        Boolean hasImages = false;
        JSONObject imagesData = (JSONObject) object;
        try {
            JSONArray imageArray = imagesData.getJSONArray("data");
            for (int i = 0; i < imageArray.length(); i++) {
                JSONObject imageData = imageArray.getJSONObject(i);
                Log.d("adding album...", imageData.getString("id"));
                urls.add("http://imgur.com/" + imageData.getString("cover") + "m.png");
                ids.add(imageData.getString("id"));
            }
            if (imageArray.length() > 0)
                hasImages = true;
            else
                hasImages = false;

        } catch (JSONException e) {
            Log.e("Error!", e.toString());
            imagesData = null;
        }
        if (hasImages)
            imageAdapter.notifyDataSetChanged();
        else if(imagesData != null)
            noImageView.setVisibility(View.VISIBLE);
        else
            errorText.setVisibility(View.VISIBLE);
    }

    public void getImages() {
        Fetcher fetcher = new Fetcher(this, "3/account/" + username + "/albums", (MainActivity) getActivity());
        fetcher.execute();
    }

    public void selectItem(int position) {
        String id = ids.get(position);
        ImagesFragment fragment = new ImagesFragment();
        Bundle bundle = new Bundle();
        bundle.putString("imageCall", "3/album/" + id);
        bundle.putString("id", id);
        fragment.setArguments(bundle);
        MainActivity activity = (MainActivity) getActivity();
        activity.changeFragment(fragment, true);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putStringArrayList("urls", urls);
        savedInstanceState.putStringArrayList("ids", ids);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
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

            UrlImageViewHelper.setUrlDrawable(imageView, urls.get(position));
            return imageView;
        }

    }

    private class GridItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private static class NewAlbumAsync extends AsyncTask<Void, Void, Void> {
        private String title;
        private String description;
        MainActivity activity;

        public NewAlbumAsync(String _title, String _description, MainActivity _activity) {
            title = _title;
            description = _description;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            HashMap<String, Object> albumMap = new HashMap<String, Object>();
            albumMap.put("title", title);
            albumMap.put("description", description);
            activity.makeCall("/3/album/", "post", albumMap);
            return null;
        }
    }
}
