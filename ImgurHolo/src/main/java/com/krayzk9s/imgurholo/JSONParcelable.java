package com.krayzk9s.imgurholo;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONObject;

/**
 * Created by Kurt Zimmer on 7/27/13.
 */
public class JSONParcelable implements Parcelable {

    private JSONObject jsonData;

    /**
     * Standard basic constructor for non-parcel
     * object creation
     */
    public JSONParcelable() {
    }

    ;

    /**
     * Constructor to use when re-constructing object
     * from a parcel
     *
     * @param in a parcel from which to read this object
     */
    public JSONParcelable(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        // We just need to write each field into the
        // parcel. When we read from parcel, they
        // will come back in the same order
        dest.writeString(jsonData.toString());
    }

    public void setJSONObject(JSONObject object) {
        jsonData = object;
    }

    public JSONObject getJSONObject() {
        return jsonData;
    }

    /**
     * Called from the constructor to create this
     * object from a parcel.
     *
     * @param in parcel from which to re-create object
     */
    private void readFromParcel(Parcel in) {

        // We just need to read back each
        // field in the order that it was
        // written to the parcel
        try {
            jsonData = new JSONObject(in.readString());
        } catch (Exception e) {
            Log.d("Error!", e.toString());
        }
    }

    /**
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays.
     * <p/>
     * This also means that you can use use the default
     * constructor to create the object and use another
     * method to hyrdate it as necessary.
     * <p/>
     * I just find it easier to use the constructor.
     * It makes sense for the way my brain thinks ;-)
     */
    public static final Parcelable.Creator CREATOR =
            new Parcelable.Creator() {
                public JSONParcelable createFromParcel(Parcel in) {
                    return new JSONParcelable(in);
                }

                public JSONParcelable[] newArray(int size) {
                    return new JSONParcelable[size];
                }
            };

}