package org.open311.android.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.open311.android.models.Attachment;

import java.util.LinkedList;
import java.util.List;

/**
 * Class to hold a list of Attachments and prepare them for upload.
 * Created by milo@dogodigi.net on 12/29/16.
 */

public class AttachmentAdapter extends BaseAdapter {
    private static final int ITEM_TYPE_ONLINE = 0;
    private static final int ITEM_TYPE_DISK = 1;

    private LinkedList<Attachment> attachments;
    private Context context;

    public AttachmentAdapter(Context context) {
        super();
        this.context = context;
        this.attachments = new LinkedList<Attachment>();
    }
    public AttachmentAdapter(LinkedList<Attachment> attachments){
        super();
        this.attachments = attachments;
    }
    public LinkedList<Attachment> getList(){
        return this.attachments;
    }
    @Override
    public int getCount() {
        return attachments.size();
    }

    @Override
    public Object getItem(int i) {
        return attachments.get(i);
    }

    @Override
    public long getItemId(int i) {
        return attachments.get(i).hashCode();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        return null;
    }

    public void delete(Attachment attachment) {
        this.attachments.remove(attachment);
    }

    public void add(Attachment attachment) {
        //TODO check to see if the attachment exists, so it will only be handled once
        if (!this.attachments.contains(attachment)) {
            this.attachments.add(attachment);
        }

    }
}
