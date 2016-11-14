package org.open311.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.codeforamerica.open311.facade.data.Service;
import org.open311.android.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by miblon on 11/14/16.
 */

public class ServicesAdapter extends ArrayAdapter<Service> {
    private Activity activity;
    private List<Service> services;

    private LayoutInflater inflater;

    public ServicesAdapter(Context context, int resource) {
        super(context, resource);
    }

    public ServicesAdapter(Activity activity, List<Service> services) {
        super(activity, R.layout.item_service, services);
        this.activity = activity;
        if (services == null) {
            services = new ArrayList<Service>();
        } else {
            this.services = services;
        }
    }
    @Override
    public int getCount() {
        return services.size();
    }

    @Override
    public Service getItem(int location) {
        return services.get(location);
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
            convertView = inflater.inflate(R.layout.item_service, null);

        TextView name = (TextView) convertView.findViewById(R.id.item_service_name);
        TextView description = (TextView) convertView.findViewById(R.id.item_service_description);

        // getting data for the row
        Service m = services.get(position);
        // name
        name.setText(m.getServiceName());
        // description
        description.setText(m.getDescription());

        return convertView;
    }

    @Override
    public void add(Service service) {
        super.add(service);
        services.add(service);
        notifyDataSetChanged();
    }

    @Override
    public void remove(Service service) {
        super.remove(service);
        services.remove(service);
        notifyDataSetChanged();
    }
}
