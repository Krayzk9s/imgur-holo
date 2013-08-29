package com.krayzk9s.imgurholo;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by Kurt Zimmer on 7/22/13.
 */
public class SingleImageFragment extends Fragment {

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

    public SingleImageFragment() {
        inGallery = false;
        sort = "Best";
    }

    public void setParams(JSONObject _params) {
        imageData = new JSONParcelable();
        imageData.setJSONObject(_params);
    }

    public void setGallery(boolean gallery) {
        inGallery = gallery;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
        MainActivity activity = (MainActivity) getActivity();
        if (activity.theme.equals(activity.HOLO_LIGHT))
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        final MainActivity activity = (MainActivity) getActivity();
        SharedPreferences settings = activity.getSettings();
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
                        AsyncTask<Void, Void, Boolean> galleryAsync = new AsyncTask<Void, Void, Boolean>() {
                            @Override
                            protected Boolean doInBackground(Void... voids) {
                                try {
                                    JSONObject jsonObject = activity.makeCall("3/gallery/image/" + imageData.getJSONObject().getString("id"), "get", null);
                                    if (jsonObject.getJSONObject("data").has("error")) {
                                        HashMap<String, Object> galleryMap = new HashMap<String, Object>();
                                        galleryMap.put("terms", "1");
                                        galleryMap.put("title", newGalleryTitle.getText().toString());
                                        activity.makeCall("3/gallery/" + imageData.getJSONObject().getString("id"), "post", galleryMap);
                                        return true;
                                    } else {
                                        activity.makeCall("3/gallery/" + imageData.getJSONObject().getString("id"), "delete", null);
                                        return false;
                                    }
                                } catch (Exception e) {
                                    Log.e("Error!", "oops, some text fields missing values" + e.toString());
                                }
                                return false;
                            }

                            @Override
                            protected void onPostExecute(Boolean bool) {
                                int duration = Toast.LENGTH_SHORT;
                                Toast toast;
                                if (bool)
                                    toast = Toast.makeText(activity, "Submitted!", duration);
                                else
                                    toast = Toast.makeText(activity, "Removed!", duration);
                                toast.show();
                            }
                        };
                        galleryAsync.execute();
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
                    if (imageData.getJSONObject().getString("title") != "null")
                        newTitle.setText(imageData.getJSONObject().getString("title"));
                    final EditText newBody = new EditText(activity);
                    newBody.setHint("Description");
                    newTitle.setHint("Title");
                    if (imageData.getJSONObject().getString("description") != "null")
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
                            if (!newTitle.getText().toString().equals("")) {
                                imageTitle.setText(newTitle.getText().toString());
                                imageTitle.setVisibility(View.VISIBLE);
                            } else
                                imageTitle.setVisibility(View.GONE);
                            if (!newBody.getText().toString().equals("")) {
                                imageDescription.setText(newBody.getText().toString());
                                imageDescription.setVisibility(View.VISIBLE);
                            } else
                                imageDescription.setVisibility(View.GONE);
                            AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... voids) {
                                    try {
                                        HashMap<String, Object> editImageMap = new HashMap<String, Object>();
                                        editImageMap.put("title", newTitle.getText().toString());
                                        editImageMap.put("description", newBody.getText().toString());
                                        activity.makeCall("3/image/" + imageData.getJSONObject().getString("id"), "post", editImageMap);
                                    } catch (Exception e) {
                                        Log.e("Error!", "oops, some text fields missing values" + e.toString());
                                    }
                                    return null;
                                }
                            };
                            async.execute();
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                        }
                    }).show();
                } catch (Exception e) {
                    Log.e("Error!", "oops, some image fields missing values" + e.toString());
                }
                return true;
            case R.id.action_download:
                AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {
                            URL url = new URL(imageData.getJSONObject().getString("link"));
                            String type = imageData.getJSONObject().getString("link").split("/")[3];
                            File file = new File(android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + imageData.getJSONObject().getString("id") + "." + type);
                            URLConnection ucon = url.openConnection();
                            InputStream is = ucon.getInputStream();
                            BufferedInputStream bis = new BufferedInputStream(is);
                            ByteArrayBuffer baf = new ByteArrayBuffer(50);
                            int current = 0;
                            while ((current = bis.read()) != -1) {
                                baf.append((byte) current);
                            }
                            FileOutputStream fos = new FileOutputStream(file);
                            fos.write(baf.toByteArray());
                            fos.close();
                            activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                                    + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))));
                        } catch (Exception e) {
                            Log.e("Error!", e.toString());
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast;
                        toast = Toast.makeText(activity, "Downloaded!", duration);
                        toast.show();
                    }
                };
                async.execute();
                return true;
            case R.id.action_delete:
                new AlertDialog.Builder(activity).setTitle("Delete Image?")
                        .setMessage("Are you sure you want to delete this image? This cannot be undone")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... voids) {
                                        try {
                                            MainActivity activity = (MainActivity) getActivity();
                                            activity.makeCall("3/image/" + imageData.getJSONObject().getString("id"), "delete", null);
                                        } catch (Exception e) {
                                            Log.e("Error!", e.toString());
                                        }
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Void aVoid) {
                                        getActivity().getFragmentManager().popBackStack();
                                    }
                                };
                                async.execute();
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
                } catch (Exception e) {
                    Log.e("Error!", e.toString());
                }
                new AlertDialog.Builder(activity).setTitle("Set Link Type to Copy")
                        .setItems(copyTypes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                MainActivity activity = (MainActivity) getActivity();
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
                                } catch (Exception e) {
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
                } catch (Exception e) {
                    Log.e("Error!", "bad link to share");
                }
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void refreshComments() {
        commentAdapter.clear();
        commentAdapter.hiddenViews.clear();
        commentAdapter.notifyDataSetChanged();
        getComments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final MainActivity activity = (MainActivity) getActivity();
        SharedPreferences settings = activity.getSettings();
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
        if (activity.theme.equals(activity.HOLO_LIGHT))
            imageLayoutView = (LinearLayout) View.inflate(activity, R.layout.image_view, null);
        else
            imageLayoutView = (LinearLayout) View.inflate(activity, R.layout.dark_image_view, null);
        final WebView imageView = (WebView) imageLayoutView.findViewById(R.id.single_image_view);
        imageView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // do your handling codes here, which url is the requested url
                // probably you need to open that url rather than redirect:
                return false; // then it is not handled by default action
            }
        });

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
            mlp.setMargins(0, 0, 0, 4);
            updateImageFont();
            imageUpvote.setVisibility(View.VISIBLE);
            imageDownvote.setVisibility(View.VISIBLE);
            imageUser.setVisibility(View.VISIBLE);
            imageFavorite.setVisibility(View.VISIBLE);
            imageComment.setVisibility(View.VISIBLE);
            try {
                if (!imageData.getJSONObject().has("account_url") || imageData.getJSONObject().getString("account_url") == "null" || imageData.getJSONObject().getString("account_url") == "[deleted]")
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
            } catch (Exception e) {
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
                            if (activity.theme.equals(activity.HOLO_LIGHT))
                                imageFavorite.setImageResource(R.drawable.rating_favorite);
                            else
                                imageFavorite.setImageResource(R.drawable.dark_rating_favorite);
                        }
                    } catch (Exception e) {
                        Log.e("Error!", "missing data" + e.toString());
                    }

                    AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            MainActivity activity = (MainActivity) getActivity();
                            try {
                                activity.makeCall("3/image/" + imageData.getJSONObject().getString("id") + "/favorite", "post", null);
                            } catch (Exception e) {
                                Log.e("Error!", e.toString());
                            }
                            return null;
                        }
                    };
                    async.execute();
                }
            });
            imageUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        AccountFragment accountFragment = new AccountFragment(imageData.getJSONObject().getString("account_url"));
                        MainActivity activity = (MainActivity) getActivity();
                        activity.changeFragment(accountFragment);
                    } catch (Exception e) {
                        Log.e("Error!", e.toString());
                    }

                }
            });
            imageComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        MainActivity activity = (MainActivity) getActivity();
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
                                if (newBody.getText().toString().length() < 141) {
                                    AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... voids) {
                                            MainActivity activity = (MainActivity) getActivity();
                                            try {
                                                HashMap<String, Object> commentMap = new HashMap<String, Object>();
                                                commentMap.put("comment", newBody.getText().toString());
                                                commentMap.put("image_id", imageData.getJSONObject().getString("id"));
                                                activity.makeCall("3/comment/", "post", commentMap);
                                            } catch (Exception e) {
                                                Log.e("Error!", "oops, some text fields missing values" + e.toString());
                                            }
                                            return null;
                                        }

                                        @Override
                                        protected void onPostExecute(Void aVoid) {
                                            int duration = Toast.LENGTH_SHORT;
                                            Toast toast;
                                            toast = Toast.makeText(getActivity(), "Comment Posted!", duration);
                                            toast.show();
                                            refreshComments();
                                        }
                                    };
                                    async.execute();
                                } else {
                                    //do nothing
                                }
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();
                    } catch (Exception e) {
                        Log.e("Error!", "missing data");
                    }
                }
            });
            imageUpvote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (!imageData.getJSONObject().getString("vote").equals("up")) {
                            if (activity.theme.equals(activity.HOLO_LIGHT)) {
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
                            if (activity.theme.equals(activity.HOLO_LIGHT)) {
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
                    } catch (Exception e) {
                        Log.e("Error!", e.toString());
                    }
                    updateImageFont();
                    AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            MainActivity activity = (MainActivity) getActivity();
                            try {
                                activity.makeCall("3/gallery/" + imageData.getJSONObject().getString("id") + "/vote/up", "post", null);
                            } catch (Exception e) {
                                Log.e("Error!", e.toString());
                            }
                            return null;
                        }
                    };
                    async.execute();
                }
            });
            imageDownvote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (!imageData.getJSONObject().getString("vote").equals("down")) {
                            if (activity.theme.equals(activity.HOLO_LIGHT)) {
                                imageData.getJSONObject().put("vote", "down");
                                imageUpvote.setImageResource(R.drawable.rating_good);
                                imageDownvote.setImageResource(R.drawable.red_rating_bad);
                            } else {
                                imageUpvote.setImageResource(R.drawable.dark_rating_good);
                                imageDownvote.setImageResource(R.drawable.red_rating_bad);
                            }
                            imageData.getJSONObject().put("downs", (Integer.parseInt(imageData.getJSONObject().getString("downs")) + 1) + "");
                            if(imageData.getJSONObject().getString("vote").equals("up")) {
                                imageData.getJSONObject().put("score", (Integer.parseInt(imageData.getJSONObject().getString("score")) - 2) + "");
                                imageData.getJSONObject().put("ups", (Integer.parseInt(imageData.getJSONObject().getString("ups")) - 1) + "");
                            }
                            else {
                                imageData.getJSONObject().put("score", (Integer.parseInt(imageData.getJSONObject().getString("score")) - 1) + "");
                            }
                            imageData.getJSONObject().put("vote", "down");
                        } else {
                            if (activity.theme.equals(activity.HOLO_LIGHT)) {
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
                    } catch (Exception e) {
                        Log.e("Error!", e.toString());
                    }
                    updateImageFont();
                    AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            MainActivity activity = (MainActivity) getActivity();
                            try {
                                activity.makeCall("3/gallery/" + imageData.getJSONObject().getString("id") + "/vote/down", "post", null);
                            } catch (Exception e) {
                                Log.e("Error!", e.toString());
                            }
                            return null;
                        }
                    };
                    async.execute();
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
                AsyncTask<Void, Void, Bitmap> asyncTask = new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Void... voids) {
                        try {
                            URL url;
                            if (imageData.getJSONObject().has("cover"))
                                url = new URL("http://imgur.com/" + imageData.getJSONObject().getString("cover") + ".png");
                            else
                                url = new URL(imageData.getJSONObject().getString("link"));
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setDoInput(true);
                            connection.connect();
                            InputStream input = connection.getInputStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(input);
                            return bitmap;
                        } catch (Exception e) {
                            Log.e("Error!", e.toString());
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        Log.e("set zoomable view", "set");
                        zoomableImageView.setImageBitmap(bitmap);
                    }
                };
                asyncTask.execute();
            }
        });
        ArrayAdapter<String> tempAdapter = new ArrayAdapter<String>(mainView.getContext(),
                R.layout.drawer_list_item, mMenuList);


        Log.d("URI", "YO I'M IN YOUR SINGLE FRAGMENT gallery:" + inGallery);
        try {
            Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            if (imageData.getJSONObject().has("cover"))
                imageView.loadUrl("http://imgur.com/" + imageData.getJSONObject().getString("cover") + ".png");
            else
                imageView.loadUrl(imageData.getJSONObject().getString("link"));
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, imageData.getJSONObject().getInt("height"), getResources().getDisplayMetrics());
            int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, imageData.getJSONObject().getInt("width"), getResources().getDisplayMetrics());
            if (width < size.x)
                imageView.setLayoutParams(new TableRow.LayoutParams(
                        width, height));
            else
                imageView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
        } catch (Exception e) {
            Log.e("drawable Error!", e.toString());
        }
        TextView imageTitle = (TextView) imageLayoutView.findViewById(R.id.single_image_title);
        TextView imageDescription = (TextView) imageLayoutView.findViewById(R.id.single_image_description);

        try {
            String size = String.valueOf(NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("width"))) + "x" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("height")) + " (" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("size")) + " bytes)";
            String initial = imageData.getJSONObject().getString("type") + " | " + size + " | Views: " + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("views"));
            imageDetails.setText(initial);
            Log.d("imagedata", imageData.getJSONObject().toString());
            if (imageData.getJSONObject().getString("title") != "null")
                imageTitle.setText(imageData.getJSONObject().getString("title"));
            else
                imageTitle.setVisibility(View.GONE);
            if (imageData.getJSONObject().getString("description") != "null")
                imageDescription.setText(imageData.getJSONObject().getString("description"));
            else
                imageDescription.setVisibility(View.GONE);
            commentLayout.addHeaderView(imageLayoutView);
            commentLayout.setAdapter(tempAdapter);
        } catch (Exception e) {
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
                if (imageData.getJSONObject().getString("vote") != null && imageData.getJSONObject().getString("vote").equals("up"))
                    imageScore.setText(Html.fromHtml("<font color=#89c624>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("score")) + " points </font> (<font color=#89c624>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("ups")) + "</font>/<font color=#ee4444>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("downs")) + "</font>)"));
                else if (imageData.getJSONObject().getString("vote") != null && imageData.getJSONObject().getString("vote").equals("down"))
                    imageScore.setText(Html.fromHtml("<font color=#ee4444>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("score")) + " points </font> (<font color=#89c624>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("ups")) + "</font>/<font color=#ee4444>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("downs")) + "</font>)"));
                else
                    imageScore.setText(Html.fromHtml(NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("score")) + " points (<font color=#89c624>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("ups")) + "</font>/<font color=#ee4444>" + NumberFormat.getIntegerInstance().format(imageData.getJSONObject().getInt("downs")) + "</font>)"));
        }
        catch (Exception e) {
            Log.e("Error font!", e.toString());
        }
    }

    public void getComments() {
        MainActivity activity = (MainActivity) getActivity();
        if(!activity.getSettings().getBoolean("ShowComments", true))
            return;
        AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                if (inGallery) {
                    try {
                        commentData.setJSONObject(activity.makeCall("3/gallery/image/" + imageData.getJSONObject().getString("id") + "/comments", "get", null));
                    } catch (Exception e) {
                        Log.e("Error3!", e.toString());
                    }
                    Log.d("Gallery Image", "Getting comments..." + commentData.toString());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                try {
                    if (inGallery && commentAdapter != null) {
                        addComments();
                        commentAdapter.notifyDataSetChanged();
                    }

                } catch (Exception e) {
                    Log.e("Error2!", e.toString());
                }
            }
        };
        async.execute();

    }

    private void addComments() {
        try {
            Log.d("getting data", commentData.toString());
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
        } catch (Exception e) {
            Log.e("Error1!", e.toString());
        }
    }

    private void getIndents(JSONObject comment, int currentIndent) {
        JSONArray children;
        try {
            comment.put("indent", currentIndent);
            children = null;
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
        } catch (Exception e) {
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
            catch (Exception e) {
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
            catch (Exception e) {
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
        } catch (Exception e) {
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
        public LinearLayout buttons;
        public String id;
        public ImageButton upvote;
        public ImageButton downvote;
        public ImageButton reply;
        public ImageButton report;
        public ImageButton user;
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
            if (!hiddenViews.contains(Integer.valueOf(position)))
                hiddenViews.add(Integer.valueOf(position));
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
            ViewHolder holder;
            int type = getItemViewType(position);
            if (convertView == null || convertView.getTag() == null) {
                holder = new ViewHolder();
                switch (type) {
                    case HIDDEN_TYPE:
                        convertView = mInflater.inflate(R.layout.zerolayout, null);
                        return convertView;
                    case VISIBLE_TYPE:
                        MainActivity activity = (MainActivity) getActivity();
                        if (activity.theme.equals(activity.HOLO_LIGHT))
                            convertView = (LinearLayout) View.inflate(activity, R.layout.comment_list_item, null);
                        else
                            convertView = (LinearLayout) View.inflate(activity, R.layout.comment_list_item_dark, null);
                        holder = new ViewHolder();
                        holder.body = (TextView) convertView.findViewById(R.id.body);
                        holder.username = (TextView) convertView.findViewById(R.id.username);
                        holder.points = (TextView) convertView.findViewById(R.id.points);
                        holder.buttons = (LinearLayout) convertView.findViewById(R.id.comment_buttons);
                        holder.upvote = (ImageButton) holder.buttons.findViewById(R.id.rating_good);
                        holder.downvote = (ImageButton) holder.buttons.findViewById(R.id.rating_bad);
                        holder.user = (ImageButton) holder.buttons.findViewById(R.id.user);
                        holder.reply = (ImageButton) holder.buttons.findViewById(R.id.reply);
                        holder.report = (ImageButton) holder.buttons.findViewById(R.id.report);
                        holder.header = (RelativeLayout) convertView.findViewById(R.id.header);
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
            switch (getItemViewType(position)) {
                case VISIBLE_TYPE:
                    try {
                        holder.position = position;
                        holder.id = viewData.getString("id");
                        int indentLevel = viewData.getInt("indent");
                        holder.buttons.setVisibility(View.GONE);
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
                        holder.header.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                setDescendantsHidden((View) view.getParent().getParent().getParent());
                            }
                        });
                        holder.user.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    AccountFragment accountFragment = new AccountFragment(viewData.getString("author"));
                                    MainActivity activity = (MainActivity) getActivity();
                                    activity.changeFragment(accountFragment);
                                } catch (Exception e) {
                                    Log.e("Error!", e.toString());
                                }

                            }
                        });
                        convertView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                setDescendantsHidden(view);
                            }
                        });
                        holder.body.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                View convertView = (View) view.getParent().getParent().getParent();
                                ViewHolder viewHolder = (ViewHolder) convertView.getTag();
                                try {
                                    Log.d("testing", viewData.getString("author"));
                                    Log.d("testing", String.valueOf(viewData.getString("author").equals("[deleted]")));
                                    if (viewHolder.buttons.getVisibility() == View.GONE && !viewData.getString("author").equals("[deleted]"))
                                        viewHolder.buttons.setVisibility(View.VISIBLE);
                                    else
                                        viewHolder.buttons.setVisibility(View.GONE);
                                } catch (Exception e) {
                                    Log.e("Error!", e.toString());
                                }
                            }
                        });
                        if (viewData.getString("vote") != null && viewData.getString("vote").equals("up"))
                            holder.upvote.setImageResource(R.drawable.green_rating_good);
                        else if (viewData.getString("vote") != null && viewData.getString("vote").equals("down"))
                            holder.downvote.setImageResource(R.drawable.red_rating_bad);
                        holder.reply.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                LinearLayout layout = (LinearLayout) view.getParent().getParent();
                                final ViewHolder dataHolder = (ViewHolder) layout.getTag();
                                try {
                                    MainActivity activity = (MainActivity) getActivity();
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
                                            if (newBody.getText().toString().length() < 141) {
                                                AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                                                    @Override
                                                    protected Void doInBackground(Void... voids) {
                                                        MainActivity activity = (MainActivity) getActivity();
                                                        try {
                                                            Log.d("comment", dataHolder.id + newBody.getText().toString() + imageData.getJSONObject().getString("id"));
                                                            HashMap<String, Object> commentMap = new HashMap<String, Object>();
                                                            commentMap.put("comment", newBody.getText().toString());
                                                            commentMap.put("image_id", imageData.getJSONObject().getString("id"));
                                                            commentMap.put("parent_id", dataHolder.id);
                                                            activity.makeCall("3/comment/", "post", commentMap);
                                                        } catch (Exception e) {
                                                            Log.e("Error!", "oops, some text fields missing values" + e.toString());
                                                        }
                                                        return null;
                                                    }

                                                    @Override
                                                    protected void onPostExecute(Void aVoid) {
                                                        int duration = Toast.LENGTH_SHORT;
                                                        Toast toast;
                                                        toast = Toast.makeText(getActivity(), "Comment Posted!", duration);
                                                        toast.show();
                                                        refreshComments();
                                                    }
                                                };
                                                async.execute();
                                            } else {
                                                //do nothing
                                            }
                                        }
                                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            // Do nothing.
                                        }
                                    }).show();
                                } catch (Exception e) {
                                    Log.e("Error!", "missing data" + e.toString());
                                }
                            }
                        });
                        holder.upvote.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                MainActivity activity = (MainActivity) getActivity();
                                LinearLayout layout = (LinearLayout) view.getParent().getParent();
                                final ViewHolder dataHolder = (ViewHolder) layout.getTag();
                                try {
                                    if (!viewData.getString("vote").equals("up")) {
                                        dataHolder.upvote.setImageResource(R.drawable.green_rating_good);
                                        if (activity.theme.equals(activity.HOLO_LIGHT))
                                            dataHolder.downvote.setImageResource(R.drawable.rating_bad);
                                        else
                                            dataHolder.downvote.setImageResource(R.drawable.dark_rating_bad);
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
                                        if (activity.theme.equals(activity.HOLO_LIGHT)) {
                                            dataHolder.upvote.setImageResource(R.drawable.rating_good);
                                            dataHolder.downvote.setImageResource(R.drawable.rating_bad);
                                        } else {
                                            dataHolder.upvote.setImageResource(R.drawable.dark_rating_good);
                                            dataHolder.downvote.setImageResource(R.drawable.dark_rating_bad);
                                        }
                                        viewData.put("vote", "none");
                                    }
                                } catch (Exception e) {
                                    Log.e("Error!", e.toString());
                                }
                                updateFontColor(dataHolder, viewData);
                                AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... voids) {
                                        MainActivity activity = (MainActivity) getActivity();
                                        activity.makeCall("/3/comment/" + dataHolder.id + "/vote/up", "post", null);
                                        return null;
                                    }
                                };
                                async.execute();
                            }
                        });
                        holder.downvote.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                MainActivity activity = (MainActivity) getActivity();
                                LinearLayout layout = (LinearLayout) view.getParent().getParent();
                                final ViewHolder dataHolder = (ViewHolder) layout.getTag();
                                try {
                                    if (!viewData.getString("vote").equals("down")) {
                                        dataHolder.downvote.setImageResource(R.drawable.red_rating_bad);
                                        if (activity.theme.equals(activity.HOLO_LIGHT)) {
                                            dataHolder.upvote.setImageResource(R.drawable.rating_good);
                                        } else {
                                            dataHolder.upvote.setImageResource(R.drawable.dark_rating_good);
                                        }
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
                                        if (activity.theme.equals(activity.HOLO_LIGHT)) {
                                            dataHolder.upvote.setImageResource(R.drawable.rating_good);
                                            dataHolder.downvote.setImageResource(R.drawable.rating_bad);
                                        } else {
                                            dataHolder.upvote.setImageResource(R.drawable.dark_rating_good);
                                            dataHolder.downvote.setImageResource(R.drawable.dark_rating_bad);
                                        }
                                        viewData.put("vote", "none");
                                    }
                                } catch (Exception e) {
                                    Log.e("Error!", e.toString());
                                }
                                updateFontColor(dataHolder, viewData);
                                AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... voids) {
                                        MainActivity activity = (MainActivity) getActivity();
                                        activity.makeCall("/3/comment/" + dataHolder.id + "/vote/down", "post", null);
                                        return null;
                                    }
                                };
                                async.execute();
                            }
                        });
                        holder.report.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                            }
                        });
                        if (viewData.has("hidden") && viewData.getInt("hidden") == ViewHolder.VIEW_HIDDEN) {
                            holder.body.setVisibility(View.GONE);
                        } else
                            holder.body.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        Log.e("View Error!", e.toString());
                    }
                    convertView.setTag(holder);
                    return convertView;
                case HIDDEN_TYPE:
                    return convertView;
                default:
                    return convertView;
            }
        }
        private void updateFontColor(ViewHolder dataHolder, JSONObject viewData) {
            MainActivity activity = (MainActivity) getActivity();
            String username = "";
            try {
                if (viewData.getString("author").length() < 25)
                   username = viewData.getString("author") + " &#8226; ";
                else
                   username = viewData.getString("author").substring(0, 25) + "..." + " &#8226; ";
                if(activity.getSettings().getBoolean("ShowVotes", true))
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
            catch (Exception e) {
                Log.e("Error!", e.toString());
            }
        }
    }
}