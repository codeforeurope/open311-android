package org.open311.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.open311.android.R;
import org.open311.android.models.Attachment;

import java.util.LinkedList;

/**
 * Adapter to process attachments into UI components.
 * Created by miblon on 1/3/17.
 */

public class AttachmentAdapter extends ArrayAdapter<Attachment> {
    private Activity activity;
    private LinkedList<Attachment> attachments;

    private LayoutInflater inflater;

    public AttachmentAdapter(Context context, int resource) {
        super(context, resource);
    }

    public AttachmentAdapter(Activity activity, LinkedList<Attachment> attachments) {
        super(activity, R.layout.item_attachment, attachments);
        this.activity = activity;
        this.attachments = attachments;
    }
    @Override
    public int getCount() {
        return attachments.size();
    }

    @Override
    public Attachment getItem(int location) {
        return attachments.get(location);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (inflater == null)
            inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null)
            convertView = inflater.inflate(R.layout.item_attachment, null);

        ImageView icon = (ImageView) convertView.findViewById(R.id.item_attachment_image);
        TextView description = (TextView) convertView.findViewById(R.id.item_attachment_text);

        Attachment m = attachments.get(position);
        icon.setImageResource(m.getIcon());
        description.setText(niceName(m.getUri()));

        return convertView;
    }

    @Override
    public void add(Attachment attachment) {
        super.add(attachment);
        attachments.add(attachment);
        notifyDataSetChanged();
    }

    @Override
    public void remove(Attachment attachment) {
        super.remove(attachment);
        attachments.remove(attachment);
        notifyDataSetChanged();
    }

    private String niceName(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            return uri.getLastPathSegment();
        } else if (scheme.equals("content")) {
            Cursor returnCursor = getContext().getContentResolver().query(uri, null, null, null, null);
            int nameIndex = 0;
            if (returnCursor != null) {
                nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                return returnCursor.getString(nameIndex);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
