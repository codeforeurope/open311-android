package org.open311.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.codeforamerica.open311.facade.data.Service;
import org.open311.android.R;

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
        this.services = services;
    }

    @Override
    public int getCount() {
        if (services != null) {
            return services.size();
        } else {
            return 0;
        }
    }

    public void setServices(List<Service> services) {
        this.services = services;

        if (services != null) {
            this.notifyDataSetChanged();
        }
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
        if (m.getDescription() != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                description.setText(Html.fromHtml(m.getDescription(), Html.FROM_HTML_MODE_COMPACT));
            } else {
                description.setText(Html.fromHtml(m.getDescription()));
            }
        } else {
            description.setVisibility(View.INVISIBLE);
        }
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
