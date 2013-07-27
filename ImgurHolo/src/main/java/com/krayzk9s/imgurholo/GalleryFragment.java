package com.krayzk9s.imgurholo;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.scribe.model.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Kurt Zimmer on 7/23/13.
 */
public class GalleryFragment extends Fragment {

    private ArrayList<String> urls;
    private ArrayList<JSONObject> ids;
    ImageAdapter imageAdapter;
    String sort;
    String gallery;
    int page;
    AsyncTask<Void, Void, Void> async;
    String subreddit;
    String memeType;
    String window;
    ArrayAdapter<CharSequence> mSpinnerAdapter;
    ActionBar actionBar;

    public GalleryFragment() {

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
        menu.findItem(R.id.action_sort).setVisible(true);
        menu.findItem(R.id.subreddit).setVisible(true);
        menu.findItem(R.id.action_upload).setVisible(false);
        menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
        menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MainActivity activity = (MainActivity) getActivity();
        switch (item.getItemId()) {
            case R.id.action_sort:
                Log.d("sorting", "sorting");
                return true;
            case R.id.subreddit:
                gallery = "subreddit";
                sort = "time";
                final EditText subredditText = new EditText(activity);
                subredditText.setSingleLine();
                new AlertDialog.Builder(activity).setTitle("Choose SubReddit").setView(subredditText).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        subreddit = subredditText.getText().toString();
                        mSpinnerAdapter.add("subreddit");
                        actionBar.setSelectedNavigationItem(5);
                        makeGallery();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                return true;
            case R.id.menuSortPopularity:
                sort = "viral";
                break;
            case R.id.menuSortNewest:
                sort = "time";
                break;
            case R.id.menuSortTop:
                sort = "top";
                break;
            case R.id.menuSortDay:
                sort = "top";
                window = "day";
                break;
            case R.id.menuSortWeek:
                sort = "top";
                window = "week";
                break;
            case R.id.menuSortMonth:
                sort = "top";
                window = "month";
                break;
            case R.id.menuSortYear:
                sort = "top";
                window = "year";
                break;
            case R.id.menuSortAll:
                sort = "top";
                window = "all";
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        makeGallery();
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        page = 0;
        subreddit = "pics";
        memeType = "top";
        gallery = "hot";
        sort = "viral";
        window = "day";
        setupActionBar(false);
        urls = new ArrayList<String>();
        ids = new ArrayList<JSONObject>();
        View view = inflater.inflate(R.layout.image_layout, container, false);
        GridView gridview = (GridView) view;
        imageAdapter = new ImageAdapter(view.getContext());
        gridview.setAdapter(imageAdapter);
        gridview.setOnItemClickListener(new GridItemClickListener());
        return gridview;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if(sort.equals("viral") && !gallery.equals("top"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setChecked(true);
        else if(sort.equals("time") && !gallery.equals("top"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortNewest).setChecked(true);
        else if(window.equals("day"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setChecked(true);
        else if(window.equals("week"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setChecked(true);
        else if(window.equals("month"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setChecked(true);
        else if(window.equals("year"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setChecked(true);
        else if(window.equals("all"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setChecked(true);

        if (gallery.equals("hot") || gallery.equals("user")) {
            menu.findItem(R.id.action_sort).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
            //menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(true);
        } else if (gallery.equals("top")) {
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(false);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortNewest).setVisible(false);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setVisible(true);
        } else if (gallery.equals("memes")) {
            //menu.findItem(R.id.action_sort).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(true);
            //menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(true);
            if(sort.equals("top"))
            {
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setVisible(true);
            }
        } else if (gallery.equals("random")) {
            menu.findItem(R.id.action_sort).setVisible(false);
        } else if (gallery.equals("subreddit")) {
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(false);
            //menu.findItem(R.id.action_sort).setVisible(true);
            if(sort.equals("top"))
            {
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setVisible(true);
            }
        }
    }

    private void makeGallery() {
        urls = new ArrayList<String>();
        ids = new ArrayList<JSONObject>();
        imageAdapter.notifyDataSetChanged();
        MainActivity activity = (MainActivity) getActivity();
        activity.invalidateOptionsMenu();
        async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                ActionBar actionBar = activity.getActionBar();
                Token accessKey = activity.getAccessToken();
                Log.d("Key", accessKey.getToken());
                JSONObject imagesData = new JSONObject();
                if (gallery.equals("hot") || gallery.equals("top") || gallery.equals("user")) {
                    imagesData = activity.makeGetCall("3/gallery/" + gallery + "/" + sort + "/" + window + "/" + page);
                } else if (gallery.equals("memes")) {
                    imagesData = activity.makeGetCall("3/gallery/g/memes/" + sort + "/" + window + "/" + page);
                } else if (gallery.equals("random")) {
                    imagesData = activity.makeGetCall("3/gallery/random/random/" + page);
                } else if (gallery.equals("subreddit")) {
                    Log.d("SORT", sort);
                    imagesData = activity.makeGetCall("3/gallery/r/" + subreddit + "/" + sort + "/" + window + "/" + page);
                }
                try {
                    Log.d("URI", imagesData.toString());
                    urls = new ArrayList<String>();
                    ids = new ArrayList<JSONObject>();
                    JSONArray imageArray = imagesData.getJSONArray("data");
                    int imageLength = Math.min(30, imageArray.length());
                    for (int i = 0; i < imageLength; i++) {
                        JSONObject imageData = imageArray.getJSONObject(i);
                        Log.d("Data", imageData.toString());
                        if (imageData.has("is_album") && imageData.getBoolean("is_album"))
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
        if(gallery.equals("subreddit"))
            setupActionBar(true);
        else
            setupActionBar(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        async.cancel(true);
    }

    private void setupActionBar(boolean subreddit) {
        MainActivity activity = (MainActivity) getActivity();
        actionBar = activity.getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        Resources res = activity.getResources();
        List<CharSequence> options = new ArrayList(Arrays.asList(res.getStringArray(R.array.galleryOptions)));
        mSpinnerAdapter = new ArrayAdapter<CharSequence>(activity, android.R.layout.simple_spinner_dropdown_item
              ,options);
        ActionBar.OnNavigationListener mNavigationCallback = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int i, long l) {
                page = 0;
                switch (i) {
                    case 0:
                        gallery = "hot";
                        sort = "viral";
                        break;
                    case 1:
                        gallery = "top";
                        sort = "day";
                        break;
                    case 2:
                        gallery = "user";
                        sort = "viral";
                        break;
                    case 3:
                        gallery = "memes";
                        sort = "viral";
                        break;
                    case 4:
                        gallery = "random";
                        break;
                }
                if(mSpinnerAdapter.getCount() > 5 && !gallery.equals("subreddit"))
                    mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
                Log.d("URI", gallery);
                Log.d("URI", "" + i);
                Log.d("URI", "" + l);
                makeGallery();
                return true;
            }
        };
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, mNavigationCallback);
    }
}