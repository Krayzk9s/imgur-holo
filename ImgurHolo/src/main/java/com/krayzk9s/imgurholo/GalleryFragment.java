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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Kurt Zimmer on 7/23/13.
 */
public class GalleryFragment extends Fragment {

    private ArrayList<String> urls;
    private ArrayList<JSONParcelable> ids;
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
    int firstPass = 0;
    int selectedIndex;
    JSONObject imagesData;
    SearchView mSearchView;
    MenuItem searchItem;
    String search;
    CharSequence spinner;

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
        MainActivity activity = (MainActivity)getActivity();
        if(activity.theme == activity.HOLO_LIGHT)
            inflater.inflate(R.menu.main, menu);
        else
            inflater.inflate(R.menu.main_dark, menu);
        menu.findItem(R.id.action_sort).setVisible(true);
        menu.findItem(R.id.subreddit).setVisible(true);
        menu.findItem(R.id.action_upload).setVisible(false);

        menu.findItem(R.id.action_search).setVisible(true);

        searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchItem.getActionView();
       SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                // Do nothing
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d("searching", mSearchView.getQuery() + "");
                mSpinnerAdapter.add("search: " + mSearchView.getQuery());
                if (mSpinnerAdapter.getCount() > 6)
                    mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
                gallery = "search";
                search = mSearchView.getQuery() + "";
                searchItem.collapseActionView();
                actionBar.setSelectedNavigationItem(5);
                makeGallery();
                return true;
            }
        };
        mSearchView.setOnQueryTextListener(queryTextListener);
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
                        mSpinnerAdapter.add("/r/" + subreddit);
                        if (mSpinnerAdapter.getCount() > 6)
                            mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
                        mSpinnerAdapter.notifyDataSetChanged();
                        actionBar.setSelectedNavigationItem(5);
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
        boolean newData = false;
        if (savedInstanceState != null) {
            gallery = savedInstanceState.getString("gallery");
            sort = savedInstanceState.getString("sort");
            window = savedInstanceState.getString("window");
            subreddit = savedInstanceState.getString("subreddit");
            urls = savedInstanceState.getStringArrayList("urls");
            try {
                ids = savedInstanceState.getParcelableArrayList("ids");
            } catch (Exception e) {
                Log.e("Error!", e.toString());
            }
            page = savedInstanceState.getInt("page");
            selectedIndex = savedInstanceState.getInt("selectedIndex");
            spinner = savedInstanceState.getCharSequence("spinner");
        } else if (subreddit == null) {
            page = 0;
            subreddit = "pics";
            memeType = "top";
            gallery = "hot";
            sort = "viral";
            window = "day";
            urls = new ArrayList<String>();
            ids = new ArrayList<JSONParcelable>();
            selectedIndex = 0;
            newData = true;
        }
        Log.d("NOT HERE EITHER", gallery);
        View view = inflater.inflate(R.layout.image_layout, container, false);
        GridView gridview = (GridView) view.findViewById(R.id.grid_layout);
        imageAdapter = new ImageAdapter(view.getContext());
        gridview.setAdapter(imageAdapter);
        gridview.setOnItemClickListener(new GridItemClickListener());
        gridview.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }
            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int lastInScreen = firstVisibleItem + visibleItemCount;
                if ((lastInScreen == totalItemCount) && urls != null && urls.size() > 0) {
                    try {
                        Log.d("Extending", "Getting more images!");
                        JSONArray imageArray = imagesData.getJSONArray("data");
                        int imageLength = Math.min(urls.size() + 30, imageArray.length());
                        for (int i = urls.size(); i < imageLength; i++) {
                            JSONObject imageData = imageArray.getJSONObject(i);
                            Log.d("Data", imageData.toString());
                            if (imageData.has("is_album") && imageData.getBoolean("is_album")) {
                                if (!urls.contains("http://imgur.com/" + imageData.getString("cover") + "m.png"))
                                    urls.add("http://imgur.com/" + imageData.getString("cover") + "m.png");
                            }
                            else {
                                if (!urls.contains("http://imgur.com/" + imageData.getString("id") + "m.png"))
                                    urls.add("http://imgur.com/" + imageData.getString("id") + "m.png");
                            }
                            JSONParcelable dataParcel = new JSONParcelable();
                            dataParcel.setJSONObject(imageData);
                            ids.add(dataParcel);
                            imageAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        Log.e("Error!", e.toString());
                    }
                }
            }
        });
        if(!newData)
            firstPass = 0;
        setupActionBar();
        if(newData)
            makeGallery();
        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (sort == null || (sort.equals("viral") && !gallery.equals("top")))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setChecked(true);
        else if (sort.equals("time") && !gallery.equals("top"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortNewest).setChecked(true);
        else if (window.equals("day"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setChecked(true);
        else if (window.equals("week"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setChecked(true);
        else if (window.equals("month"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setChecked(true);
        else if (window.equals("year"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setChecked(true);
        else if (window.equals("all"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setChecked(true);

        if (gallery == null || gallery.equals("hot") || gallery.equals("user")) {
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
            if (sort.equals("top")) {
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
            if (sort.equals("top")) {
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
        ids = new ArrayList<JSONParcelable>();
        imageAdapter.notifyDataSetChanged();
        MainActivity activity = (MainActivity) getActivity();
        activity.invalidateOptionsMenu();
        async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                imagesData = new JSONObject();
                if (gallery.equals("hot") || gallery.equals("top") || gallery.equals("user")) {
                    imagesData = activity.makeGetCall("3/gallery/" + gallery + "/" + sort + "/" + window + "/" + page);
                } else if (gallery.equals("memes")) {
                    imagesData = activity.makeGetCall("3/gallery/g/memes/" + sort + "/" + window + "/" + page);
                } else if (gallery.equals("random")) {
                    imagesData = activity.makeGetCall("3/gallery/random/random/" + page);
                } else if (gallery.equals("subreddit")) {
                    imagesData = activity.makeGetCall("3/gallery/r/" + subreddit + "/" + sort + "/" + window + "/" + page);
                } else if (gallery.equals("search")) {
                    imagesData = activity.makeGetCall("3/gallery/search?q=" + search);
                }
                try {
                    Log.d("URI", imagesData.toString());
                    urls = new ArrayList<String>();
                    ids = new ArrayList<JSONParcelable>();
                    JSONArray imageArray = imagesData.getJSONArray("data");
                    int imageLength = Math.min(30, imageArray.length());
                    for (int i = 0; i < imageLength; i++) {
                        JSONObject imageData = imageArray.getJSONObject(i);
                        Log.d("Data", imageData.toString());
                        if (imageData.has("is_album") && imageData.getBoolean("is_album")) {
                            if (!urls.contains("http://imgur.com/" + imageData.getString("cover") + "m.png"))
                                urls.add("http://imgur.com/" + imageData.getString("cover") + "m.png");
                        }
                        else {
                            if (!urls.contains("http://imgur.com/" + imageData.getString("id") + "m.png"))
                                urls.add("http://imgur.com/" + imageData.getString("id") + "m.png");
                        }
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
                Log.d("returning", urls.size() + "");
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
            ImageView imageView = new SquareImageView(mContext);
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
        try {
            if (id.has("is_album") && id.getBoolean("is_album")) {
                ImagesFragment fragment = new ImagesFragment();
                fragment.setImageCall(id.getString("id"), "3/album/" + id.getString("id"), id);
                MainActivity activity = (MainActivity) getActivity();
                activity.changeFragment(fragment);
            } else {
                SingleImageFragment fragment = new SingleImageFragment();
                fragment.setGallery(true);
                fragment.setParams(id);
                MainActivity activity = (MainActivity) getActivity();
                activity.changeFragment(fragment);
            }
        } catch (Exception e) {
            Log.e("Error!", e.toString());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MainActivity activity = (MainActivity) getActivity();
        ActionBar actionBar = activity.getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        if (async != null)
            async.cancel(true);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putString("gallery", gallery);
        savedInstanceState.putString("sort", sort);
        savedInstanceState.putString("window", window);
        savedInstanceState.putString("subreddit", subreddit);
        savedInstanceState.putStringArrayList("urls", urls);
        savedInstanceState.putParcelableArrayList("ids", ids);
        savedInstanceState.putInt("page", page);
        savedInstanceState.putInt("selectedIndex", selectedIndex);
        if(mSpinnerAdapter != null && mSpinnerAdapter.getCount() > 5)
            savedInstanceState.putCharSequence("spinner", mSpinnerAdapter.getItem(5));
        else
            savedInstanceState.putCharSequence("spinner", null);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private void setupActionBar() {
        MainActivity activity = (MainActivity) getActivity();
        actionBar = activity.getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        Resources res = activity.getResources();
        List<CharSequence> options = new ArrayList(Arrays.asList(res.getStringArray(R.array.galleryOptions)));
        mSpinnerAdapter = new ArrayAdapter<CharSequence>(activity, android.R.layout.simple_spinner_dropdown_item
                , options);
        if(spinner != null)
            mSpinnerAdapter.add(spinner);
        ActionBar.OnNavigationListener mNavigationCallback = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int i, long l) {
                Log.d("URI", String.valueOf(firstPass));
                page = 0;
                if (firstPass > 1) {
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
                    selectedIndex = i;
                    if (mSpinnerAdapter.getCount() > 5 && !gallery.equals("subreddit") && !gallery.equals("search"))
                        mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
                    Log.d("URI", gallery);
                    Log.d("URI", "" + i);
                    makeGallery();
                } else {
                    Log.d("URI4", String.valueOf(firstPass));
                    Log.d("index", String.valueOf(selectedIndex));
                        firstPass += 2;
                    Log.d("URI2", String.valueOf(firstPass));
                }
                Log.d("URI3", String.valueOf(firstPass));
                return true;
            }
        };
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, mNavigationCallback);
        actionBar.setSelectedNavigationItem(selectedIndex);
    }
}