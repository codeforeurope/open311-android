package org.open311.android.models;

/**
 * Created by miblon on 1/23/17.
 */

public class UploadResult {
    public String getMimetype() {
        return mimetype;
    }

    public UploadResult setMimetype(String mimetype) {
        this.mimetype = mimetype;
        return this;
    }

    public String getPath() {
        return path;
    }

    public UploadResult setPath(String path) {
        this.path = path;
        return this;
    }

    public String getName() {
        return name;
    }

    public UploadResult setName(String name) {
        this.name = name;
        return this;
    }

    private String mimetype;
    private String path;
    private String name;
}
