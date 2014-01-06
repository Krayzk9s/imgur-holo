package com.krayzk9s.imgurholo.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Kurt Zimmer on 1/5/14.
 */
public class SubredditDialogFragment extends android.support.v4.app.DialogFragment {
    DragSortListView subredditList;
    DragSortController mController;
    HashSet<String> subredditArray;
    SubredditArrayAdapter subredditAdapter;
    SharedPreferences settings;

    public SubredditDialogFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.subreddit_dialog, container);
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        settings = activity.getApiCall().settings;
        final EditText editText = (EditText) view.findViewById(R.id.subreddit_dialog_text);
        subredditList = (DragSortListView) view.findViewById(R.id.subreddit_dialog_list);
        Button subredditCancelButton = (Button) view.findViewById(R.id.subreddit_cancel);
        Button subredditAddButton = (Button) view.findViewById(R.id.subreddit_add);
        Button subredditOKButton = (Button) view.findViewById(R.id.subreddit_ok);
        subredditCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        subredditAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subredditAdapter.add(editText.getText().toString());
                editText.setText("");
                saveSubreddits();
            }
        });
        subredditOKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String subreddit = editText.getText().toString();
                Intent intent = new Intent();
                intent.putExtra("subreddit", subreddit);
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                dismiss();
            }
        });
        mController = buildController(subredditList);
        subredditList.setFloatViewManager(mController);
        subredditList.setOnTouchListener(mController);
        subredditList.setDragEnabled(true);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String subreddit = v.getText().toString();
                    Intent intent = new Intent();
                    intent.putExtra("subreddit", subreddit);
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        subredditAdapter = new SubredditArrayAdapter(getActivity());
        subredditList.setAdapter(subredditAdapter);
        subredditList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.putExtra("subreddit", subredditAdapter.getItem(position));
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                dismiss();
            }
        });
        getDialog().setTitle(R.string.dialog_reddit_choose_title);
        DragSortListView.DropListener onDrop =
                new DragSortListView.DropListener() {
                    @Override
                    public void drop(int from, int to) {
                        if (from != to) {
                            String item = subredditAdapter.getItem(from);
                            subredditAdapter.remove(item);
                            subredditAdapter.insert(item, to);
                            saveSubreddits();
                        }
                    }
                };
        DragSortListView.RemoveListener onRemove =
                new DragSortListView.RemoveListener() {
                    @Override
                    public void remove(int which) {
                        subredditAdapter.remove(subredditAdapter.getItem(which));
                        saveSubreddits();
                    }
                };
        subredditList.setDropListener(onDrop);
        subredditList.setRemoveListener(onRemove);
        return view;
    }

    public void saveSubreddits() {
        ArrayList<String> subreddits = new ArrayList<String>();
        for(int i = 0; i < subredditAdapter.getCount(); i++) {
            subreddits.add(subredditAdapter.getItem(i));
        }
        Log.d("SubredditsSet", subreddits.toString());
        settings.edit().putString("Subreddits", TextUtils.join(",", subreddits)).commit();
        Log.d("Subreddits", settings.getString("Subreddits", "lol,loller"));
    }

    public DragSortController buildController(DragSortListView dslv) {
        DragSortController controller = new DragSortController(dslv);
        controller.setDragHandleId(R.id.drag_handle);
        controller.setRemoveEnabled(true);
        controller.setSortEnabled(true);
        controller.setDragInitMode(DragSortController.ON_DOWN);
        controller.setRemoveMode(DragSortController.FLING_RIGHT_REMOVE);
        return controller;
    }


    public class SubredditArrayAdapter extends ArrayAdapter<String> {

        public SubredditArrayAdapter(Context context) {
            super(context, R.layout.drag_item);
            this.addAll(settings.getString("Subreddits", "aww,carporn,earthporn,foodporn,historyporn,mapporn,oldschoolcool,spaceporn,wallpapers").split(","));
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = View.inflate(this.getContext(), R.layout.drag_item, null);
            TextView menuItem = (TextView) convertView.findViewById(R.id.drag_text);
            menuItem.setText(this.getItem(position));
            return convertView;
        }
    }
}
