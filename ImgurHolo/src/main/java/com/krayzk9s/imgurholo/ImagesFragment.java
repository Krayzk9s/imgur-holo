package com.krayzk9s.imgurholo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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
    GridView gridview;
    public boolean selecting = false;
    MultiChoiceModeListener multiChoiceModeListener;
    ArrayList<String> intentReturn;
    String albumId;
    JSONObject galleryAlbumData;
    JSONObject imageParam;
    JSONObject imagesData;
    TextView noImageView;

    public ImagesFragment() {

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
        inflater.inflate(R.menu.main, menu);
        menu.findItem(R.id.action_upload).setVisible(false);
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
        MainActivity activity = (MainActivity) getActivity();
        switch (item.getItemId()) {
            case R.id.action_copy:
                try {
                    ClipboardManager clipboard = (ClipboardManager)
                            activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("imgur Link", "http://imgur.com/a/" + albumId);
                    clipboard.setPrimaryClip(clip);
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
                            imageParam = activity.makeGetCall("3/image/" + galleryAlbumData.getString("cover")).getJSONObject("data");
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
                        Log.d("Sending Params w/ new data", galleryAlbumData.toString());
                        SingleImageFragment fragment = new SingleImageFragment();
                        fragment.setParams(galleryAlbumData);
                        fragment.setGallery(true);
                        MainActivity activity = (MainActivity) getActivity();
                        activity.changeFragment(fragment);
                    }
                };
                async.execute();


            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        AddImagesToAlbum imageAsync;
        ArrayList<String> imageIds;
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
        if(urls == null) {
            urls = new ArrayList<String>();
            ids = new ArrayList<JSONParcelable>();
        }
        View view = inflater.inflate(R.layout.image_layout, container, false);
        gridview = (GridView) view.findViewById(R.id.grid_layout);
        noImageView = (TextView) view.findViewById(R.id.no_images);
        imageAdapter = new ImageAdapter(view.getContext());
        gridview.setAdapter(imageAdapter);
        gridview.setOnItemClickListener(new GridItemClickListener());
        gridview.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        multiChoiceModeListener = new MultiChoiceModeListener();
        gridview.setMultiChoiceModeListener(multiChoiceModeListener);
        if (savedInstanceState == null && urls.size() == 0) {

            AsyncTask<Void, Void, Boolean> imageAsync = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    MainActivity activity = (MainActivity) getActivity();
                    imagesData = activity.makeGetCall(imageCall);
                    try {
                        JSONArray imageArray;
                        if (imagesData.optJSONObject("data") != null)
                            imageArray = imagesData.getJSONObject("data").getJSONArray("images");
                        else
                            imageArray = imagesData.getJSONArray("data");
                        Log.d("single image array", imageArray.toString());
                        for (int i = 0; i < imageArray.length(); i++) {
                            JSONObject imageData = imageArray.getJSONObject(i);
                            Log.d("Data", imageData.toString());
                            if (imageData.has("is_album") && imageData.getBoolean("is_album")) {
                                    urls.add("http://imgur.com/" + imageData.getString("cover") + "m.png");
                            }
                            else {
                                    urls.add("http://imgur.com/" + imageData.getString("id") + "m.png");
                            }
                            JSONParcelable dataParcel = new JSONParcelable();
                            dataParcel.setJSONObject(imageData);
                            ids.add(dataParcel);
                        }
                        return (imageArray.length() > 0);
                    } catch (Exception e) {
                        Log.e("Error!", "bad image array data" + e.toString());
                    }
                    return false;
                }
                @Override
                protected void onPostExecute(Boolean changed) {
                    if(changed)
                        imageAdapter.notifyDataSetChanged();
                    else
                        noImageView.setVisibility(View.VISIBLE);
                }
            };
            imageAsync.execute();
        } else if (savedInstanceState != null) {
            urls = savedInstanceState.getStringArrayList("urls");
            ids = savedInstanceState.getParcelableArrayList("ids");
        }

        return view;
    }

    public class ImageAdapter extends BaseAdapter {
        CheckableLayout l;
        SquareImageView i;
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
            if (convertView == null) {
                i = new SquareImageView(mContext);
                i.setScaleType(ImageView.ScaleType.CENTER_CROP);
                l = new CheckableLayout((MainActivity) getActivity());
                l.setPadding(8, 8, 8, 8);
                l.addView(i);
            } else {
                l = (CheckableLayout) convertView;
                i = (SquareImageView) l.getChildAt(0);
            }
            UrlImageViewHelper.setUrlDrawable(i, urls.get(position), R.drawable.icon);
            return l;
        }
    }

    private class GridItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    public void selectItem(int position) {
        if (!selecting) {
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
            }
            catch (Exception e) {
                Log.e("Error!", e.toString());
            }
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
                    getChecked();
                    async = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            MainActivity activity = (MainActivity) getActivity();
                            activity.deleteImages(intentReturn);
                            return null;
                        }
                    };
                    async.execute();
                    break;
                default:
                    break;
            }
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
            if (selecting) {
                Intent intent = new Intent();
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

        public void setChecked(boolean checked) {
            mChecked = checked;
            setBackgroundDrawable(checked ?
                    getResources().getDrawable(R.drawable.select_background)
                    : null);
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void toggle() {
            setChecked(!mChecked);
        }

    }

    private class AddImagesToAlbum extends AsyncTask<Void, Void, Void> {
        private ArrayList<String> imageIDsAsync;
        boolean add;

        public AddImagesToAlbum(ArrayList<String> _imageIDs, boolean _add) {
            imageIDsAsync = _imageIDs;
            add = _add;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = (MainActivity) getActivity();
            String albumids = "";
            for (int i = 0; i < imageIDsAsync.size(); i++) {
                albumids += imageIDsAsync.get(i);
            }
            activity.editAlbum(albumids, albumId);
            return null;
        }

    }
}
