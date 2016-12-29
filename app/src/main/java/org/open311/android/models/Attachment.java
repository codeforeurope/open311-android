package org.open311.android.models;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.Serializable;

/**
 * Class for attachments that can be part of a open311 report
 * Created by milo@dogodigi.net on 11/23/16.
 */

public class Attachment implements Serializable {

    private Uri uri;
    private int status;

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
}
