package org.open311.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.codeforamerica.open311.facade.data.Service;
import org.open311.android.R;

import java.util.ArrayList;

public class ServiceListAdapter extends ArrayAdapter<Service> {
    public ServiceListAdapter(Context context, ArrayList<Service> ServiceList) {
        super(context, 0, ServiceList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Service service = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.service_row, parent, false);
        }
        TextView title = (TextView) convertView.findViewById(R.id.service_title);
        TextView description = (TextView) convertView.findViewById(R.id.service_description);
        title.setText(service.getServiceName());
        String ed_text = service.getDescription();

        if (ed_text != null && ed_text.length() > 0) {
            description.setText(ed_text);
            description.setVisibility(View.VISIBLE);

        } else {
            description.setVisibility(View.GONE);
        }
        return convertView;
    }
}