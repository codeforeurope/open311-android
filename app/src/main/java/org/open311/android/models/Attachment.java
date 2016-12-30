package org.open311.android.models;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.Serializable;

/**
 * Class for attachments that can be part of a open311 report
 * Created by milo@dogodigi.net on 11/23/16.
 */

public class Attachment implements Serializable {

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
        public int toInt(){
            return intValue;
        }
    }
    private Uri uri;
    private int status;
    private AttachmentType type;


    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Uri getUri() {
        return uri;
    }

    public Attachment setUri(Uri uri) {
        this.uri = uri;
        return this;
    }

    public Attachment(AttachmentType type){
        this.type = type;
    }
}
