package com.krayzk9s.imgurholo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Kurt Zimmer on 7/23/13.
 */
public class AlbumsFragment extends Fragment {

    private ArrayList<String> urls;
    private ArrayList<String> ids;
    ImageAdapter imageAdapter;
    AsyncTask<Void, Void, Void> async;

    public AlbumsFragment() {

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
        menu.findItem(R.id.action_new).setVisible(true);
        menu.findItem(R.id.action_upload).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        MainActivity activity = (MainActivity) getActivity();
        switch (item.getItemId()) {
            case R.id.action_new:
                final EditText newTitle = new EditText(activity);
                newTitle.setSingleLine();
                final EditText newDescription = new EditText(activity);
                newDescription.setHint("Body");
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
        GridView gridview = (GridView) view;
        imageAdapter = new ImageAdapter(view.getContext());
        gridview.setAdapter(imageAdapter);
        gridview.setOnItemClickListener(new GridItemClickListener());
        if (newData) {
            async = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    MainActivity activity = (MainActivity) getActivity();
                    JSONObject imagesData = activity.makeGetCall("3/account/me/albums");
                    try {
                        JSONArray imageArray = imagesData.getJSONArray("data");
                        for (int i = 0; i < imageArray.length(); i++) {
                            JSONObject imageData = imageArray.getJSONObject(i);
                            urls.add("http://imgur.com/" + imageData.getString("cover") + "m.png");
                            ids.add(imageData.getString("id"));
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
            activity.makeNewAlbum(title, description);
            return null;
        }
    }
}
