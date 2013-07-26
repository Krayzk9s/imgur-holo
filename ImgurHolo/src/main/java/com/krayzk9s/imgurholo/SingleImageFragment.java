package com.krayzk9s.imgurholo;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Kurt Zimmer on 7/22/13.
 */
public class SingleImageFragment extends Fragment {

    String[] mMenuList;
    JSONObject imageData;
    JSONObject commentData;
    Boolean inGallery;
    CommentAdapter commentAdapter;
    View mainView;
    ListView commentLayout;

    public SingleImageFragment() {
        inGallery = false;
    }

    public void setParams(JSONObject _params) {
        imageData = _params;
    }

    public void setGallery(Boolean gallery) {
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
        menu.findItem(R.id.action_share).setVisible(true);
        menu.findItem(R.id.action_copy).setVisible(true);
        menu.findItem(R.id.action_upload).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        MainActivity activity = (MainActivity) getActivity();
        switch (item.getItemId()) {
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
                                            link = "http://imgur.com/" + imageData.getString("id");
                                            break;
                                        case 1:
                                            link = imageData.getString("link");
                                            break;
                                        case 2:
                                            link = "<a href=\"http://imgur.com/" + imageData.getString("id") + "\"><img src=\"" + imageData.getString("link") + "\" title=\"Hosted by imgur.com\"/></a>";
                                            break;
                                        case 3:
                                            link = "[IMG]" + imageData.getString("link") + "[/IMG]";
                                            break;
                                        case 4:
                                            link = "[URL=http://imgur.com/" + imageData.getString("id") + "][IMG]" + imageData.getString("link") + "[/IMG][/URL]";
                                            break;
                                        case 5:
                                            link = "[Imgur](http://i.imgur.com/" + imageData.getString("id") + ")";
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
                    intent.putExtra(Intent.EXTRA_TEXT, imageData.getString("link"));
                } catch (Exception e) {
                    Log.e("Error!", "bad link to share");
                }
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        ImageView imageView = (ImageView) imageLayoutView.findViewById(R.id.single_image_view);
        if (inGallery) {
            LinearLayout layout = (LinearLayout) imageLayoutView.findViewById(R.id.image_buttons);
            layout.setVisibility(View.VISIBLE);
        }
        ArrayAdapter<String> tempAdapter = new ArrayAdapter<String>(mainView.getContext(),
                R.layout.drawer_list_item, mMenuList);


        Log.d("URI", "YO I'M IN YOUR SINGLE FRAGMENT gallery:" + inGallery);
        try {
            UrlImageViewHelper.setUrlDrawable(imageView, imageData.getString("link"), R.drawable.icon);
        } catch (Exception e) {
            Log.e("drawable Error!", e.toString());
        }
        TextView imageDetails = (TextView) imageLayoutView.findViewById(R.id.single_image_details);
        TextView imageTitle = (TextView) imageLayoutView.findViewById(R.id.single_image_title);
        try {
            String size = String.valueOf(imageData.getInt("width")) + "x" + String.valueOf(imageData.getInt("height")) + " (" + String.valueOf(imageData.getInt("size")) + " bytes)";
            Calendar accountCreationDate = Calendar.getInstance();
            accountCreationDate.setTimeInMillis((long) imageData.getInt("datetime") * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            String accountCreated = sdf.format(accountCreationDate.getTime());
            imageDetails.setText(imageData.getString("type") + " | " + size + " | Views: " + String.valueOf(imageData.getInt("views")));
            if (imageData.getString("title") != "null")
                imageTitle.setText(imageData.getString("title"));
            commentLayout.addHeaderView(imageLayoutView);
            commentLayout.setAdapter(tempAdapter);
        } catch (Exception e) {
            Log.e("Text Error!", e.toString());
        }
        AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                if (inGallery) {
                    try {
                        commentData = activity.makeGetCall("3/gallery/image/" + imageData.getString("id") + "/comments");
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
        return mainView;
    }

    private void addComments() {
        try {
            Log.d("getting data", commentData.toString());
            JSONArray commentArray = commentData.getJSONArray("data");
            Log.d("calling indent function", commentArray.toString());
            for (int i = 0; i < commentArray.length(); i++) {
                getIndents(commentArray.getJSONObject(i), 0);
            }

        } catch (Exception e) {
            Log.e("Error!", e.toString());
        }
    }

    private void getIndents(JSONObject comment, int currentIndent) {
        JSONArray children;
        try {
            Log.d("Putting Indent", comment.toString());
            comment.put("indent", currentIndent);
            if (comment.has("children")) {
                children = comment.getJSONArray("children");
                Log.d("Got children", children.toString());
                for (int i = 0; i < children.length(); i++) {
                    JSONObject child = children.getJSONObject(i);
                    Log.d("Got child", child.toString());
                    getIndents(child, currentIndent++);
                }
                comment.remove("children");
            }
        } catch (Exception e) {
            Log.e("Error!", e.toString());
        }
        commentAdapter.add(comment);
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

    public class CommentAdapter extends ArrayAdapter<JSONObject> {
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
            JSONObject viewData = this.getItem(position);
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
                holder.reply.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LinearLayout layout = (LinearLayout) view.getParent().getParent();
                        final ViewHolder dataHolder = (ViewHolder) layout.getTag();
                        try {
                            MainActivity activity = (MainActivity) getActivity();
                            final EditText newBody = new EditText(activity);
                            newBody.setHint("Body");
                            new AlertDialog.Builder(activity).setTitle("Send Message")
                                    .setView(newBody).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... voids) {
                                            MainActivity activity = (MainActivity) getActivity();
                                            try {
                                                Log.d("comment", dataHolder.id + newBody.getText().toString() + imageData.getString("id"));
                                                activity.makeGalleryReply(dataHolder.id, newBody.getText().toString(), imageData.getString("id"));
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
}