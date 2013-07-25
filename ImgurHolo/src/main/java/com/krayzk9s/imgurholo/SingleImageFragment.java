package com.krayzk9s.imgurholo;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mainView = inflater.inflate(R.layout.single_image_layout, container, false);


        mMenuList = getResources().getStringArray(R.array.emptyList);
        commentAdapter = new CommentAdapter(mainView.getContext(),
                R.id.comment_item);
        commentLayout = (ListView) mainView.findViewById(R.id.comment_thread);
        MainActivity activity = (MainActivity)getActivity();
        LinearLayout imageLayoutView = (LinearLayout) View.inflate(activity, R.layout.image_view, null);
        ImageView imageView = (ImageView) imageLayoutView.findViewById(R.id.single_image_view);
        if(inGallery) {
            LinearLayout layout = (LinearLayout)imageLayoutView.findViewById(R.id.image_buttons);
            layout.setVisibility(View.VISIBLE);
        }
        ArrayAdapter<String> tempAdapter = new ArrayAdapter<String>(mainView.getContext(),
                R.layout.drawer_list_item, mMenuList);



        Log.d("URI", "YO I'M IN YOUR SINGLE FRAGMENT gallery:" + inGallery);
        try {
            UrlImageViewHelper.setUrlDrawable(imageView, imageData.getString("link"), R.drawable.icon);
        }
        catch (Exception e) {
            Log.e("drawable Error!", e.toString());
        }
        TextView imageDetails = (TextView)imageLayoutView.findViewById(R.id.single_image_details);
        TextView imageTitle = (TextView)imageLayoutView.findViewById(R.id.single_image_title);
        try {
            String size = String.valueOf(imageData.getInt("width")) + "x" + String.valueOf(imageData.getInt("height")) + " (" + String.valueOf(imageData.getInt("size")) + " bytes)";
            Calendar accountCreationDate = Calendar.getInstance();
            accountCreationDate.setTimeInMillis((long) imageData.getInt("datetime") * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            String accountCreated = sdf.format(accountCreationDate.getTime());
            imageDetails.setText(imageData.getString("type") + " | " + size + " | Views: " + String.valueOf(imageData.getInt("views")));
            if(imageData.getString("title") != "null")
                imageTitle.setText(imageData.getString("title"));
            commentLayout.addHeaderView(imageLayoutView);
            commentLayout.setAdapter(tempAdapter);
    }
    catch (Exception e) {
        Log.e("Text Error!", e.toString());
    }


        AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                if(inGallery)
                {
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
                    if(inGallery) {
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

    private void addComments()
    {
        try {
            Log.d("getting data", commentData.toString());
            JSONArray commentArray = commentData.getJSONArray("data");
            Log.d("calling indent function", commentArray.toString());
            for(int i = 0; i < commentArray.length(); i++)
            {
                getIndents(commentArray.getJSONObject(i), 0);
            }

        } catch (Exception e) {
            Log.e("Error!", e.toString());
        }
    }


    private void getIndents(JSONObject comment, int currentIndent)
    {
        JSONArray children;
        try {
            Log.d("Putting Indent", comment.toString());
            comment.put("indent", currentIndent);
            if(comment.has("children"))
            {
                children = comment.getJSONArray("children");
                Log.d("Got children", children.toString());
                for(int i = 0; i < children.length(); i++) {
                    JSONObject child = children.getJSONObject(i);
                    Log.d("Got child", child.toString());
                    getIndents(child, currentIndent++);
                }
                comment.remove("children");
            }
        }
        catch (Exception e)
        {
            Log.e("Error!", e.toString());
        }
        commentAdapter.add(comment);
    }


    public class CommentAdapter extends ArrayAdapter<JSONObject>
    {
        private LayoutInflater mInflater;

        public CommentAdapter(Context context, int textViewResourceId)
        {
            super(context, textViewResourceId);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder;
            if(convertView == null)
            {
                convertView = mInflater.inflate(R.layout.comment_list_item, null);
                holder = new ViewHolder();
                holder.body = (TextView)convertView.findViewById(R.id.body);
                holder.header = (TextView)convertView.findViewById(R.id.header);
                holder.buttons = (LinearLayout)convertView.findViewById(R.id.comment_buttons);
                holder.indentViews = new View[] {
                        convertView.findViewById(R.id.margin_1),
                        convertView.findViewById(R.id.margin_2),
                        convertView.findViewById(R.id.margin_3),
                        convertView.findViewById(R.id.margin_4),
                        convertView.findViewById(R.id.margin_5),
                        convertView.findViewById(R.id.margin_6)
                };
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            JSONObject viewData = this.getItem(position);
            try {
                int indentLevel = viewData.getInt("indent");
                holder.buttons.setVisibility(View.GONE);
                for (int i = 0; i < indentLevel && i < holder.indentViews.length; i++) {
                    holder.indentViews[i].setVisibility(View.VISIBLE);
                }
                for (int i = indentLevel; i < holder.indentViews.length; i++) {
                    holder.indentViews[i].setVisibility(View.GONE);
                }
                holder.body.setText(viewData.getString("comment"));
                holder.header.setText(viewData.getString("author") + " " + viewData.getString("points") + "pts (" + viewData.getString("ups") + "/" + viewData.getString("downs") + ")");
                convertView.setTag(holder);
            }
            catch (Exception e) {
                Log.e("Error!", e.toString());
            }
            return convertView;
        }
    }
    private static class ViewHolder {
        public TextView header;
        public TextView body;
        public View[] indentViews;
        public LinearLayout buttons;
    }
}