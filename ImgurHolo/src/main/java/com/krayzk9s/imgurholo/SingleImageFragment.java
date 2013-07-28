package com.krayzk9s.imgurholo;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import uk.co.senab.photoview.PhotoViewAttacher;

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
    ImageButton imageReport;
    ArrayList<JSONParcelable> commentArray;
    PhotoViewAttacher mAttacher;

    public SingleImageFragment() {
        inGallery = false;
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
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        if(!inGallery)
            menu.findItem(R.id.action_delete).setVisible(true);
        menu.findItem(R.id.action_share).setVisible(true);
        menu.findItem(R.id.action_download).setVisible(true);
        menu.findItem(R.id.action_copy).setVisible(true);
        menu.findItem(R.id.action_upload).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        final MainActivity activity = (MainActivity) getActivity();
        switch (item.getItemId()) {
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
                        }
                        catch (Exception e) {
                            Log.e("Error!", e.toString());
                        }
                        return null;
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
                                            String deletehash = imageData.getJSONObject().getString("deletehash");
                                            MainActivity activity = (MainActivity) getActivity();
                                            activity.deleteImage(deletehash);
                                        }
                                        catch (Exception e) {
                                            Log.e("Error!", e.toString());
                                        }
                                        return null;
                                    }
                                    protected void onPostExecute(Void... voids) {
                                        finishActivity();
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
                new AlertDialog.Builder(activity).setTitle("Set Link Type to Copy")
                        .setItems(R.array.copyTypes, new DialogInterface.OnClickListener() {
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

    public void finishActivity() {
        getActivity().getFragmentManager().popBackStack();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mainView = inflater.inflate(R.layout.single_image_layout, container, false);
        mMenuList = getResources().getStringArray(R.array.emptyList);
        commentAdapter = new CommentAdapter(mainView.getContext(),
                R.id.comment_item);
        commentLayout = (ListView) mainView.findViewById(R.id.comment_thread);
        MainActivity activity = (MainActivity) getActivity();
        LinearLayout imageLayoutView = (LinearLayout) View.inflate(activity, R.layout.image_view, null);
        WebView imageView = (WebView) imageLayoutView.findViewById(R.id.single_image_view);
        if(savedInstanceState != null) {
            imageData = savedInstanceState.getParcelable("imageData");
        }
        if (inGallery) {
            LinearLayout layout = (LinearLayout) imageLayoutView.findViewById(R.id.image_buttons);
            layout.setVisibility(View.VISIBLE);
            imageUpvote = (ImageButton) imageLayoutView.findViewById(R.id.rating_good);
            imageDownvote = (ImageButton) imageLayoutView.findViewById(R.id.rating_bad);
            imageFavorite = (ImageButton) imageLayoutView.findViewById(R.id.rating_favorite);
            imageComment = (ImageButton) imageLayoutView.findViewById(R.id.comment);
            imageReport = (ImageButton) imageLayoutView.findViewById(R.id.report);
            try {
                if(imageData.getJSONObject().getString("vote") != null && imageData.getJSONObject().getString("vote").equals("up"))
                    imageUpvote.setImageResource(R.drawable.green_rating_good);
                else if (imageData.getJSONObject().getString("vote") != null && imageData.getJSONObject().getString("vote").equals("down"))
                    imageDownvote.setImageResource(R.drawable.red_rating_bad);
                if(imageData.getJSONObject().getBoolean("favorite"))
                    imageFavorite.setImageResource(R.drawable.green_rating_favorite);
            }
            catch (Exception e)
            {
                Log.e("Error!", e.toString());
            }
            imageFavorite.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    imageFavorite.setImageResource(R.drawable.green_rating_favorite);
                    AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            MainActivity activity = (MainActivity) getActivity();
                            try {
                                activity.makePostCall("3/image/" + imageData.getJSONObject().getString("id") + "/favorite");
                            }
                            catch (Exception e)
                            {
                                Log.e("Error!", e.toString());
                            }
                            return null;
                        }
                    };
                    async.execute();
                }
            });
            imageComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        MainActivity activity = (MainActivity) getActivity();
                        final EditText newBody = new EditText(activity);
                        newBody.setHint("Body");
                        new AlertDialog.Builder(activity).setTitle("Comment on Image")
                                .setView(newBody).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... voids) {
                                        MainActivity activity = (MainActivity) getActivity();
                                        try {
                                            activity.makeGalleryReply(imageData.getJSONObject().getString("id"), newBody.getText().toString(), null);
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
                        Log.e("Error!", "missing data");
                    }
                }
            });
            imageUpvote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    imageUpvote.setImageResource(R.drawable.green_rating_good);
                    imageDownvote.setImageResource(R.drawable.rating_bad);
                    AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            MainActivity activity = (MainActivity) getActivity();
                            try {
                                activity.makePostCall("3/gallery/" + imageData.getJSONObject().getString("id") + "/vote/up");
                            }
                            catch (Exception e)
                            {
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
                    imageUpvote.setImageResource(R.drawable.rating_good);
                    imageDownvote.setImageResource(R.drawable.red_rating_bad);
                    AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            MainActivity activity = (MainActivity) getActivity();
                            try {
                                activity.makePostCall("3/gallery/" + imageData.getJSONObject().getString("id") + "/vote/down");
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
        ArrayAdapter<String> tempAdapter = new ArrayAdapter<String>(mainView.getContext(),
                R.layout.drawer_list_item, mMenuList);


        Log.d("URI", "YO I'M IN YOUR SINGLE FRAGMENT gallery:" + inGallery);
        try {
            Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            imageView.loadUrl(imageData.getJSONObject().getString("link"));
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, imageData.getJSONObject().getInt("height"), getResources().getDisplayMetrics());
            int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, imageData.getJSONObject().getInt("width"), getResources().getDisplayMetrics());
            if(width < size.x)
                imageView.setLayoutParams(new TableRow.LayoutParams(
                        width, height));
            else
                imageView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
            /*
            if(imageData.getJSONObject().has("cover"))
                UrlImageViewHelper.setUrlDrawable(imageView, "http://imgur.com/" + imageData.getJSONObject().getString("cover") + ".png", R.drawable.icon);
            else
                UrlImageViewHelper.setUrlDrawable(imageView, imageData.getJSONObject().getString("link"), R.drawable.icon);
            mAttacher = new PhotoViewAttacher(imageView);*/
        } catch (Exception e) {
            Log.e("drawable Error!", e.toString());
        }
        TextView imageDetails = (TextView) imageLayoutView.findViewById(R.id.single_image_details);
        TextView imageTitle = (TextView) imageLayoutView.findViewById(R.id.single_image_title);
        try {
            Log.d("imagedata", imageData.getJSONObject().toString());
            String size = String.valueOf(imageData.getJSONObject().getInt("width")) + "x" + String.valueOf(imageData.getJSONObject().getInt("height")) + " (" + String.valueOf(imageData.getJSONObject().getInt("size")) + " bytes)";
            Calendar accountCreationDate = Calendar.getInstance();
            accountCreationDate.setTimeInMillis((long) imageData.getJSONObject().getInt("datetime") * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            String accountCreated = sdf.format(accountCreationDate.getTime());
            imageDetails.setText(imageData.getJSONObject().getString("type") + " | " + size + " | Views: " + String.valueOf(imageData.getJSONObject().getInt("views")));
            if (imageData.getJSONObject().getString("title") != "null")
                imageTitle.setText(imageData.getJSONObject().getString("title"));
            commentLayout.addHeaderView(imageLayoutView);
            commentLayout.setAdapter(tempAdapter);
        } catch (Exception e) {
            Log.e("Text Error!", e.toString());
        }

        if(savedInstanceState == null) {
        AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                if (inGallery) {
                    try {
                        commentData = new JSONParcelable();
                        commentData.setJSONObject(activity.makeGetCall("3/gallery/image/" + imageData.getJSONObject().getString("id") + "/comments"));
                    } catch (Exception e) {
                        Log.e("Error!", e.toString());
                    }
                    Log.d("Gallery Image", "Getting comments..." + commentData.toString());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                try {
                    if (inGallery) {
                        AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                addComments();
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                commentAdapter.notifyDataSetChanged();
                                commentLayout.setAdapter(commentAdapter);
                                Log.d("Gallery Image", "Data set changed");
                            }
                        };
                        async.execute();
                    }

                } catch (Exception e) {
                    Log.e("Error!", e.toString());
                }
            }
        };
        async.execute();
        }
        else {
            commentArray = savedInstanceState.getParcelableArrayList("commentData");
            commentAdapter.addAll(commentArray);
            commentLayout.setAdapter(commentAdapter);
            commentAdapter.notifyDataSetChanged();
        }
        return mainView;
    }

    private void addComments() {
        try {
            Log.d("getting data", commentData.toString());
            JSONArray commentJSONArray = commentData.getJSONObject().getJSONArray("data");
            commentArray = new ArrayList<JSONParcelable>();
            Log.d("calling indent function", commentJSONArray.toString());
            for (int i = 0; i < commentJSONArray.length(); i++) {
                getIndents(commentJSONArray.getJSONObject(i), 0);
            }
           commentAdapter.addAll(commentArray);
        } catch (Exception e) {
            Log.e("Error!", e.toString());
        }
    }

    private void getIndents(JSONObject comment, int currentIndent) {
        JSONArray children;
        try {
            Log.d("Putting Indent", comment.toString());
            Log.d("Indent", "" + currentIndent);
            comment.put("indent", currentIndent);
            Log.d("Put Indent", comment.toString());
            children = null;
            if (comment.has("children")) {
                children = comment.getJSONArray("children");
                comment.remove("children");
            }
            JSONParcelable commentParse = new JSONParcelable();
            commentParse.setJSONObject(comment);
            commentArray.add(commentParse);
            if (children != null) {
                Log.d("Got children", children.toString());
                for (int i = 0; i < children.length(); i++) {
                    JSONObject child = children.getJSONObject(i);
                    Log.d("Got child", child.toString());
                    getIndents(child, currentIndent + 1);
                }

            }
        } catch (Exception e) {
            Log.e("Error!", e.toString());
        }
    }

    private static class ViewHolder {
        public TextView header;
        public TextView body;
        public View[] indentViews;
        public LinearLayout buttons;
        public String id;
        public ImageButton upvote;
        public ImageButton downvote;
        public ImageButton reply;
        public ImageButton report;
    }

    public class CommentAdapter extends ArrayAdapter<JSONParcelable> {
        private LayoutInflater mInflater;

        public CommentAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.comment_list_item, null);
                holder = new ViewHolder();
                holder.body = (TextView) convertView.findViewById(R.id.body);
                holder.header = (TextView) convertView.findViewById(R.id.header);
                holder.buttons = (LinearLayout) convertView.findViewById(R.id.comment_buttons);
                holder.upvote = (ImageButton) holder.buttons.findViewById(R.id.rating_good);
                holder.downvote = (ImageButton) holder.buttons.findViewById(R.id.rating_bad);
                holder.reply = (ImageButton) holder.buttons.findViewById(R.id.reply);
                holder.report = (ImageButton) holder.buttons.findViewById(R.id.report);
                holder.id = "";
                holder.indentViews = new View[]{
                        convertView.findViewById(R.id.margin_1),
                        convertView.findViewById(R.id.margin_2),
                        convertView.findViewById(R.id.margin_3),
                        convertView.findViewById(R.id.margin_4),
                        convertView.findViewById(R.id.margin_5),
                        convertView.findViewById(R.id.margin_6)
                };
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            JSONObject viewData = this.getItem(position).getJSONObject();
            try {
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
                holder.header.setText(viewData.getString("author") + " " + viewData.getString("points") + "pts (" + viewData.getString("ups") + "/" + viewData.getString("downs") + ")");
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ViewHolder viewHolder = (ViewHolder) view.getTag();
                        if (viewHolder.buttons.getVisibility() == View.GONE)
                            viewHolder.buttons.setVisibility(View.VISIBLE);
                        else
                            viewHolder.buttons.setVisibility(View.GONE);
                    }
                });
                if(imageData.getJSONObject().getString("vote") != null && imageData.getJSONObject().getString("vote").equals("up"))
                    holder.upvote.setImageResource(R.drawable.green_rating_good);
                else if(imageData.getJSONObject().getString("vote") != null && imageData.getJSONObject().getString("vote").equals("down"))
                    holder.downvote.setImageResource(R.drawable.red_rating_bad);
                holder.reply.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LinearLayout layout = (LinearLayout) view.getParent().getParent();
                        final ViewHolder dataHolder = (ViewHolder) layout.getTag();
                        try {
                            MainActivity activity = (MainActivity) getActivity();
                            final EditText newBody = new EditText(activity);
                            newBody.setHint("Body");
                            new AlertDialog.Builder(activity).setTitle("Reply to Comment")
                                    .setView(newBody).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... voids) {
                                            MainActivity activity = (MainActivity) getActivity();
                                            try {
                                                Log.d("comment", dataHolder.id + newBody.getText().toString() + imageData.getJSONObject().getString("id"));
                                                activity.makeGalleryReply(imageData.getJSONObject().getString("id"), newBody.getText().toString(), dataHolder.id);
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
                            Log.e("Error!", "missing data");
                        }
                    }
                });
                holder.upvote.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LinearLayout layout = (LinearLayout) view.getParent().getParent();
                        final ViewHolder dataHolder = (ViewHolder) layout.getTag();
                        dataHolder.upvote.setImageResource(R.drawable.green_rating_good);
                        dataHolder.downvote.setImageResource(R.drawable.rating_bad);
                        AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                MainActivity activity = (MainActivity) getActivity();
                                activity.makePostCall("/3/comment/" + dataHolder.id + "/vote/up");
                                return null;
                            }
                        };
                        async.execute();
                    }
                });
                holder.downvote.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LinearLayout layout = (LinearLayout) view.getParent().getParent();
                        final ViewHolder dataHolder = (ViewHolder) layout.getTag();
                        dataHolder.upvote.setImageResource(R.drawable.rating_good);
                        dataHolder.downvote.setImageResource(R.drawable.red_rating_bad);
                        AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                MainActivity activity = (MainActivity) getActivity();
                                activity.makePostCall("/3/comment/" + dataHolder.id + "/vote/down");
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
                convertView.setTag(holder);
            } catch (Exception e) {
                Log.e("Error!", e.toString());
            }
            return convertView;
        }
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable("imageData", imageData);
        savedInstanceState.putParcelableArrayList("commentData", commentArray);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
}