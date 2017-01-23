package org.open311.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.codeforamerica.open311.facade.data.Service;
import org.open311.android.R;

import java.util.List;

/**
 * Retrieve services list for display in dialog
 * Created by miblon on 11/14/16.
 */

public class ServicesAdapter extends ArrayAdapter<Service> {
    private Activity activity;
    private LayoutInflater mInflater;
    private List<Service> services;

    static class ViewHolder {
        private TextView name;
        private TextView description;
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

    @Override
    public Service getItem(int location) {
        return services.get(location);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {

            holder = new ViewHolder();
            mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.item_service, null);

            holder.name = (TextView) convertView.findViewById(R.id.item_service_name);
            holder.description = (TextView) convertView.findViewById(R.id.item_service_description);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        // getting data for the row
        Service m = getItem(position);
        // name
        if (m != null) {
            holder.name.setText(m.getServiceName());
            // description
            if (m.getDescription() != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    holder.description.setText(Html.fromHtml(m.getDescription(), Html.FROM_HTML_MODE_LEGACY));
                } else {
                    holder.description.setText(Html.fromHtml(m.getDescription()));
                }
            }
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
