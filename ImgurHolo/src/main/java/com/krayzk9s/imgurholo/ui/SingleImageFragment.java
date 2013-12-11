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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.libs.ZoomableImageView;
import com.krayzk9s.imgurholo.services.DownloadService;
import com.krayzk9s.imgurholo.tools.ApiCall;
import com.krayzk9s.imgurholo.tools.Fetcher;
import com.krayzk9s.imgurholo.tools.GetData;
import com.krayzk9s.imgurholo.tools.LoadImageAsync;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * Created by Kurt Zimmer on 7/22/13.
 */
public class SingleImageFragment extends Fragment implements GetData, OnRefreshListener {

    String[] mMenuList;
    JSONParcelable imageData;
    JSONParcelable commentData;
    boolean inGallery;
    CommentAdapter commentAdapter;
    View mainView;
    ListView commentLayout;
    ImageButton imageUpvote;
    ImageButton imageDownvote;
    ImageButton imageFavorite;
    ImageButton imageComment;
    ImageButton imageUser;
    ImageButton imageFullscreen;
    ArrayList<JSONParcelable> commentArray;
    LinearLayout imageLayoutView;
    PopupWindow popupWindow;
    String sort;
    TextView imageScore;
    TextView imageDetails;
    String newGalleryString;
    ActionMode mActionMode;
    ImageView imageView;
    PullToRefreshLayout mPullToRefreshLayout;
    final SingleImageFragment singleImageFragment = this;
    final static String DELETE = "delete";
    final static String FAVORITE = "favorite";
    final static String POSTCOMMENT = "postComment";
    final static String GALLERY = "gallery";
    final static String GALLERYPOST = "galleryPost";
    final static String GALLERYDELETE = "galleryDelete";
    final static String UPVOTE = "upvote";
    final static String COMMENTS = "comments";
    final static String DOWNVOTECOMMENT = "downvoteComment";
    final static String UPVOTECOMMENT = "upvoteComment";
    final static String REPLY = "reply";
    final static String EDITIMAGE = "editImage";

