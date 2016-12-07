package org.open311.android.models;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.Serializable;

/**
 * Created by miblon on 11/23/16.
 */

public class Attachment implements Serializable {

    public Uri getUri() {
        return uri;
    }

    public Attachment setUri(Uri uri) {
        this.uri = uri;
        return this;
    }

    private Uri uri;


}
