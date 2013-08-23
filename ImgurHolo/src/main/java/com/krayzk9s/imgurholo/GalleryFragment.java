package com.krayzk9s.imgurholo;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

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
    int lastInView = -1;
    TextView errorText;
    GridView gridview;
    int oldwidth = 0;

    public GalleryFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        MainActivity activity = (MainActivity)getActivity();
        if(activity.theme.equals(activity.HOLO_LIGHT))
            inflater.inflate(R.menu.main, menu);
        else
            inflater.inflate(R.menu.main_dark, menu);
        menu.findItem(R.id.action_sort).setVisible(true);
        menu.findItem(R.id.subreddit).setVisible(true);
        menu.findItem(R.id.action_upload).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);

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
                subreddit = null;
                if (mSpinnerAdapter.getCount() > 6) {
                    mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
                }
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
        page = 0;
        switch (item.getItemId()) {
            case R.id.action_sort:
                Log.d("sorting", "sorting");
                return true;
            case R.id.action_refresh:
                page = 0;
                makeGallery();
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
                        search = null;
                        if (mSpinnerAdapter.getCount() > 6) {
                            mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
                        }
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
        final MainActivity activity = (MainActivity) getActivity();
        if (savedInstanceState != null) {
            gallery = savedInstanceState.getString("gallery");
            sort = savedInstanceState.getString("sort");
            window = savedInstanceState.getString("window");
            subreddit = savedInstanceState.getString("subreddit");
            search = savedInstanceState.getString("search");
            urls = savedInstanceState.getStringArrayList("urls");
            try {
                ids = savedInstanceState.getParcelableArrayList("ids");
            } catch (Exception e) {
                Log.e("Error!", e.toString());
            }
            page = savedInstanceState.getInt("page");
            selectedIndex = savedInstanceState.getInt("selectedIndex");
            spinner = savedInstanceState.getCharSequence("spinner");
        } else if (subreddit == null && search == null) {
            SharedPreferences settings = activity.getSettings();
            page = 0;
            subreddit = "pics";
            memeType = "top";
            gallery = settings.getString("DefaultGallery", "hot");
            ArrayList<String> galleryOptions = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.galleryOptions)));
            sort = "viral";
            window = "day";
            urls = new ArrayList<String>();
            ids = new ArrayList<JSONParcelable>();
            selectedIndex = galleryOptions.indexOf(gallery);
            newData = true;
        }
        Log.d("NOT HERE EITHER", gallery);
        View view = inflater.inflate(R.layout.image_layout, container, false);
        errorText = (TextView) view.findViewById(R.id.error);
        gridview = (GridView) view.findViewById(R.id.grid_layout);

        SharedPreferences settings = activity.getSettings();
        gridview.setColumnWidth(activity.dpToPx(Integer.parseInt(settings.getString("IconSize", "90"))));
        imageAdapter = new ImageAdapter(view.getContext());
        gridview.setAdapter(imageAdapter);
        gridview.setOnItemClickListener(new GridItemClickListener());
        gridview.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if(imageAdapter.getNumColumns() == 0 || gridview.getWidth() != oldwidth)
                            setNumColumns();
                    }
                });
        gridview.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }
            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(lastInView == -1)
                    lastInView = firstVisibleItem;
                else if (lastInView > firstVisibleItem) {
                    actionBar.show();
                    lastInView = firstVisibleItem;
                }
                else if (lastInView < firstVisibleItem) {
                    actionBar.hide();
                    lastInView = firstVisibleItem;
                }
                int lastInScreen = firstVisibleItem + visibleItemCount;
                if ((lastInScreen == totalItemCount) && urls != null && urls.size() > 0) {
                    try {
                        Log.d("Extending", "Getting more images!");
                        JSONArray imageArray = imagesData.getJSONArray("data");
                        int imageLength = Math.min(urls.size() + 30, imageArray.length());
                        boolean loadedMore = false;
                        for (int i = urls.size(); i < imageLength; i++) {
                            loadedMore = true;
                            JSONObject imageData = imageArray.getJSONObject(i);
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
                        if(!loadedMore) {
                            if(!gallery.equals("search")) {
                                page += 1;
                                getImages();
                            }
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

    private void setNumColumns() {
        Log.d("Setting Columns", "Setting Columns");
        MainActivity activity = (MainActivity) getActivity();
        if(activity != null) {
            oldwidth = gridview.getWidth();
            SharedPreferences settings = activity.getSettings();
            Log.d("numColumnsWidth", gridview.getWidth()+"");
            Log.d("numColumnsIconWidth", activity.dpToPx((Integer.parseInt(settings.getString("IconSize", "90"))))+"");
            final int numColumns = (int) Math.floor(
                    gridview.getWidth() / (activity.dpToPx((Integer.parseInt(settings.getString("IconSize", "90")))) + activity.dpToPx(4)));
            if (numColumns > 0) {
                imageAdapter.setNumColumns(numColumns);
                if (BuildConfig.DEBUG) {
                    Log.d("NUMCOLS", "onCreateView - numColumns set to " + numColumns);
                }
                imageAdapter.notifyDataSetChanged();
            }
        }
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
        getImages();
    }

    private void getImages() {
        errorText.setVisibility(View.GONE);
        async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                imagesData = new JSONObject();
                Log.d("imagesData", "loading");
                if (gallery.equals("hot") || gallery.equals("top") || gallery.equals("user")) {
                    imagesData = activity.makeCall("3/gallery/" + gallery + "/" + sort + "/" + window + "/" + page, "get", null);
                } else if (gallery.equals("memes")) {
                    imagesData = activity.makeCall("3/gallery/g/memes/" + sort + "/" + window + "/" + page, "get", null);
                } else if (gallery.equals("random")) {
                    imagesData = activity.makeCall("3/gallery/random/random/" + page, "get", null);
                } else if (gallery.equals("subreddit")) {
                    imagesData = activity.makeCall("3/gallery/r/" + subreddit + "/" + sort + "/" + window + "/" + page, "get", null);
                } else if (gallery.equals("search")) {
                    imagesData = activity.makeCall("3/gallery/search?q=" + search, "get", null);
                }
                Log.d("imagesData", "checking");
                if(imagesData == null) {
                    return null;
                }
                Log.d("imagesData", "failed");
                try {
                    Log.d("URI", imagesData.toString());
                    JSONArray imageArray = imagesData.getJSONArray("data");
                    int imageLength = Math.min(30, imageArray.length());
                    for (int i = 0; i < imageLength; i++) {
                        JSONObject imageData = imageArray.getJSONObject(i);
                        Log.d("Data", imageData.toString());
                        if (imageData.has("is_album") && imageData.getBoolean("is_album")) {
                            if (!urls.contains("http://imgur.com/" + imageData.getString("cover") + "m.png")) {
                                urls.add("http://imgur.com/" + imageData.getString("cover") + "m.png");
                                JSONParcelable dataParcel = new JSONParcelable();
                                dataParcel.setJSONObject(imageData);
                                ids.add(dataParcel);
                            }
                        }
                        else {
                            if (!urls.contains("http://imgur.com/" + imageData.getString("id") + "m.png"))
                            {
                                urls.add("http://imgur.com/" + imageData.getString("id") + "m.png");
                                JSONParcelable dataParcel = new JSONParcelable();
                                dataParcel.setJSONObject(imageData);
                                ids.add(dataParcel);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("Error!", e.toString());
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                if(imagesData != null)
                    imageAdapter.notifyDataSetChanged();
                else
                    errorText.setVisibility(View.VISIBLE);
            }
        };
        async.execute();
    }

    public class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private int mNumColumns;

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }

        public int getNumColumns() {
            return mNumColumns;
        }

        public ImageAdapter(Context c) {
            mContext = c;
        }

        @Override
        public long getItemId(int position) {
            return position < mNumColumns ? 0 : position - mNumColumns;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (position < mNumColumns) ? 1 : 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public int getCount() {
            return urls.size()  + mNumColumns;
        }

        public Object getItem(int position) {
            return position < mNumColumns ?
                    null :urls.get(position);
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position < mNumColumns) {
                if (convertView == null) {
                    convertView = new View(mContext);
                }
                convertView.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, getActivity().getActionBar().getHeight()));
                return convertView;
            }
            else {
                ImageView imageView;
                if(convertView == null) {
                    imageView = new SquareImageView(mContext);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
                else {
                    imageView = (ImageView) convertView;
                }
                UrlImageViewHelper.setUrlDrawable(imageView, urls.get(position - mNumColumns));
                return imageView;
            }
        }

    }

    private class GridItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position - imageAdapter.getNumColumns());
        }
    }

    public void selectItem(int position) {
        JSONObject id = ids.get(position).getJSONObject();
        //try {
            //if (id.has("is_album") && id.getBoolean("is_album")) {
            //    ImagesFragment fragment = new ImagesFragment();
            //    fragment.setImageCall(id.getString("id"), "3/album/" + id.getString("id"), id);
            //    MainActivity activity = (MainActivity) getActivity();
            //    activity.changeFragment(fragment);
            //} else {
                ImagePager pager = new ImagePager(position);
                /*SingleImageFragment fragment = new SingleImageFragment();
                fragment.setGallery(true);
                fragment.setParams(id);*/
                pager.setImageData(new ArrayList<JSONParcelable>(ids));
                MainActivity activity = (MainActivity) getActivity();
                activity.changeFragment(pager);
           // }
        //} catch (Exception e) {
        //    Log.e("Error!", e.toString());
        //}
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
        savedInstanceState.putString("search", search);
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
                    if (mSpinnerAdapter.getCount() > 5 && !gallery.equals("subreddit") && !gallery.equals("search")) {
                        mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
                        subreddit = null;
                        search = null;
                    }
                    Log.d("URI", gallery);
                    Log.d("URI", "" + i);
                    page = 0;
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
        Log.d("gallery", gallery);
        if(selectedIndex == 5) {
            if(subreddit != null)
                mSpinnerAdapter.add("/r/" + subreddit);
            else
                mSpinnerAdapter.add("search: " + search);
        }
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, mNavigationCallback);
        actionBar.setSelectedNavigationItem(selectedIndex);
    }

}