package com.krayzk9s.imgurholo.tools;

import android.app.Activity;
import android.util.DisplayMetrics;

/**
 * Created by Kurt Zimmer on 11/20/13.
 */
public class Utils {
    public static int dpToPx(int dp, Activity activity) {
        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
