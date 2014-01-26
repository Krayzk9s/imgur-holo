package com.krayzk9s.imgurholo.ui;

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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.dialogs.SubredditDialogFragment;
import com.krayzk9s.imgurholo.tools.GetData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class GalleryFragment extends ImagesFragment implements GetData {

    private String sort;
    private String gallery;
    private int page;
    private String subreddit;
    private String window;
    private SpinnerAdapter mSpinnerAdapter;
    private int selectedIndex;
    private SearchView mSearchView;
    private MenuItem searchItem;
    private String search;
    private CharSequence spinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        final SharedPreferences settings = activity.getApiCall().settings;
        isGridView = settings.getString("GalleryLayout", getString(R.string.card_view)).equals(getString(R.string.grid_view));
        if (savedInstanceState != null) {
            Log.d("Restoring state", "restoring");
            gallery = savedInstanceState.getString("gallery");
            sort = savedInstanceState.getString("sort");
            window = savedInstanceState.getString("window");
            subreddit = savedInstanceState.getString(getResources().getString(R.string.subreddit));
            search = savedInstanceState.getString("search");
            selectedIndex = savedInstanceState.getInt("selectedIndex");
            spinner = savedInstanceState.getCharSequence("spinner");
			imageCall = savedInstanceState.getString("imageCall");
        } else {
            subreddit = "pics";
            gallery = settings.getString("DefaultGallery", getResources().getString(R.string.viral));
            ArrayList<String> galleryOptions = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.galleryOptions)));
            sort = getResources().getString(R.string.viralsort);
            window = getResources().getString(R.string.day);
            selectedIndex = galleryOptions.indexOf(gallery);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        setupActionBar();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        if (activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
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
        menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortBest).setVisible(false);
        menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
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
                gallery = getResources().getString(R.string.subreddit);
                sort = getResources().getString(R.string.newsort);
                final EditText subredditText = new EditText(activity);
                subredditText.setSingleLine();
                SubredditDialogFragment subredditDialogFragment = new SubredditDialogFragment();
                subredditDialogFragment.show(getActivity().getSupportFragmentManager(), "TAG");
                subredditDialogFragment.setTargetFragment(this, 0);

                return true;
            case R.id.menuSortPopularity:
                sort = getResources().getString(R.string.viralsort);
                break;
            case R.id.menuSortNewest:
                sort = getResources().getString(R.string.newsort);
                break;
            case R.id.menuSortTop:
                sort = getResources().getString(R.string.topsort);
                break;
            case R.id.menuSortDay:
                sort = getResources().getString(R.string.topsort);
                window = getResources().getString(R.string.day);
                break;
            case R.id.menuSortWeek:
                sort = getResources().getString(R.string.topsort);
                window = getResources().getString(R.string.week);
                break;
            case R.id.menuSortMonth:
                sort = getResources().getString(R.string.topsort);
                window = getResources().getString(R.string.month);
                break;
            case R.id.menuSortYear:
                sort = getResources().getString(R.string.topsort);
                window = getResources().getString(R.string.year);
                break;
            case R.id.menuSortAll:
                sort = getResources().getString(R.string.topsort);
                window = getResources().getString(R.string.all);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        makeGallery();
        return true;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (sort == null || (sort.equals(getResources().getString(R.string.viralsort)) && !gallery.equals(getResources().getString(R.string.top))))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setChecked(true);
        else if (sort.equals(getResources().getString(R.string.newsort)) && !gallery.equals(getResources().getString(R.string.top)))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortNewest).setChecked(true);
        else if (window.equals(getResources().getString(R.string.day)))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setChecked(true);
        else if (window.equals(getResources().getString(R.string.week)))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setChecked(true);
        else if (window.equals(getResources().getString(R.string.month)))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setChecked(true);
        else if (window.equals(getResources().getString(R.string.year)))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setChecked(true);
        else if (window.equals(getResources().getString(R.string.all)))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setChecked(true);

        if (gallery == null || gallery.equals(getResources().getString(R.string.viral)) || gallery.equals(getResources().getString(R.string.user))) {
            menu.findItem(R.id.action_sort).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
            //menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(true);
        } else if (gallery.equals(getResources().getString(R.string.top))) {
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(false);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortNewest).setVisible(false);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setVisible(true);
        } else if (gallery.equals(getResources().getString(R.string.memes))) {
            //menu.findItem(R.id.action_sort).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(true);
            //menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(true);
            if (sort.equals(getResources().getString(R.string.topsort))) {
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setVisible(true);
            }
        } else if (gallery.equals(getResources().getString(R.string.random))) {
            menu.findItem(R.id.action_sort).setVisible(false);
        } else if (gallery.equals(getResources().getString(R.string.subreddit))) {
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(false);
            //menu.findItem(R.id.action_sort).setVisible(true);
            if (sort.equals(getResources().getString(R.string.topsort))) {
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setVisible(true);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null)
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }

    @Override
    protected void makeGallery() {
        if (gallery.equals(getResources().getString(R.string.viral)) || gallery.equals("hot")) { //hot is for legacy
            imageCall = "3/gallery/hot/" + sort + "/" + window;
        } else if (gallery.equals(getResources().getString(R.string.top))) {
            imageCall = "3/gallery/top/" + sort + "/" + window;
        } else if (gallery.equals(getResources().getString(R.string.user))) {
            imageCall = "3/gallery/user/" + sort + "/" + window;
        } else if (gallery.equals(getResources().getString(R.string.memes))) {
            imageCall = "3/gallery/g/memes/" + sort + "/" + window;
        } else if (gallery.equals(getResources().getString(R.string.random))) {
            imageCall = "3/gallery/random/random/";
        } else if (gallery.equals(getResources().getString(R.string.subreddit))) {
            imageCall = "3/gallery/r/" + subreddit + "/" + sort + "/" + window;
        } else if (gallery.equals("search")) {
            imageCall = "3/gallery/search?q=" + search;
        }
        super.makeGallery();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current state
        savedInstanceState.putString("gallery", gallery);
        savedInstanceState.putString("sort", sort);
        savedInstanceState.putString("window", window);
        savedInstanceState.putString(getResources().getString(R.string.subreddit), subreddit);
        savedInstanceState.putString("search", search);
		savedInstanceState.putString("imageCall", imageCall);
        savedInstanceState.putInt("page", page);
        savedInstanceState.putInt("selectedIndex", selectedIndex);
        if (mSpinnerAdapter != null && mSpinnerAdapter.getCount() > 5)
            savedInstanceState.putCharSequence("spinner", mSpinnerAdapter.getItem(5));
        else
            savedInstanceState.putCharSequence("spinner", null);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private void setupActionBar() {
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        Resources res = activity.getResources();
        ArrayList options = new ArrayList(Arrays.asList(res.getStringArray(R.array.galleryOptions)));
        mSpinnerAdapter = new SpinnerAdapter(activity, options);
        if (spinner != null)
            mSpinnerAdapter.add(spinner);
        ActionBar.OnNavigationListener mNavigationCallback = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int i, long l) {
                String newGallery = "";
                switch (i) {
                    case 0:
                        newGallery = getResources().getString(R.string.viral);
                        sort = getResources().getString(R.string.viralsort);
                        break;
                    case 1:
                        newGallery = getResources().getString(R.string.top);
                        sort = getResources().getString(R.string.day);
                        break;
                    case 2:
                        newGallery = getResources().getString(R.string.user);
                        sort = getResources().getString(R.string.viralsort);
                        break;
                    case 3:
                        newGallery = getResources().getString(R.string.memes);
                        sort = getResources().getString(R.string.viralsort);
                        break;
                    case 4:
                        newGallery = getResources().getString(R.string.random);
                        break;
                }
                if (newGallery.equals(gallery))
                    return true;
                else if (i < 5)
                    gallery = newGallery;
                selectedIndex = i;
                if (mSpinnerAdapter.getCount() > 5 && !gallery.equals(getResources().getString(R.string.subreddit)) && !gallery.equals("search")) {
                    mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
                    subreddit = null;
                    search = null;
                }
                page = 0;
                if(i != 5)
                    makeGallery();
                return true;
            }
        };
        Log.d("gallery", gallery);
        if (selectedIndex == 5) {
            if (subreddit != null)
                mSpinnerAdapter.add("/r/" + subreddit);
            else
                mSpinnerAdapter.add("search: " + search);
        }
        actionBar.setSelectedNavigationItem(selectedIndex);
        Log.d("Setting Item", "Setting Item");
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, mNavigationCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case 0:
                if(resultCode != Activity.RESULT_OK)
                    return;
                subreddit = data.getStringExtra("subreddit");
                mSpinnerAdapter.add("/r/" + subreddit);
                search = null;
                if (mSpinnerAdapter.getCount() > 6) {
                    mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
                }
                mSpinnerAdapter.notifyDataSetChanged();
                actionBar.setSelectedNavigationItem(5);
                makeGallery();
        }
    }

    public class SpinnerAdapter extends ArrayAdapter<CharSequence> {
        final Context mContext;

        public SpinnerAdapter(Context context, List<CharSequence> options) {
            super(context, android.R.layout.simple_spinner_dropdown_item, options);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater =
                    (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.gallery_spinner, null);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.action_bar_title);
                holder.subtitle = (TextView) convertView.findViewById(R.id.action_bar_subtitle);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.title.setText("Gallery");
            holder.subtitle.setText(getItem(position));
            return convertView;
        }

        class ViewHolder {
            TextView title;
            TextView subtitle;
        }
    }

}