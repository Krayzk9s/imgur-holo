package com.krayzk9s.imgurholo.tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.libs.ZoomableImageView;
import com.krayzk9s.imgurholo.services.DownloadService;
import com.krayzk9s.imgurholo.ui.SingleImageFragment;

import org.json.JSONException;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Copyright 2013 Kurt Zimmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
					if (imageUpvote != null && imageDownvote != null && apiCall.settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT)) {
						imageUpvote.setImageResource(R.drawable.green_rating_good);
						imageDownvote.setImageResource(R.drawable.rating_bad);
					} else if (imageUpvote != null && imageDownvote != null) {
						imageUpvote.setImageResource(R.drawable.green_rating_good);
						imageDownvote.setImageResource(R.drawable.dark_rating_bad);
					}
					imageData.getJSONObject().put("ups", (Integer.parseInt(imageData.getJSONObject().getString("ups")) + 1) + "");
					if (imageData.getJSONObject().getString("vote").equals("down")) {
						imageData.getJSONObject().put("score", (Integer.parseInt(imageData.getJSONObject().getString("score")) + 2) + "");
						imageData.getJSONObject().put("downs", (Integer.parseInt(imageData.getJSONObject().getString("downs")) - 1) + "");
					} else {
						imageData.getJSONObject().put("score", (Integer.parseInt(imageData.getJSONObject().getString("score")) + 1) + "");
					}
					imageData.getJSONObject().put("vote", "up");
				} else {
					if (imageUpvote != null && imageDownvote != null && apiCall.settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT)) {
						imageUpvote.setImageResource(R.drawable.rating_good);
						imageDownvote.setImageResource(R.drawable.rating_bad);
					} else if (imageUpvote != null && imageDownvote != null) {
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
		} catch (Exception e) {
			Log.e("Error!", e.toString());
		}
	}

	public static void downVote(GetData fragment, JSONParcelable imageData, ImageButton imageUpvote, ImageButton imageDownvote, ApiCall apiCall) {
			try {
				if (!imageData.getJSONObject().getString("vote").equals("down")) {
					if (apiCall.settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT)) {
						if (imageUpvote != null && imageDownvote != null) {
							imageUpvote.setImageResource(R.drawable.rating_good);
							imageDownvote.setImageResource(R.drawable.red_rating_bad);
						}
					} else if (imageUpvote != null && imageDownvote != null) {
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
					if (apiCall.settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT) && imageUpvote != null && imageDownvote != null) {
						imageUpvote.setImageResource(R.drawable.rating_good);
						imageDownvote.setImageResource(R.drawable.rating_bad);
					} else if (imageUpvote != null && imageDownvote != null) {
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
		} catch (JSONException e) {
			Log.e("Error!", e.toString());
		}
	}

	public static void fullscreen(android.support.v4.app.Fragment fragment, JSONParcelable imageData, PopupWindow popupWindow, View mainView) {
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
		} catch (JSONException e) {
			Log.e("Error font!", e.toString());
		}
	}

	public static void updateInfoFont(JSONParcelable imageData, TextView infoText) {
		try {
			String albumText = "";
			if (imageData.getJSONObject().has("is_album") && imageData.getJSONObject().getBoolean("is_album"))
				albumText = "[album] ";
			if (!imageData.getJSONObject().getString("section").equals("null"))
				albumText += "/r/" + imageData.getJSONObject().getString("section") + " " + Html.fromHtml("&#8226;") + " ";
			Calendar calendar = Calendar.getInstance();
			long now = calendar.getTimeInMillis();
			infoText.setText(albumText + DateUtils.getRelativeTimeSpanString(imageData.getJSONObject().getLong("datetime") * 1000, now, DateUtils.MINUTE_IN_MILLIS) + " " + Html.fromHtml("&#8226;") + " ");
		} catch (JSONException e) {
			Log.e("Error!", e.toString());
		}
	}

	public static void shareImage(Fragment fragment, JSONParcelable imageData) {
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		try {
			intent.putExtra(Intent.EXTRA_TEXT, imageData.getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_LINK));
		} catch (JSONException e) {
			Log.e("Error!", "bad link to share");
		}
		fragment.startActivity(intent);
	}

	public static void copyImageURL(Fragment fragment, JSONParcelable imageData) {
		final Activity activity = fragment.getActivity();
		final JSONParcelable data = imageData;
		final Fragment frag = fragment;
		String[] copyTypes = fragment.getResources().getStringArray(R.array.copyTypes);
		try {
			copyTypes[0] = copyTypes[0] + "\nhttp://imgur.com/" + imageData.getJSONObject().getString("id");
			copyTypes[1] = copyTypes[1] + "\n" + imageData.getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_LINK);
			copyTypes[2] = copyTypes[2] + "\n<a href=\"http://imgur.com/" + imageData.getJSONObject().getString("id") + "\"><img src=\"" + imageData.getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_LINK) + "\" title=\"Hosted by imgur.com\"/></a>";
			copyTypes[3] = copyTypes[3] + "\n[IMG]" + imageData.getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_LINK) + "[/IMG]";
			copyTypes[4] = copyTypes[4] + "\n[URL=http://imgur.com/" + imageData.getJSONObject().getString("id") + "][IMG]" + imageData.getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_LINK) + "[/IMG][/URL]";
			copyTypes[5] = copyTypes[5] + "\n[Imgur](http://i.imgur.com/" + imageData.getJSONObject().getString("id") + ")";
		} catch (JSONException e) {
			Log.e("Error!", e.toString());
		}
		new AlertDialog.Builder(activity).setTitle("Set Link Type to Copy")
				.setItems(copyTypes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						ClipboardManager clipboard = (ClipboardManager)
								activity.getSystemService(Context.CLIPBOARD_SERVICE);
						try {
							String link = "";
							switch (whichButton) {
								case 0:
									link = "http://imgur.com/" + data.getJSONObject().getString("id");
									break;
								case 1:
									link = data.getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_LINK);
									break;
								case 2:
									link = "<a href=\"http://imgur.com/" + data.getJSONObject().getString("id") + "\"><img src=\"" + data.getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_LINK) + "\" title=\"Hosted by imgur.com\"/></a>";
									break;
								case 3:
									link = "[IMG]" + data.getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_LINK) + "[/IMG]";
									break;
								case 4:
									link = "[URL=http://imgur.com/" + data.getJSONObject().getString("id") + "][IMG]" + data.getJSONObject().getString(ImgurHoloActivity.IMAGE_DATA_LINK) + "[/IMG][/URL]";
									break;
								case 5:
									link = "[Imgur](http://i.imgur.com/" + data.getJSONObject().getString("id") + ")";
									break;
								default:
									break;
							}
							int duration = Toast.LENGTH_SHORT;
							Toast toast;
							toast = Toast.makeText(frag.getActivity(), "URL Copied!", duration);
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
	}

	public static void downloadImage(Fragment fragment, JSONParcelable imageData) {
		Intent serviceIntent = new Intent(fragment.getActivity(), DownloadService.class);
		ArrayList<Parcelable> downloadIDs = new ArrayList<Parcelable>();
		downloadIDs.add(imageData);
		serviceIntent.putParcelableArrayListExtra("ids", downloadIDs);
		fragment.getActivity().startService(serviceIntent);
	}
}
