package com.krayzk9s.imgurholo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Kurt Zimmer on 7/22/13.
 */
public class ImagesFragment extends Fragment {

    public boolean selecting = false;
    ImageAdapter imageAdapter;
    AsyncTask<Void, Void, Void> async;
    String imageCall;
    GridView gridview;
    MultiChoiceModeListener multiChoiceModeListener;
    ArrayList<String> intentReturn;
    String albumId;
    JSONObject galleryAlbumData;
    JSONObject imageParam;
    JSONObject imagesData;
    TextView noImageView;
    int page;
    boolean gettingImages = false;
    int lastInView = -1;
    private ArrayList<String> urls;
    private ArrayList<JSONParcelable> ids;

    public ImagesFragment() {
        page = 0;
    }

    public void setImageCall(String _Album, String _imageCall, JSONObject _data) {
        albumId = _Album;
        imageCall = _imageCall;
        galleryAlbumData = _data;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity.theme == activity.HOLO_LIGHT)
            inflater.inflate(R.menu.main, menu);
        else
            inflater.inflate(R.menu.main_dark, menu);
        menu.findItem(R.id.action_upload).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);
        menu.findItem(R.id.action_download).setVisible(true);
        if (albumId != null && galleryAlbumData == null) {
            menu.findItem(R.id.action_new).setVisible(true);
        }
        if (albumId != null && galleryAlbumData != null) {
            menu.findItem(R.id.action_comments).setVisible(true);
        }
        if (albumId != null) {
            menu.findItem(R.id.action_copy).setVisible(true);
            menu.findItem(R.id.action_share).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        final MainActivity activity = (MainActivity) getActivity();
        Toast toast;
        int duration;
        switch (item.getItemId()) {
            case R.id.action_download:
                duration = Toast.LENGTH_SHORT;
                toast = Toast.makeText(activity, "Downloading " + urls.size() + " images! This may take a while...", duration);
                toast.show();
                AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {
                            for (int i = 0; i < urls.size(); i++) {
                                String type = ids.get(i).getJSONObject().getString("type").split("/")[1];
                                Log.d("URL", "http://i.imgur.com/" + ids.get(i).getJSONObject().getString("id") + "." + type);
                                Log.d("IDs", ids.get(i).getJSONObject().getString("id"));
                                URL url = new URL("http://i.imgur.com/" + ids.get(i).getJSONObject().getString("id") + "." + type);
                                File file = new File(android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + ids.get(i).getJSONObject().getString("id") + "." + type);
                                URLConnection ucon = url.openConnection();
                                InputStream is = ucon.getInputStream();
                                BufferedInputStream bis = new BufferedInputStream(is);
                                ByteArrayBuffer baf = new ByteArrayBuffer(500);
                                int current = 0;
                                while ((current = bis.read()) != -1) {
                                    baf.append((byte) current);
                                }
                                FileOutputStream fos = new FileOutputStream(file);
                                fos.write(baf.toByteArray());
                                fos.close();
                            }
                        } catch (Exception e) {
                            Log.e("Error!", e.toString());
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                                + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))));
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast;
                        toast = Toast.makeText(activity, "Downloaded All!", duration);
                        toast.show();
                    }
                };
                async.execute();
                return true;
            case R.id.action_refresh:
                urls = new ArrayList<String>();
                ids = new ArrayList<JSONParcelable>();
                page = 0;
                imageAdapter.notifyDataSetChanged();
                getImages();
                return true;
            case R.id.action_copy:
                try {
                    ClipboardManager clipboard = (ClipboardManager)
                            activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("imgur Link", "http://imgur.com/a/" + albumId);
                    clipboard.setPrimaryClip(clip);
                    duration = Toast.LENGTH_SHORT;
                    toast = Toast.makeText(activity, "Copied!", duration);
                    toast.show();
                } catch (Exception e) {
                    Log.e("Error!", e.toString());
                }
                return true;
            case R.id.action_share:
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                try {
                    intent.putExtra(Intent.EXTRA_TEXT, "http://imgur.com/a/" + albumId);
                } catch (Exception e) {
                    Log.e("Error!", "bad link to share");
                }
                startActivity(intent);
                return true;
            case R.id.action_new:
                Intent i = new Intent(this.getActivity().getApplicationContext(), ImageSelectActivity.class);
                startActivityForResult(i, 1);
                //select image
                return true;
            case R.id.action_comments:
                async = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        MainActivity activity = (MainActivity) getActivity();
                        try {
                            imageParam = activity.makeCall("3/image/" + galleryAlbumData.getString("cover"), "get", null).getJSONObject("data");
                            Log.d("Params", imageParam.toString());
                            galleryAlbumData.put("width", imageParam.getInt("width"));
                            galleryAlbumData.put("type", imageParam.getString("type"));
                            galleryAlbumData.put("height", imageParam.getInt("height"));
                            galleryAlbumData.put("size", imageParam.getInt("size"));
                            Log.d("Params w/ new data", galleryAlbumData.toString());
                        } catch (Exception e) {
                            Log.e("Error!", "bad single image call" + e.toString());
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        if(galleryAlbumData != null) {
                            SingleImageFragment fragment = new SingleImageFragment();
                            fragment.setParams(galleryAlbumData);
                            fragment.setGallery(true);
                            MainActivity activity = (MainActivity) getActivity();
                            activity.changeFragment(fragment);
                        }
                    }
                };
                async.execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        AddImagesToAlbum imageAsync;
        ArrayList<String> imageIds;
        Log.d("requestcode", requestCode + "");
        Object bundle = data.getExtras().get("data");
        Log.d("HELO", bundle.getClass().toString());
        switch (requestCode) {
            case 1:
                super.onActivityResult(requestCode, resultCode, data);
                imageIds = data.getStringArrayListExtra("data");
                Log.d("Ids!", imageIds.toString());
                imageAsync = new AddImagesToAlbum(imageIds, true);
                imageAsync.execute();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (urls == null) {
            urls = new ArrayList<String>();
            ids = new ArrayList<JSONParcelable>();
        }
        View view = inflater.inflate(R.layout.image_layout, container, false);
        gridview = (GridView) view.findViewById(R.id.grid_layout);
        noImageView = (TextView) view.findViewById(R.id.no_images);
        imageAdapter = new ImageAdapter(view.getContext());
        gridview.setAdapter(imageAdapter);
        final MainActivity activity = (MainActivity) getActivity();
        SharedPreferences settings = activity.getSettings();
        gridview.setColumnWidth(activity.dpToPx(settings.getInt("IconSize", 90)));
        gridview.setOnItemClickListener(new GridItemClickListener());
        gridview.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        multiChoiceModeListener = new MultiChoiceModeListener();
        gridview.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (imageAdapter.getNumColumns() == 0) {
                            SharedPreferences settings = activity.getSettings();
                            Log.d("numColumnsWidth", gridview.getWidth() + "");
                            Log.d("numColumnsIconWidth", activity.dpToPx((settings.getInt("IconSize", 90))) + "");
                            final int numColumns = (int) Math.floor(
                                    gridview.getWidth() / (activity.dpToPx((settings.getInt("IconSize", 90))) + activity.dpToPx(2)));
                            if (numColumns > 0) {
                                imageAdapter.setNumColumns(numColumns);
                                if (BuildConfig.DEBUG) {
                                    Log.d("NUMCOLS", "onCreateView - numColumns set to " + numColumns);
                                }
                                imageAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
        gridview.setMultiChoiceModeListener(multiChoiceModeListener);
        if (albumId == null) {
            gridview.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (lastInView == -1)
                        lastInView = firstVisibleItem;
                    else if (lastInView > firstVisibleItem) {
                        getActivity().getActionBar().show();
                        lastInView = firstVisibleItem;
                    } else if (lastInView < firstVisibleItem) {
                        getActivity().getActionBar().hide();
                        lastInView = firstVisibleItem;
                    }
                }
            });
        }
        if (savedInstanceState == null && urls.size() == 0) {
            getImages();

        } else if (savedInstanceState != null) {
            urls = savedInstanceState.getStringArrayList("urls");
            ids = savedInstanceState.getParcelableArrayList("ids");
        }

        return view;
    }

    private void getImages() {
        AsyncTask<Void, Void, Boolean> imageAsync = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                boolean changed = false;
                JSONArray imageArray = new JSONArray();
                do {
                    try {
                        changed = false;
                        MainActivity activity = (MainActivity) getActivity();
                        Log.d("call", imageCall + "/" + page);
                        if (activity != null)
                            imagesData = activity.makeCall(imageCall + "/" + page, "get", null);
                        else
                            return true;
                        Log.d("page", page + "");

                        if (imagesData.optJSONObject("data") != null)
                            imageArray = imagesData.getJSONObject("data").getJSONArray("images");
                        else
                            imageArray = imagesData.getJSONArray("data");
                        Log.d("single image array", imageArray.toString());
                        for (int i = 0; i < imageArray.length(); i++) {
                            JSONObject imageData = imageArray.getJSONObject(i);
                            Log.d("Data", imageData.toString());
                            if(imageCall.equals("3/account/me/likes") && !imageData.getBoolean("favorite"))
                                continue;
                            JSONParcelable dataParcel = new JSONParcelable();
                            dataParcel.setJSONObject(imageData);
                            if (imageData.has("is_album") && imageData.getBoolean("is_album") && !urls.contains("http://imgur.com/" + imageData.getString("cover") + "m.png")) {
                                urls.add("http://imgur.com/" + imageData.getString("cover") + "m.png");
                                ids.add(dataParcel);
                                changed = true;
                            } else if(!urls.contains("http://imgur.com/" + imageData.getString("id") + "m.png")) {
                                urls.add("http://imgur.com/" + imageData.getString("id") + "m.png");
                                ids.add(dataParcel);
                                changed = true;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Error!", "bad image array data" + e.toString());
                    }
                    page += 1;
                } while (imageArray.length() > 0 && changed);
                return changed;
            }

            @Override
            protected void onPostExecute(Boolean changed) {
                gettingImages = false;
                if (imageAdapter != null)
                    imageAdapter.notifyDataSetChanged();
                else if (urls.size() == 0 && noImageView != null)
                    noImageView.setVisibility(View.VISIBLE);
                else
                    gettingImages = true;
            }
        };
        imageAsync.execute();
    }

    public void selectItem(int position) {
        if (!selecting) {
            ImagePager imagePager = new ImagePager(position);
            ArrayList<JSONParcelable> idCopy = ids;
            imagePager.setImageData(idCopy);
            MainActivity activity = (MainActivity) getActivity();
            activity.changeFragment(imagePager);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (async != null)
            async.cancel(true);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putStringArrayList("urls", urls);
        savedInstanceState.putParcelableArrayList("ids", ids);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public class ImageAdapter extends BaseAdapter {
        CheckableLayout l;
        SquareImageView i;
        private Context mContext;
        private int mNumColumns;

        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getNumColumns() {
            return mNumColumns;
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
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
            return urls.size() + mNumColumns;
        }

        public Object getItem(int position) {
            return position < mNumColumns ?
                    null : urls.get(position);
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
            } else {
                if (convertView == null) {
                    i = new SquareImageView(mContext);
                    i.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    l = new CheckableLayout((MainActivity) getActivity());
                    l.setPadding(2, 2, 2, 2);
                    l.addView(i);
                } else {
                    l = (CheckableLayout) convertView;
                    i = (SquareImageView) l.getChildAt(0);
                }
                UrlImageViewHelper.setUrlDrawable(i, urls.get(position - mNumColumns));
                return l;
            }
        }
    }

    private class GridItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position - imageAdapter.getNumColumns());
        }
    }

    public class MultiChoiceModeListener implements GridView.MultiChoiceModeListener {
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle("Select Items");
            mode.setSubtitle("One item selected");
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.images_multi, menu);
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    if (albumId != null) {
                        getChecked();
                        async = new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                MainActivity activity = (MainActivity) getActivity();
                                for(int i = 0; i < intentReturn.size(); i++) {
                                    JSONObject response = activity.makeCall("3/image/" + intentReturn.get(i), "get", null);
                                    try {
                                        String deletehash = response.getJSONObject("data").getString("deletehash");
                                        activity.makeCall("3/image/" + deletehash, "delete", null);
                                    }
                                    catch (Exception e) {
                                        Log.e("Error!", e.toString());
                                    }
                                }
                                return null;
                            }
                        };
                        async.execute();
                    }
                    break;
                default:
                    break;
            }
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
            if (selecting) {
                Intent intent = new Intent();
                getChecked();
                intent.putExtra("data", intentReturn);
                ImageSelectActivity imageSelectActivity = (ImageSelectActivity) getActivity();
                imageSelectActivity.setResult(imageSelectActivity.RESULT_OK, intent);
                imageSelectActivity.finish();
            }
        }

        private void getChecked() {
            intentReturn = new ArrayList<String>();
            try {
                for (int i = 0; i < gridview.getCount(); i++) {
                    if (gridview.isItemChecked(i))
                        intentReturn.add(ids.get(i).getJSONObject().getString("id"));
                }
            } catch (Exception e) {
                Log.e("Error!", e.toString());
            }
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                              boolean checked) {
            int selectCount = gridview.getCheckedItemCount();
            Log.d("count", "" + selectCount);
            switch (selectCount) {
                case 1:
                    mode.setSubtitle("One item selected");
                    break;
                default:
                    mode.setSubtitle("" + selectCount + " items selected");
                    break;
            }
        }
    }

    public class CheckableLayout extends FrameLayout implements Checkable {
        private boolean mChecked;

        public CheckableLayout(Context context) {
            super(context);
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void setChecked(boolean checked) {
            mChecked = checked;
            setBackgroundDrawable(checked ?
                    getResources().getDrawable(R.drawable.select_background)
                    : null);
        }

        public void toggle() {
            setChecked(!mChecked);
        }

    }

    private class AddImagesToAlbum extends AsyncTask<Void, Void, Void> {
        boolean add;
        private ArrayList<String> imageIDsAsync;

        public AddImagesToAlbum(ArrayList<String> _imageIDs, boolean _add) {
            imageIDsAsync = _imageIDs;
            add = _add;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = (MainActivity) getActivity();
            String albumids = "";
            for (int i = 0; i < imageIDsAsync.size(); i++) {
                if (i != 0)
                    albumids += ",";
                albumids += imageIDsAsync.get(i);
            }
            HashMap<String, Object> editMap = new HashMap<String, Object>();
            editMap.put("ids", albumids);
            editMap.put("id", albumId);
            activity.makeCall("3/album/" + albumId, "post", editMap);
            return null;
        }

    }
}
