package org.open311.android.models;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import org.open311.android.R;

/**
 * Class for attachments that can be part of a open311 report
 * Created by milo@dogodigi.net on 11/23/16.
 */

public class Attachment implements Parcelable {

    protected Attachment(Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Creator<Attachment> CREATOR = new Creator<Attachment>() {
        @Override
        public Attachment createFromParcel(Parcel in) {
            return new Attachment(in);
        }

        @Override
        public Attachment[] newArray(int size) {
            return new Attachment[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(uri, i);
    }

    public enum AttachmentType {
        IMAGE("Image", 0),
        AUDIO("Audio", 1);

        private String stringValue;
        private int intValue;

        AttachmentType(String toString, int value) {
            stringValue = toString;
            intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        public int toInt() {
            return intValue;
        }
    }

    private Uri uri;
    private AttachmentType type;


    public Attachment(AttachmentType type, Uri uri) {
        super();
        this.type = type;
        this.uri = uri;
    }

    public Uri getUri() {
        return uri;
    }

    public AttachmentType getType() {
        return type;
    }

    public String getDescription() {
        return uri.toString();
    }

    public int getIcon() {
        switch (type) {
            case AUDIO:
                return R.drawable.ic_play_arrow;
            case IMAGE:
            default:
                return R.drawable.ic_image;
        }
    }
}
