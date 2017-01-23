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
 * Picklist to select a current Server
 * Created by miblon on 11/14/16.
 */

public class CitiesAdapter extends ArrayAdapter<Server> {
    private Activity activity;
    private List<Server> servers;

    private LayoutInflater inflater;

    public CitiesAdapter(Context context, int resource) {
        super(context, resource);
    }

    public CitiesAdapter(Activity activity, List<Server> servers) {
        super(activity, R.layout.item_server, servers);
        this.activity = activity;
        this.servers = servers;
    }

    @Override
    public int getCount() {
        return servers.size();
    }

    @Override
    public Server getItem(int location) {
        return servers.get(location);
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
            convertView = inflater.inflate(R.layout.item_server, null);

        TextView name = (TextView) convertView.findViewById(R.id.item_server_name);
        Server m = servers.get(position);
        name.setText(m.getName());
        return convertView;
    }

    @Override
    public void add(Server service) {
        super.add(service);
        servers.add(service);
        notifyDataSetChanged();
    }

    @Override
    public void remove(Server service) {
        super.remove(service);
        servers.remove(service);
        notifyDataSetChanged();
    }
}
