package org.open311.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.codeforamerica.open311.facade.data.Server;
import org.open311.android.R;

import java.util.List;

/**
 * Picklist to select a current City
 * Created by miblon on 11/14/16.
 */

public class CitiesAdapter extends ArrayAdapter<Server> {
    private Activity activity;
    private List<Server> cities;

    private LayoutInflater inflater;

    public CitiesAdapter(Context context, int resource) {
        super(context, resource);
    }

    public CitiesAdapter(Activity activity, List<Server> cities) {
        super(activity, R.layout.item_city, cities);
        this.activity = activity;
        this.cities = cities;
    }

    @Override
    public int getCount() {
        return cities.size();
    }

    @Override
    public Server getItem(int location) {
        return cities.get(location);
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
            convertView = inflater.inflate(R.layout.item_city, null);

        TextView name = (TextView) convertView.findViewById(R.id.item_city_name);
        Server m = cities.get(position);
        name.setText(m.getName());
        return convertView;
    }

    @Override
    public void add(Server service) {
        super.add(service);
        cities.add(service);
        notifyDataSetChanged();
    }

    @Override
    public void remove(Server service) {
        super.remove(service);
        cities.remove(service);
        notifyDataSetChanged();
    }
}
