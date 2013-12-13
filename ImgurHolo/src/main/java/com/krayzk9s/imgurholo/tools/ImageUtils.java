package com.krayzk9s.imgurholo.tools;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.libs.ZoomableImageView;
import com.krayzk9s.imgurholo.ui.SingleImageFragment;

import org.json.JSONException;

import java.text.NumberFormat;

/**
 * Created by info on 12/13/13.
 */
public class ImageUtils {
    public static void favoriteImage(GetData fragment, JSONParcelable imageData, ImageButton imageFavorite, ApiCall apiCall) {
        try {
            if (!imageData.getJSONObject().getBoolean("favorite")) {
                imageFavorite.setImageResource(R.drawable.green_rating_favorite);
                imageData.getJSONObject().put("favorite", true);
            } else {
                imageData.getJSONObject().put("favorite", false);
                if (apiCall.settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
                    imageFavorite.setImageResource(R.drawable.rating_favorite);
                else
                    imageFavorite.setImageResource(R.drawable.dark_rating_favorite);
            }
            Fetcher fetcher = new Fetcher(fragment, "3/image/" + imageData.getJSONObject().getString("id") + "/favorite", ApiCall.POST, null, apiCall, SingleImageFragment.FAVORITE);
            fetcher.execute();
        } catch (JSONException e) {
            Log.e("Error!", "missing data" + e.toString());
        }
    }
    public static void gotoUser(android.support.v4.app.Fragment fragment, JSONParcelable imageData) {
        try {
            Intent intent = new Intent();
            intent.putExtra("username", imageData.getJSONObject().getString("account_url"));
            intent.setAction(ImgurHoloActivity.ACCOUNT_INTENT);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            fragment.startActivity(intent);
        } catch (JSONException e) {
            Log.e("Error!", e.toString());
        }
    }

    public static void upVote(GetData fragment, JSONParcelable imageData, ImageButton imageUpvote, ImageButton imageDownvote, ApiCall apiCall) {
        try {
            if (!imageData.getJSONObject().getString("vote").equals("up")) {
                if (apiCall.settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT)) {
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
                if (apiCall.settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT)) {
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
        try {
            Fetcher fetcher = new Fetcher(fragment, "3/gallery/" + imageData.getJSONObject().getString("id") + "/vote/up", ApiCall.POST, null, apiCall, SingleImageFragment.UPVOTE);
            fetcher.execute();
        }
        catch (Exception e) {
            Log.e("Error!", e.toString());
        }
    }
    public static void downVote(GetData fragment, JSONParcelable imageData, ImageButton imageUpvote, ImageButton imageDownvote, ApiCall apiCall) {
        try {
                if (!imageData.getJSONObject().getString("vote").equals("down")) {
                    if (apiCall.settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT)) {
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
                    if (apiCall.settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT)) {
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
            try {
                Fetcher fetcher = new Fetcher(fragment, "3/gallery/" + imageData.getJSONObject().getString("id") + "/vote/down", ApiCall.POST, null, apiCall, SingleImageFragment.UPVOTE);
                fetcher.execute();
            }
            catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
    }
    public static void fullscreen(android.support.v4.app.Fragment fragment, JSONParcelable imageData, PopupWindow popupWindow, View mainView) {
        if (popupWindow != null) {
            popupWindow.dismiss();
        }
        popupWindow = new PopupWindow();
        popupWindow.setBackgroundDrawable(new ColorDrawable(0x80000000));
        popupWindow.setFocusable(true);
        popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        final ZoomableImageView zoomableImageView = new ZoomableImageView(fragment.getActivity());
        popupWindow.setContentView(zoomableImageView);
        popupWindow.showAtLocation(mainView, Gravity.TOP, 0, 0);
        LoadImageAsync loadImageAsync = new LoadImageAsync(zoomableImageView, imageData);
        loadImageAsync.execute();
    }
    public static void updateImageFont(JSONParcelable imageData, TextView imageScore) {
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
}