    public SingleImageFragment() {
        inGallery = false;
        sort = "Best";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if(bundle.containsKey("gallery"))
            inGallery = bundle.getBoolean("gallery");
        imageData = bundle.getParcelable("imageData");
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if(activity != null && activity.getActionBar() != null)
            activity.getActionBar().setTitle("Image");
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (sort == null || sort.equals("Best"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortBest).setChecked(true);
        else if (sort.equals("Top"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setChecked(true);
        else if (sort.equals("New"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortNewest).setChecked(true);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        if (activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
            inflater.inflate(R.menu.main, menu);
        else
            inflater.inflate(R.menu.main_dark, menu);
        if (imageData.getJSONObject().has("deletehash")) {
            menu.findItem(R.id.action_delete).setVisible(true);
            menu.findItem(R.id.action_edit).setVisible(true);
            menu.findItem(R.id.action_submit).setVisible(true);
        }
        menu.findItem(R.id.action_share).setVisible(true);
        menu.findItem(R.id.action_download).setVisible(true);
        menu.findItem(R.id.action_copy).setVisible(true);
        menu.findItem(R.id.action_refresh).setVisible(true);
        menu.findItem(R.id.action_upload).setVisible(false);
        menu.findItem(R.id.action_sort).setVisible(true);
        menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortNewest).setVisible(true);
        menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortBest).setVisible(true);
        menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(false);
    }

    @Override
    public void onRefreshStarted(View view) {
        refreshComments();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        final Activity activity = getActivity();
        switch (item.getItemId()) {
            case R.id.action_sort:
                return true;
            case R.id.menuSortNewest:
                sort = "New";
                refreshComments();
                activity.invalidateOptionsMenu();
                return true;
            case R.id.menuSortTop:
                sort = "Top";
                refreshComments();
                activity.invalidateOptionsMenu();
                return true;
            case R.id.menuSortBest:
                sort = "Best";
                refreshComments();
                activity.invalidateOptionsMenu();
                return true;
            case R.id.action_refresh:
                refreshComments();
                return true;
            case R.id.action_submit:
                final EditText newGalleryTitle = new EditText(activity);
                newGalleryTitle.setHint("Title");
                newGalleryTitle.setSingleLine();
                new AlertDialog.Builder(activity).setTitle("Set Gallery Title/Press OK to remove")
                        .setView(newGalleryTitle).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(newGalleryTitle.getText() == null)
                            return;
                        HashMap<String, Object> galleryMap = new HashMap<String, Object>();
                        galleryMap.put("terms", "1");
                        galleryMap.put("title", newGalleryTitle.getText().toString());
                        newGalleryString = newGalleryTitle.getText().toString();
                        try {
                            Fetcher fetcher = new Fetcher(singleImageFragment, "3/gallery/image/" + imageData.getJSONObject().getString("id"), ApiCall.GET, galleryMap, ((ImgurHoloActivity)getActivity()).getApiCall(), GALLERY);
                            fetcher.execute();
                        }
                        catch (Exception e) {
                            Log.e("Error!", e.toString());
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();

                return true;
            case R.id.action_edit:
                try {
                    final EditText newTitle = new EditText(activity);
                    newTitle.setSingleLine();
                    if (!imageData.getJSONObject().getString("title").equals("null"))
                        newTitle.setText(imageData.getJSONObject().getString("title"));
                    final EditText newBody = new EditText(activity);
                    newBody.setHint("Description");
                    newTitle.setHint("Title");
                    if (!imageData.getJSONObject().getString("description").equals("null"))
                        newBody.setText(imageData.getJSONObject().getString("description"));
                    LinearLayout linearLayout = new LinearLayout(activity);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayout.addView(newTitle);
                    linearLayout.addView(newBody);
                    new AlertDialog.Builder(activity).setTitle("Edit Image Details")
                            .setView(linearLayout).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            TextView imageTitle = (TextView) imageLayoutView.findViewById(R.id.single_image_title);
                            TextView imageDescription = (TextView) imageLayoutView.findViewById(R.id.single_image_description);
                            if (newTitle.getText() != null && !newTitle.getText().toString().equals("")) {
                                imageTitle.setText(newTitle.getText().toString());
                                imageTitle.setVisibility(View.VISIBLE);
                            } else
                                imageTitle.setVisibility(View.GONE);
                            if (newBody.getText() != null && !newBody.getText().toString().equals("")) {
                                imageDescription.setText(newBody.getText().toString());
                                imageDescription.setVisibility(View.VISIBLE);
                            } else
                                imageDescription.setVisibility(View.GONE);
                            HashMap<String, Object> editImageMap = new HashMap<String, Object>();
                            editImageMap.put("title", newTitle.getText().toString());
                            editImageMap.put("description", newBody.getText().toString());
                            try {
                                Fetcher fetcher = new Fetcher(singleImageFragment, "3/image/" + imageData.getJSONObject().getString("id"), ApiCall.POST, editImageMap, ((ImgurHoloActivity)getActivity()).getApiCall(), EDITIMAGE);
                                fetcher.execute();
                            }
                            catch (JSONException e) {
                                Log.e("Error!", e.toString());
                            }
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                        }
                    }).show();
                } catch (JSONException e) {
                    Log.e("Error!", "oops, some image fields missing values" + e.toString());
                }
                return true;
            case R.id.action_download:
                Intent serviceIntent = new Intent(activity, DownloadService.class);
                ArrayList<Parcelable> downloadIDs = new ArrayList<Parcelable>();
                downloadIDs.add(imageData);
                serviceIntent.putParcelableArrayListExtra("ids", downloadIDs);
                activity.startService(serviceIntent);
                return true;
            case R.id.action_delete:
                new AlertDialog.Builder(activity).setTitle("Delete Image?")
                        .setMessage("Are you sure you want to delete this image? This cannot be undone")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    Fetcher fetcher = new Fetcher(singleImageFragment, "3/image/" + imageData.getJSONObject().getString("id"), ApiCall.DELETE, null, ((ImgurHoloActivity)getActivity()).getApiCall(), DELETE);
                                    fetcher.execute();
                                }
                                catch (JSONException e) {
                                    Log.e("Error!", e.toString());
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();
                return true;
            case R.id.action_copy:
                String[] copyTypes = getResources().getStringArray(R.array.copyTypes);
                try {
                    copyTypes[0] = copyTypes[0] + "\nhttp://imgur.com/" + imageData.getJSONObject().getString("id");
                    copyTypes[1] = copyTypes[1] + "\n" + imageData.getJSONObject().getString("link");
                    copyTypes[2] = copyTypes[2] + "\n<a href=\"http://imgur.com/" + imageData.getJSONObject().getString("id") + "\"><img src=\"" + imageData.getJSONObject().getString("link") + "\" title=\"Hosted by imgur.com\"/></a>";
                    copyTypes[3] = copyTypes[3] + "\n[IMG]" + imageData.getJSONObject().getString("link") + "[/IMG]";
                    copyTypes[4] = copyTypes[4] + "\n[URL=http://imgur.com/" + imageData.getJSONObject().getString("id") + "][IMG]" + imageData.getJSONObject().getString("link") + "[/IMG][/URL]";
                    copyTypes[5] = copyTypes[5] + "\n[Imgur](http://i.imgur.com/" + imageData.getJSONObject().getString("id") + ")";
                } catch (JSONException e) {
                    Log.e("Error!", e.toString());
                }
                new AlertDialog.Builder(activity).setTitle("Set Link Type to Copy")
                        .setItems(copyTypes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Activity activity = getActivity();
                                ClipboardManager clipboard = (ClipboardManager)
                                        activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                try {
                                    String link = "";
                                    switch (whichButton) {
                                        case 0:
                                            link = "http://imgur.com/" + imageData.getJSONObject().getString("id");
                                            break;
                                        case 1:
                                            link = imageData.getJSONObject().getString("link");
                                            break;
                                        case 2:
                                            link = "<a href=\"http://imgur.com/" + imageData.getJSONObject().getString("id") + "\"><img src=\"" + imageData.getJSONObject().getString("link") + "\" title=\"Hosted by imgur.com\"/></a>";
                                            break;
                                        case 3:
                                            link = "[IMG]" + imageData.getJSONObject().getString("link") + "[/IMG]";
                                            break;
                                        case 4:
                                            link = "[URL=http://imgur.com/" + imageData.getJSONObject().getString("id") + "][IMG]" + imageData.getJSONObject().getString("link") + "[/IMG][/URL]";
                                            break;
                                        case 5:
                                            link = "[Imgur](http://i.imgur.com/" + imageData.getJSONObject().getString("id") + ")";
                                            break;
                                        default:
                                            break;
                                    }
                                    int duration = Toast.LENGTH_SHORT;
                                    Toast toast;
                                    toast = Toast.makeText(getActivity(), "URL Copied!", duration);
                                    toast.show();
                                    ClipData clip = ClipData.newPlainText("imgur Link", link);
                                    clipboard.setPrimaryClip(clip);
                                } catch (JSONException e) {
                                    Log.e("Error!", "No link in image data!");
                                }

                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                return true;
            case R.id.action_share:
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                try {
                    intent.putExtra(Intent.EXTRA_TEXT, imageData.getJSONObject().getString("link"));
                } catch (JSONException e) {
                    Log.e("Error!", "bad link to share");
                }
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onGetObject(Object o, String tag) {
        if(tag.equals(REPLY)) {
            int duration = Toast.LENGTH_SHORT;
            Toast toast;
            toast = Toast.makeText(getActivity(), "Comment Posted!", duration);
            toast.show();
            refreshComments();
        }
        else if(tag.equals(COMMENTS)) {
            JSONObject jsonObject = (JSONObject) o;
            if(jsonObject != null)
                commentData.setJSONObject(jsonObject);
            if (inGallery && commentAdapter != null) {
                addComments();
                commentAdapter.notifyDataSetChanged();
            }
            if(mPullToRefreshLayout != null)
                mPullToRefreshLayout.setRefreshComplete();
        }
        else if(tag.equals(DELETE)) {
            getActivity().getFragmentManager().popBackStack();
        }
        else if(tag.equals(POSTCOMMENT)) {
            int duration = Toast.LENGTH_SHORT;
            Toast toast;
            toast = Toast.makeText(getActivity(), "Comment Posted!", duration);
            toast.show();
            refreshComments();
        }
        else if(tag.equals(GALLERY)) {
            JSONObject jsonObject = (JSONObject) o;
            int duration = Toast.LENGTH_SHORT;
            Toast toast;
            try {
            if (jsonObject.getJSONObject("data").has("error")) {
                HashMap<String, Object> galleryMap = new HashMap<String, Object>();
                galleryMap.put("terms", "1");
                galleryMap.put("title", newGalleryString);
                Fetcher fetcher = new Fetcher(this, "3/gallery/" + imageData.getJSONObject().getString("id"), ApiCall.POST, galleryMap, ((ImgurHoloActivity)getActivity()).getApiCall(), GALLERYPOST);
                fetcher.execute();
                toast = Toast.makeText(getActivity(), "Submitted!", duration);
            } else {
                Fetcher fetcher = new Fetcher(this, "3/gallery/" + imageData.getJSONObject().getString("id"), ApiCall.DELETE, null, ((ImgurHoloActivity)getActivity()).getApiCall(), GALLERYDELETE);
                fetcher.execute();
                toast = Toast.makeText(getActivity(), "Removed!", duration);
            }
            toast.show();
            }
            catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
        }
    }

    public void handleException(Exception e, String tag) {

    }

    public void refreshComments() {
        try {
        imageLayoutView.findViewById(R.id.image_progress).setVisibility(View.VISIBLE);
        if (imageData.getJSONObject().getInt("size") < 3250000 && imageData.getJSONObject().has("cover")) //temporary to fix large gif bug
            Ion.with(getActivity(), "http://imgur.com/" + imageData.getJSONObject().getString("cover") + ".png")
                    .setLogging("MyLogs", Log.DEBUG)
                    .progressBar((ProgressBar) imageLayoutView.findViewById(R.id.image_progress))
                    .withBitmap()
                    .intoImageView(imageView).setCallback(new FutureCallback<ImageView>() {
                @Override
                public void onCompleted(Exception e, ImageView result) {
                    imageLayoutView.findViewById(R.id.image_progress).setVisibility(View.GONE);
                }
            });
        else if(imageData.getJSONObject().getInt("size") < 3250000)
            Ion.with(getActivity(), imageData.getJSONObject().getString("link"))
                    .setLogging("MyLogs", Log.DEBUG)
                    .progressBar((ProgressBar) imageLayoutView.findViewById(R.id.image_progress))
                    .withBitmap()
                    .intoImageView(imageView).setCallback(new FutureCallback<ImageView>() {
                    @Override
                    public void onCompleted(Exception e, ImageView result) {
                        imageLayoutView.findViewById(R.id.image_progress).setVisibility(View.GONE);
                    }
            });
        }
        catch (JSONException e) {
            Log.e("Error!", e.toString());
        }
        commentAdapter.clear();
        commentAdapter.hiddenViews.clear();
        commentAdapter.notifyDataSetChanged();
        getComments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        final SharedPreferences settings = activity.getApiCall().settings;
        sort = settings.getString("CommentSort", "Best");
        boolean newData = true;
        if (commentData != null) {
            newData = false;
        }

        mainView = inflater.inflate(R.layout.single_image_layout, container, false);
        mMenuList = getResources().getStringArray(R.array.emptyList);
        if(commentAdapter == null)
            commentAdapter = new CommentAdapter(mainView.getContext(), R.id.comment_item);
        commentLayout = (ListView) mainView.findViewById(R.id.comment_thread);
        commentLayout.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        if (settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
            imageLayoutView = (LinearLayout) View.inflate(getActivity(), R.layout.image_view, null);
        else
            imageLayoutView = (LinearLayout) View.inflate(getActivity(), R.layout.dark_image_view, null);


        mPullToRefreshLayout = (PullToRefreshLayout) mainView.findViewById(R.id.ptr_layout);
        ActionBarPullToRefresh.from(getActivity())
                // Mark All Children as pullable
                .allChildrenArePullable()
                        // Set the OnRefreshListener
                .listener(this)
                        // Finally commit the setup to our PullToRefreshLayout
                .setup(mPullToRefreshLayout);
        if (savedInstanceState != null && newData) {
            imageData = savedInstanceState.getParcelable("imageData");
            inGallery = savedInstanceState.getBoolean("inGallery");
        }
        LinearLayout layout = (LinearLayout) imageLayoutView.findViewById(R.id.image_buttons);
        imageDetails = (TextView) imageLayoutView.findViewById(R.id.single_image_details);
        layout.setVisibility(View.VISIBLE);
        imageFullscreen = (ImageButton) imageLayoutView.findViewById(R.id.fullscreen);
        imageUpvote = (ImageButton) imageLayoutView.findViewById(R.id.rating_good);
        imageDownvote = (ImageButton) imageLayoutView.findViewById(R.id.rating_bad);
        imageFavorite = (ImageButton) imageLayoutView.findViewById(R.id.rating_favorite);
        imageComment = (ImageButton) imageLayoutView.findViewById(R.id.comment);
        imageUser = (ImageButton) imageLayoutView.findViewById(R.id.user);
        if (imageData.getJSONObject().has("ups")) {
            imageScore = (TextView) imageLayoutView.findViewById(R.id.single_image_score);
            imageScore.setVisibility(View.VISIBLE);
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) imageScore.getLayoutParams();
            if(mlp != null)
                mlp.setMargins(0, 0, 0, 4);
            updateImageFont();
            imageUpvote.setVisibility(View.VISIBLE);
            imageDownvote.setVisibility(View.VISIBLE);
            imageUser.setVisibility(View.VISIBLE);
            imageFavorite.setVisibility(View.VISIBLE);
            imageComment.setVisibility(View.VISIBLE);
            try {
                if (!imageData.getJSONObject().has("account_url") || imageData.getJSONObject().getString("account_url").equals("null") || imageData.getJSONObject().getString("account_url").equals("[deleted]"))
                    imageUser.setVisibility(View.GONE);
                if (!imageData.getJSONObject().has("vote")) {
                    imageUpvote.setVisibility(View.GONE);
                    imageDownvote.setVisibility(View.GONE);
                } else {
                    if (imageData.getJSONObject().getString("vote") != null && imageData.getJSONObject().getString("vote").equals("up"))
                        imageUpvote.setImageResource(R.drawable.green_rating_good);
                    else if (imageData.getJSONObject().getString("vote") != null && imageData.getJSONObject().getString("vote").equals("down"))
                        imageDownvote.setImageResource(R.drawable.red_rating_bad);
                }
                if (imageData.getJSONObject().getString("favorite") != null && imageData.getJSONObject().getBoolean("favorite"))
                    imageFavorite.setImageResource(R.drawable.green_rating_favorite);
            } catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
            imageFavorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (!imageData.getJSONObject().getBoolean("favorite")) {
                            imageFavorite.setImageResource(R.drawable.green_rating_favorite);
                            imageData.getJSONObject().put("favorite", true);
                        } else {
                            imageData.getJSONObject().put("favorite", false);
                            if (settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
                                imageFavorite.setImageResource(R.drawable.rating_favorite);
                            else
                                imageFavorite.setImageResource(R.drawable.dark_rating_favorite);
                        }
                        Fetcher fetcher = new Fetcher(singleImageFragment, "3/image/" + imageData.getJSONObject().getString("id") + "/favorite", ApiCall.POST, null, ((ImgurHoloActivity)getActivity()).getApiCall(), FAVORITE);
                        fetcher.execute();
                    } catch (JSONException e) {
                        Log.e("Error!", "missing data" + e.toString());
                    }
                }
            });
            imageUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Intent intent = new Intent();
                        intent.putExtra("username", imageData.getJSONObject().getString("account_url"));
                        intent.setAction(ImgurHoloActivity.ACCOUNT_INTENT);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        startActivity(intent);
                    } catch (JSONException e) {
                        Log.e("Error!", e.toString());
                    }

                }
            });
            imageComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Activity activity = getActivity();
                    final EditText newBody = new EditText(activity);
                    newBody.setHint("Body");
                    newBody.setLines(3);
                    final TextView characterCount = new TextView(activity);
                    characterCount.setText("140");
                    LinearLayout commentReplyLayout = new LinearLayout(activity);
                    newBody.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                            //
                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                            characterCount.setText(String.valueOf(140 - charSequence.length()));
                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            for (int i = editable.length(); i > 0; i--) {
                                if (editable.subSequence(i - 1, i).toString().equals("\n"))
                                    editable.replace(i - 1, i, "");
                            }
                        }
                    });
                    commentReplyLayout.setOrientation(LinearLayout.VERTICAL);
                    commentReplyLayout.addView(newBody);
                    commentReplyLayout.addView(characterCount);
                    new AlertDialog.Builder(activity).setTitle("Comment on Image")
                            .setView(commentReplyLayout).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (newBody.getText() != null && newBody.getText().toString().length() < 141) {
                                HashMap<String, Object> commentMap = new HashMap<String, Object>();
                                try {
                                    commentMap.put("comment", newBody.getText().toString());
                                    commentMap.put("image_id", imageData.getJSONObject().getString("id"));
                                    Fetcher fetcher = new Fetcher(singleImageFragment, "3/comment/", ApiCall.POST, commentMap, ((ImgurHoloActivity)getActivity()).getApiCall(), POSTCOMMENT);
                                    fetcher.execute();
                                } catch (JSONException e) {
                                    Log.e("Error!", e.toString());
                                }
                            }
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                        }
                    }).show();
                }
            });
            imageUpvote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (!imageData.getJSONObject().getString("vote").equals("up")) {
                            if (settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT)) {
                                imageUpvote.setImageResource(R.drawable.green_rating_good);
                                imageDownvote.setImageResource(R.drawable.rating_bad);
                            } else {
                                imageUpvote.setImageResource(R.drawable.green_rating_good);
                                imageDownvote.setImageResource(R.drawable.dark_rating_bad);
                            }
                            imageData.getJSONObject().put("ups", (Integer.parseInt(imageData.getJSONObject().getString("ups")) + 1) + "");
                            if(imageData.getJSONObject().getString("vote").equals("down")) {
                                imageData.getJSONObject().put("score", (Integer.parseInt(imageData.getJSONObject().getString("score")) + 2) + "");
                                imageData.getJSONObject().put("downs", (Integer.parseInt(imageData.getJSONObject().getString("downs")) - 1) + "");
                            }
                            else {
                                imageData.getJSONObject().put("score", (Integer.parseInt(imageData.getJSONObject().getString("score")) + 1) + "");
                            }
                            imageData.getJSONObject().put("vote", "up");
                        } else {
                            if (settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT)) {
                                imageUpvote.setImageResource(R.drawable.rating_good);
                                imageDownvote.setImageResource(R.drawable.rating_bad);
                            } else {
                                imageUpvote.setImageResource(R.drawable.dark_rating_good);
                                imageDownvote.setImageResource(R.drawable.dark_rating_bad);
                            }
                            imageData.getJSONObject().put("score", (Integer.parseInt(imageData.getJSONObject().getString("score")) - 1) + "");
                            imageData.getJSONObject().put("ups", (Integer.parseInt(imageData.getJSONObject().getString("ups")) - 1) + "");
                            imageData.getJSONObject().put("vote", "none");
                        }
                    } catch (JSONException e) {
                        Log.e("Error!", e.toString());
                    }
                    updateImageFont();
                    try {
                        Fetcher fetcher = new Fetcher(singleImageFragment, "3/gallery/" + imageData.getJSONObject().getString("id") + "/vote/up", ApiCall.POST, null, ((ImgurHoloActivity)getActivity()).getApiCall(), UPVOTE);
                        fetcher.execute();
                    }
                    catch (Exception e) {
                        Log.e("Error!", e.toString());
                    }
                }
            });
            imageDownvote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (!imageData.getJSONObject().getString("vote").equals("down")) {
                            if (settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT)) {
                                imageData.getJSONObject().put("vote", "down");
                                imageUpvote.setImageResource(R.drawable.rating_good);
                                imageDownvote.setImageResource(R.drawable.red_rating_bad);
                            } else {
                                imageUpvote.setImageResource(R.drawable.dark_rating_good);
                                imageDownvote.setImageResource(R.drawable.red_rating_bad);
                            }
                            imageData.getJSONObject().put("downs", (Integer.parseInt(imageData.getJSONObject().getString("downs")) + 1) + "");
                            if (imageData.getJSONObject().getString("vote").equals("up")) {
                                imageData.getJSONObject().put("score", (Integer.parseInt(imageData.getJSONObject().getString("score")) - 2) + "");
                                imageData.getJSONObject().put("ups", (Integer.parseInt(imageData.getJSONObject().getString("ups")) - 1) + "");
                            } else {
                                imageData.getJSONObject().put("score", (Integer.parseInt(imageData.getJSONObject().getString("score")) - 1) + "");
                            }
                            imageData.getJSONObject().put("vote", "down");
                        } else {
                            if (settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT)) {
                                imageUpvote.setImageResource(R.drawable.rating_good);
                                imageDownvote.setImageResource(R.drawable.rating_bad);
                            } else {
                                imageUpvote.setImageResource(R.drawable.dark_rating_good);
                                imageDownvote.setImageResource(R.drawable.dark_rating_bad);
                            }
                            imageData.getJSONObject().put("score", (Integer.parseInt(imageData.getJSONObject().getString("score")) + 1) + "");
                            imageData.getJSONObject().put("downs", (Integer.parseInt(imageData.getJSONObject().getString("downs")) - 1) + "");
                            imageData.getJSONObject().put("vote", "none");
                        }
                    } catch (JSONException e) {
                        Log.e("Error!", e.toString());
                    }
                    updateImageFont();
                    try {
                    Fetcher fetcher = new Fetcher(singleImageFragment, "3/gallery/" + imageData.getJSONObject().getString("id") + "/vote/down", ApiCall.POST, null, ((ImgurHoloActivity)getActivity()).getApiCall(), UPVOTE);
                    fetcher.execute();
                    }
                    catch (JSONException e) {
                        Log.e("Error!", e.toString());
                    }
                }
            });
        }
        imageFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (popupWindow != null) {
                    popupWindow.dismiss();
                    popupWindow = null;
                }
                popupWindow = new PopupWindow();
                popupWindow.setBackgroundDrawable(new ColorDrawable(0x80000000));
                popupWindow.setFocusable(true);
                popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                final ZoomableImageView zoomableImageView = new ZoomableImageView(getActivity());
                popupWindow.setContentView(zoomableImageView);
                popupWindow.showAtLocation(mainView, Gravity.TOP, 0, 0);
                LoadImageAsync loadImageAsync = new LoadImageAsync(zoomableImageView, imageData);
                loadImageAsync.execute();
            }
        });
        ArrayAdapter<String> tempAdapter = new ArrayAdapter<String>(mainView.getContext(),
                R.layout.drawer_list_item, mMenuList);
        Log.d("URI", "YO I'M IN YOUR SINGLE FRAGMENT gallery:" + inGallery);
        try {
            Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, imageData.getJSONObject().getInt("height"), getResources().getDisplayMetrics());
            int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, imageData.getJSONObject().getInt("width"), getResources().getDisplayMetrics());
            Log.d("height", ""+height);
            Log.d("width", ""+width);
            int statusBarHeight = (int)Math.ceil(25 * getActivity().getResources().getDisplayMetrics().density);
            TypedValue tv = new TypedValue();
            getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
            int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId) + statusBarHeight;
            imageView = (ImageView) imageLayoutView.findViewById(R.id.single_image_view);
            imageLayoutView.findViewById(R.id.image_progress).setVisibility(View.VISIBLE);
            if (imageData.getJSONObject().getInt("size") < 3250000 && imageData.getJSONObject().has("cover"))
                Ion.with(getActivity(), "http://imgur.com/" + imageData.getJSONObject().getString("cover") + ".png")
                        .setLogging("MyLogs", Log.DEBUG)
                        .progressBar((ProgressBar) imageLayoutView.findViewById(R.id.image_progress))
                        .withBitmap()
                        .intoImageView(imageView).setCallback(new FutureCallback<ImageView>() {
                    @Override
                    public void onCompleted(Exception e, ImageView result) {
                        imageLayoutView.findViewById(R.id.image_progress).setVisibility(View.GONE);
                    }
                });
            else if(imageData.getJSONObject().getInt("size") < 3250000)
                Ion.with(getActivity(), imageData.getJSONObject().getString("link"))
                        .setLogging("MyLogs", Log.DEBUG)
                        .progressBar((ProgressBar) imageLayoutView.findViewById(R.id.image_progress))
                        .withBitmap()
                        .intoImageView(imageView).setCallback(new FutureCallback<ImageView>() {
                    @Override
                    public void onCompleted(Exception e, ImageView result) {
                        imageLayoutView.findViewById(R.id.image_progress).setVisibility(View.GONE);
                    }
                });
            if(settings.getBoolean("VerticalHeight", true))
                imageView.setMaxHeight(size.y - actionBarHeight);
        } catch (JSONException e) {
            Log.e("drawable Error!", e.toString());
        }
        TextView imageTitle = (TextView) imageLayoutView.findViewById(R.id.single_image_title);
        TextView imageDescription = (TextView) imageLayoutView.findViewById(R.id.single_image_description);

        try {
            String size = String.valueOf(NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("width"))) + "x" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("height")) + " (" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("size")) + "B)";
            String initial = imageData.getJSONObject().getString("type") + " | " + size + " | Views: " + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("views"));
            imageDetails.setText(initial);
            Log.d("imagedata", imageData.getJSONObject().toString());
            if (!imageData.getJSONObject().getString("title").equals("null"))
                imageTitle.setText(imageData.getJSONObject().getString("title"));
            else
                imageTitle.setVisibility(View.GONE);
            if (!imageData.getJSONObject().getString("description").equals("null"))
                imageDescription.setText(imageData.getJSONObject().getString("description"));
            else
                imageDescription.setVisibility(View.GONE);
            commentLayout.addHeaderView(imageLayoutView);
            commentLayout.setAdapter(tempAdapter);
        } catch (JSONException e) {
            Log.e("Text Error!", e.toString());
        }
        if ((savedInstanceState == null || commentData == null) && newData) {
            commentData = new JSONParcelable();
            getComments();
            commentLayout.setAdapter(commentAdapter);
        } else if (newData) {
            commentArray = savedInstanceState.getParcelableArrayList("commentData");
            commentAdapter.addAll(commentArray);
            commentLayout.setAdapter(commentAdapter);
            commentAdapter.notifyDataSetChanged();
        } else if (commentArray != null) {
            commentAdapter.addAll(commentArray);
            commentLayout.setAdapter(commentAdapter);
            commentAdapter.notifyDataSetChanged();
        }
        return mainView;
    }

    private void updateImageFont() {
        try {
            if (!imageData.getJSONObject().has("vote")) {
                imageScore.setVisibility(View.GONE);
                return;
            }
            if (imageData.getJSONObject().getString("vote") != null && imageData.getJSONObject().getString("vote").equals("up"))
                imageScore.setText(Html.fromHtml("<font color=#89c624>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("score")) + " points </font> (<font color=#89c624>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("ups")) + "</font>/<font color=#ee4444>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("downs")) + "</font>)"));
            else if (imageData.getJSONObject().getString("vote") != null && imageData.getJSONObject().getString("vote").equals("down"))
                imageScore.setText(Html.fromHtml("<font color=#ee4444>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("score")) + " points </font> (<font color=#89c624>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("ups")) + "</font>/<font color=#ee4444>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("downs")) + "</font>)"));
            else
                imageScore.setText(Html.fromHtml(NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("score")) + " points (<font color=#89c624>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("ups")) + "</font>/<font color=#ee4444>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("downs")) + "</font>)"));
        }
        catch (JSONException e) {
            Log.e("Error font!", e.toString());

        }
    }

    public void getComments() {
        if(!((ImgurHoloActivity)getActivity()).getApiCall().settings.getBoolean("ShowComments", true))
            return;
        try {
            Fetcher fetcher = new Fetcher(this, "3/gallery/image/" + imageData.getJSONObject().getString("id") + "/comments", ApiCall.GET, null, ((ImgurHoloActivity)getActivity()).getApiCall(), COMMENTS);
            fetcher.execute();
        }
        catch (JSONException e) {
            Log.e("Error!", e.toString());
        }
    }

    private void addComments() {
        try {
            JSONArray commentJSONArray = commentData.getJSONObject().getJSONArray("data");
            commentArray = new ArrayList<JSONParcelable>();
            Log.d("calling indent function", commentJSONArray.toString());
            ArrayList<JSONObject> childrenArray = new ArrayList<JSONObject>();
            for (int i = 0; i < commentJSONArray.length(); i++) {
                childrenArray.add(commentJSONArray.getJSONObject(i));
            }
            if(sort.equals("New"))
                Collections.sort(childrenArray, new JSONNewestComparator());
            else if (sort.equals("Top"))
                Collections.sort(childrenArray, new JSONTopComparator());
            for (int i = 0; i < childrenArray.size(); i++) {
                getIndents(childrenArray.get(i), 0);
            }
            commentAdapter.addAll(commentArray);
        } catch (JSONException e) {
            Log.e("Error1!", e.toString());
        }
    }

    private void getIndents(JSONObject comment, int currentIndent) {
        JSONArray children;
        try {
            comment.put("indent", currentIndent);
            ArrayList<JSONObject> childrenArray = null;
            if (comment.has("children")) {
                children = comment.getJSONArray("children");
                childrenArray = new ArrayList<JSONObject>();
                for(int i = 0; i < children.length(); i++)
                    childrenArray.add(children.getJSONObject(i));
                if(sort.equals("New"))
                    Collections.sort(childrenArray, new JSONNewestComparator());
                else if (sort.equals("Top"))
                    Collections.sort(childrenArray, new JSONTopComparator());
                comment.remove("children");
            }
            JSONParcelable commentParse = new JSONParcelable();
            commentParse.setJSONObject(comment);
            commentArray.add(commentParse);
            if (childrenArray != null) {
                for (int i = 0; i < childrenArray.size(); i++) {
                    JSONObject child = childrenArray.get(i);
                    getIndents(child, currentIndent + 1);
                }
            }
        } catch (JSONException e) {
            Log.e("Error5!", e.toString());
        }
    }

    class JSONNewestComparator implements Comparator<JSONObject>
    {
        public int compare(JSONObject a, JSONObject b)
        {
            //valA and valB could be any simple type, such as number, string, whatever
            try {
                int valA = a.getInt("datetime");
                int valB = b.getInt("datetime");

                if(valA > valB)
                    return -1;
                if(valA < valB)
                    return 1;
                return 0;
            }
            catch (JSONException e) {
                Log.e("Error!", e.toString());
                return 0;
            }
        }
    }

    class JSONTopComparator implements Comparator<JSONObject>
    {
        public int compare(JSONObject a, JSONObject b)
        {
            //valA and valB could be any simple type, such as number, string, whatever
            try {
                int valA = a.getInt("points");
                int valB = b.getInt("points");

                if(valA > valB)
                    return -1;
                if(valA < valB)
                    return 1;
                return 0;
            }
            catch (JSONException e) {
                Log.e("Error!", e.toString());
                return 0;
            }
        }
    }

    private void setDescendantsHidden(View view) {
        LinearLayout convertView = (LinearLayout) view;
        ViewHolder holder = (ViewHolder) convertView.getTag();
        int position = holder.position;
        JSONObject viewData = commentAdapter.getItem(position).getJSONObject();
        try {
            if (!viewData.has("hidden"))
                viewData.put("hidden", ViewHolder.VIEW_VISIBLE);
            boolean hiding;
            int indentLevel = viewData.getInt("indent");
            if (viewData.getInt("hidden") != ViewHolder.VIEW_HIDDEN) {
                viewData.put("hidden", ViewHolder.VIEW_HIDDEN);
                hiding = true;
            } else {
                viewData.put("hidden", ViewHolder.VIEW_VISIBLE);
                hiding = false;
            }
            for (int i = position + 1; i < commentAdapter.getCount(); i++) {
                JSONObject childViewData = commentAdapter.getItem(i).getJSONObject();
                if (childViewData.getInt("indent") > indentLevel) {
                    if (hiding) {
                        childViewData.put("hidden", ViewHolder.VIEW_DESCENDANT);
                        commentAdapter.addHiddenItem(i);
                    } else {
                        childViewData.put("hidden", ViewHolder.VIEW_VISIBLE);
                        commentAdapter.removeHiddenItem(i);
                    }
                } else
                    break;
            }
        } catch (JSONException e) {
            Log.e("Converting to Hidden Error!", e.toString());
        }
        commentAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable("imageData", imageData);
        savedInstanceState.putParcelableArrayList("commentData", commentArray);
        savedInstanceState.putBoolean("inGallery", inGallery);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private static class ViewHolder {
        static final int VIEW_VISIBLE = 0;
        static final int VIEW_HIDDEN = 1;
        static final int VIEW_DESCENDANT = 2;
        public RelativeLayout header;
        public TextView username;
        public TextView points;
        public TextView body;
        public View[] indentViews;
        public String id;
        int position;
    }

    public class CommentAdapter extends ArrayAdapter<JSONParcelable> {
        final static int HIDDEN_TYPE = 0;
        final static int VISIBLE_TYPE = 1;
        private LayoutInflater mInflater;
        private ArrayList<Integer> hiddenViews;

        public CommentAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            hiddenViews = new ArrayList<Integer>();
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void addHiddenItem(int position) {
            if (!hiddenViews.contains(position))
                hiddenViews.add(position);
        }

        public void removeHiddenItem(int position) {
            hiddenViews.remove(Integer.valueOf(position));
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return hiddenViews.contains(Integer.valueOf(position)) ? HIDDEN_TYPE : VISIBLE_TYPE;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final JSONObject viewData = this.getItem(position).getJSONObject();
            int type = getItemViewType(position);
            ViewHolder holder = new ViewHolder();
            if (convertView == null || convertView.getTag() == null) {
                switch (type) {
                    case HIDDEN_TYPE:
                        convertView = mInflater.inflate(R.layout.zerolayout, null);
                        return convertView;
                    case VISIBLE_TYPE:
                        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
                        if (activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
                            convertView = View.inflate(getActivity(), R.layout.comment_list_item, null);
                        else
                            convertView = View.inflate(getActivity(), R.layout.comment_list_item_dark, null);
                        holder = new ViewHolder();
                        holder.body = (TextView) convertView.findViewById(R.id.body);
                        holder.username = (TextView) convertView.findViewById(R.id.username);
                        holder.points = (TextView) convertView.findViewById(R.id.points);
                        holder.id = "";
                        holder.position = position;
                        holder.indentViews = new View[]{
                                convertView.findViewById(R.id.margin_1),
                                convertView.findViewById(R.id.margin_2),
                                convertView.findViewById(R.id.margin_3),
                                convertView.findViewById(R.id.margin_4),
                                convertView.findViewById(R.id.margin_5),
                                convertView.findViewById(R.id.margin_6)
                        };
                        convertView.setTag(holder);
                        break;
                }

            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final View viewVar = convertView;
            final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
                // Called when the action mode is created; startActionMode() was called
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    // Inflate a menu resource providing context menu items
                    MenuInflater inflater = mode.getMenuInflater();
                    ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
                    if (activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
                        inflater.inflate(R.menu.comments, menu);
                    else
                        inflater.inflate(R.menu.comments_dark, menu);
                    return true;
                }
                // Called each time the action mode is shown. Always called after onCreateActionMode, but
                // may be called multiple times if the mode is invalidated.
                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false; // Return false if nothing is done
                }
                // Called when the user selects a contextual menu item
                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                    Fetcher fetcher;
                    switch (item.getItemId()) {
                        case R.id.user:
                            try {
                                Intent intent = new Intent();
                                intent.putExtra("username", viewData.getString("author"));
                                intent.setAction(ImgurHoloActivity.ACCOUNT_INTENT);
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                startActivity(intent);
                            } catch (JSONException e) {
                                Log.e("Error!", e.toString());
                            }
                             // Action picked, so close the CAB
                            mode.finish();
                            return true;
                        case R.id.reply:
                            Activity activity = getActivity();
                            final EditText newBody = new EditText(activity);
                            newBody.setLines(3);
                            final TextView characterCount = new TextView(activity);
                            characterCount.setText("140");
                            LinearLayout commentReplyLayout = new LinearLayout(activity);
                            newBody.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                                    //
                                }

                                @Override
                                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                                    characterCount.setText(String.valueOf(140 - charSequence.length()));
                                }

                                @Override
                                public void afterTextChanged(Editable editable) {
                                    for (int i = editable.length(); i > 0; i--) {
                                        if (editable.subSequence(i - 1, i).toString().equals("\n"))
                                            editable.replace(i - 1, i, "");
                                    }
                                }
                            });
                            commentReplyLayout.setOrientation(LinearLayout.VERTICAL);
                            commentReplyLayout.addView(newBody);
                            commentReplyLayout.addView(characterCount);
                            newBody.setHint("Body");
                            new AlertDialog.Builder(activity).setTitle("Reply to Comment")
                                    .setView(commentReplyLayout).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (newBody.getText() != null && newBody.getText().toString().length() < 141) {
                                        try {
                                            HashMap<String, Object> commentMap = new HashMap<String, Object>();
                                            commentMap.put("comment", newBody.getText().toString());
                                            commentMap.put("image_id", imageData.getJSONObject().getString("id"));
                                            commentMap.put("parent_id", + viewData.getInt("id"));
                                            Fetcher fetcher = new Fetcher(singleImageFragment, "3/comment/", ApiCall.POST, commentMap, ((ImgurHoloActivity)getActivity()).getApiCall(), REPLY);
                                            fetcher.execute();
                                        }
                                        catch(JSONException e) {
                                            Log.e("Error!", e.toString());
                                        }
                                    }
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Do nothing.
                                }
                            }).show();
                            mode.finish();
                            return true;
                        case R.id.rating_good:
                            try {
                                Log.d("data", viewData.toString());
                                if (!viewData.getString("vote").equals("up")) {
                                    viewData.put("ups", (Integer.parseInt(viewData.getString("ups")) + 1) + "");
                                    if(viewData.getString("vote").equals("down"))
                                    {
                                        viewData.put("points", (Integer.parseInt(viewData.getString("points")) + 2) + "");
                                        viewData.put("downs", (Integer.parseInt(viewData.getString("downs")) - 1) + "");
                                    }
                                    else
                                        viewData.put("points", (Integer.parseInt(viewData.getString("points")) + 1) + "");
                                    viewData.put("vote", "up");
                                } else {
                                    viewData.put("ups", (Integer.parseInt(viewData.getString("ups")) - 1) + "");
                                    viewData.put("points", (Integer.parseInt(viewData.getString("points")) - 1) + "");
                                    viewData.put("vote", "none");
                                }
                            updateFontColor((ViewHolder)viewVar.getTag(), viewData);
                            fetcher = new Fetcher(singleImageFragment, "/3/comment/" + viewData.getInt("id") + "/vote/up", ApiCall.POST, null, ((ImgurHoloActivity)getActivity()).getApiCall(), UPVOTECOMMENT);
                            fetcher.execute();
                            } catch (JSONException e) {
                                Log.e("Error!", e.toString());
                            }
                            mode.finish();
                            return true;
                        case R.id.rating_bad:
                            try {
                                if (!viewData.getString("vote").equals("down")) {
                                    viewData.put("downs", (Integer.parseInt(viewData.getString("downs")) + 1) + "");
                                    if(viewData.getString("vote").equals("up")) {
                                        viewData.put("ups", (Integer.parseInt(viewData.getString("ups")) - 1) + "");
                                        viewData.put("points", (Integer.parseInt(viewData.getString("points")) - 2) + "");
                                    }
                                    else
                                        viewData.put("points", (Integer.parseInt(viewData.getString("points")) - 1) + "");
                                    viewData.put("vote", "down");
                                } else {
                                    viewData.put("points", (Integer.parseInt(viewData.getString("points")) + 1) + "");
                                    viewData.put("downs", (Integer.parseInt(viewData.getString("downs")) - 1) + "");
                                    viewData.put("vote", "none");
                                }
                                updateFontColor((ViewHolder)viewVar.getTag(), viewData);
                            fetcher = new Fetcher(singleImageFragment, "/3/comment/" + viewData.getInt("id") + "/vote/up", ApiCall.POST, null, ((ImgurHoloActivity)getActivity()).getApiCall(), DOWNVOTECOMMENT);
                            fetcher.execute();
                            } catch (JSONException e) {
                                Log.e("Error!", e.toString());
                            }
                            mode.finish();
                            return true;
                        case R.id.report:
                            mode.finish();
                            return true;
                        default:
                            return false;
                    }
                }

                // Called when the user exits the action mode
                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    if(commentLayout.getCheckedItemCount() > 0)
                        commentLayout.setItemChecked(commentLayout.getCheckedItemPosition(), false);
                    mActionMode = null;
                }
            };
            switch (getItemViewType(position)) {
                case VISIBLE_TYPE:
                    try {
                        holder.position = position;
                        holder.id = viewData.getString("id");
                        int indentLevel = viewData.getInt("indent");
                        int indentPosition = Math.min(indentLevel, holder.indentViews.length - 1);

                        for (int i = 0; i < indentPosition - 1; i++) {
                            holder.indentViews[i].setVisibility(View.INVISIBLE);
                        }
                        if (indentPosition > 0)
                            holder.indentViews[indentPosition - 1].setVisibility(View.VISIBLE);
                        for (int i = indentPosition; i < holder.indentViews.length; i++) {
                            holder.indentViews[i].setVisibility(View.GONE);
                        }
                        holder.body.setText(viewData.getString("comment"));
                        updateFontColor(holder, viewData);
                        View commentView = convertView.findViewById(R.id.comment_item);
                        commentView.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                if(view == null || view.getParent() == null)
                                    return false;
                                setDescendantsHidden((View) view.getParent());
                                return true;
                            }
                        });
                        commentView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Start the CAB using the ActionMode.Callback defined above
                                try {
                                    if (viewData.has("hidden") && viewData.getInt("hidden") == ViewHolder.VIEW_HIDDEN) {
                                        setDescendantsHidden((View) view.getParent());
                                        return;
                                    }
                                }
                                catch (JSONException e) {
                                    Log.e("Error!", e.toString());
                                }
                                if(commentLayout.isItemChecked(commentLayout.getPositionForView(view)) && mActionMode != null) {
                                    commentLayout.setItemChecked(commentLayout.getPositionForView(view), false);
                                    mActionMode.finish();
                                    return;
                                }

                                mActionMode = getActivity().startActionMode(mActionModeCallback);
                                try {
                                    mActionMode.setTitle(viewData.getString("author"));
                                }
                                catch (JSONException e) {
                                    Log.e("Error!", e.toString());
                                }
                                commentLayout.setItemChecked(commentLayout.getPositionForView(view), true);
                            }
                        });
                        if (viewData.has("hidden") && viewData.getInt("hidden") == ViewHolder.VIEW_HIDDEN) {
                            holder.body.setVisibility(View.GONE);
                        } else
                            holder.body.setVisibility(View.VISIBLE);
                    } catch (JSONException e) {
                        Log.e("View Error!", e.toString());
                    }
                    if(convertView != null && holder != null)
                        convertView.setTag(holder);
                    return convertView;
                case HIDDEN_TYPE:
                    return convertView;
                default:
                    return convertView;
            }
        }
        private void updateFontColor(ViewHolder dataHolder, JSONObject viewData) {
            ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
            String username;
            try {
                if (viewData.getString("author").length() < 25)
                    username = viewData.getString("author") + " &#8226; ";
                else
                    username = viewData.getString("author").substring(0, 25) + "..." + " &#8226; ";
                if(activity.getApiCall().settings.getBoolean("ShowVotes", true))
                {
                    if (viewData.getString("vote") != null && viewData.getString("vote").equals("up"))
                        dataHolder.points.setText(Html.fromHtml(username + "<font color=#89c624>" + NumberFormat.getIntegerInstance().format(viewData.getInt("points")) + " points </font> (<font color=#89c624>" + NumberFormat.getIntegerInstance().format(viewData.getInt("ups")) + "</font>/<font color=#ee4444>" + NumberFormat.getIntegerInstance().format(viewData.getInt("downs")) + "</font>)"));
                    else if (viewData.getString("vote") != null && viewData.getString("vote").equals("down"))
                        dataHolder.points.setText(Html.fromHtml(username + "<font color=#ee4444>" + NumberFormat.getIntegerInstance().format(viewData.getInt("points")) + " points </font> (<font color=#89c624>" + NumberFormat.getIntegerInstance().format(viewData.getInt("ups")) + "</font>/<font color=#ee4444>" + NumberFormat.getIntegerInstance().format(viewData.getInt("downs")) + "</font>)"));
                    else
                        dataHolder.points.setText(Html.fromHtml(username + NumberFormat.getIntegerInstance().format(viewData.getInt("points")) + " points (<font color=#89c624>" + NumberFormat.getIntegerInstance().format(viewData.getInt("ups")) + "</font>/<font color=#ee4444>" + NumberFormat.getIntegerInstance().format(viewData.getInt("downs")) + "</font>)"));
                }
                else
                    dataHolder.points.setText(username);
            }
            catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
        }
    }
}