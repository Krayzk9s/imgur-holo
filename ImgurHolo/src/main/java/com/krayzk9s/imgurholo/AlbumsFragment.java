package com.krayzk9s.imgurholo;

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
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Kurt Zimmer on 7/23/13.
 */
public class AlbumsFragment extends Fragment {

    ImageAdapter imageAdapter;
    AsyncTask<Void, Void, Void> async;
    String username;
    TextView noImageView;
    private ArrayList<String> urls;
    private ArrayList<String> ids;
    int lastInView = -1;
    JSONObject imagesData;
    TextView errorText;

    public AlbumsFragment(String _username) {
        username = _username;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
                        try {
                            Log.d("Header", newTitle.getText().toString());
                            NewAlbumAsync messagingAsync = new NewAlbumAsync(newTitle.getText().toString(), newDescription.getText().toString());
                            messagingAsync.execute();
                        } catch (Exception e) {
                            Log.e("Error!", "oops, some text fields missing values");
                        }

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

    public void getImages() {
        AsyncTask<Void, Void, Boolean> imageAsync = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                imagesData = activity.makeCall("3/account/" + username + "/albums", "get", null);
                try {
                    JSONArray imageArray = imagesData.getJSONArray("data");
                    for (int i = 0; i < imageArray.length(); i++) {
                        JSONObject imageData = imageArray.getJSONObject(i);
                        Log.d("adding album...", imageData.getString("id"));
                        urls.add("http://imgur.com/" + imageData.getString("cover") + "m.png");
                        ids.add(imageData.getString("id"));
                    }
                    if (imageArray.length() > 0)
                        return true;
                    else return false;

                } catch (Exception e) {
                    Log.e("Error!", e.toString());
                    imagesData = null;
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean hasImages) {
                if (hasImages)
                    imageAdapter.notifyDataSetChanged();
                else if(imagesData != null)
                    noImageView.setVisibility(View.VISIBLE);
                else
                    errorText.setVisibility(View.VISIBLE);
            }
        };
        imageAsync.execute();
    }

    public void selectItem(int position) {
        String id = ids.get(position);
        ImagesFragment fragment = new ImagesFragment();
        fragment.albumId = id;
        fragment.setImageCall(id, "/3/album/" + id + "/images", null);
        MainActivity activity = (MainActivity) getActivity();
        activity.changeFragment(fragment);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putStringArrayList("urls", urls);
        savedInstanceState.putStringArrayList("ids", ids);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (async != null)
            async.cancel(true);
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

    private class NewAlbumAsync extends AsyncTask<Void, Void, Void> {
        private String title;
        private String description;

        public NewAlbumAsync(String _title, String _description) {
            title = _title;
            description = _description;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = (MainActivity) getActivity();
            HashMap<String, Object> albumMap = new HashMap<String, Object>();
            albumMap.put("title", title);
            albumMap.put("description", description);
            activity.makeCall("/3/album/", "post", albumMap);
            return null;
        }
    }
}
