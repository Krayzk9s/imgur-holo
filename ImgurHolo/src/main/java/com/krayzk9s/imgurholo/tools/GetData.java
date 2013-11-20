package com.krayzk9s.imgurholo.tools;

/**
 * Created by Kurt Zimmer on 11/16/13.
 */
public interface GetData {
    void onGetObject(Object data, String tag);
    void handleException(Exception e, String tag);
}
