package org.open311.android.adapters;

/**
 * Created by miblon on 7/14/16.
 */


import android.content.Context;
import android.location.Address;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.open311.android.R;
import org.open311.android.helpers.Utils;
import org.open311.android.helpers.GeocoderNominatim;
import org.osmdroid.tileprovider.util.ManifestUtil;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GeocoderAdapter extends BaseAdapter implements Filterable {

    private final Context context;

    private GeocoderFilter geocoderFilter;

    private List<Address> features;

    public GeocoderAdapter(Context context) {
        this.context = context;
    }

    /*
     * Required by BaseAdapter
     */

    @Override
    public int getCount() {
        return features.size();
    }

    @Override
    public Address getItem(int position) {
        return features.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /*
     * Get a View that displays the data at the specified position in the data set.
     */

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get view
        View view;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        } else {
            view = convertView;
        }

        // It always is a textview
        TextView text = (TextView) view;

        // Set the address
        Address feature = getItem(position);
        text.setText(Utils.addressString(feature));

        return view;
    }

    /*
     * Required by Filterable
     */

    @Override
    public Filter getFilter() {
        if (geocoderFilter == null) {
            geocoderFilter = new GeocoderFilter();
        }

        return geocoderFilter;
    }

    private class GeocoderFilter extends Filter {
        final String userAgent = "open311/1.0 (regular; info@open311.io)";

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            // No constraint
            if (TextUtils.isEmpty(constraint)) {
                return results;
            }

            // nominatim geocoder

            GeocoderNominatim geocoder = new GeocoderNominatim(context, Locale.getDefault(), userAgent);
            geocoder.setKey(ManifestUtil.retrieveKey(context, "MAPQUEST_API_KEY"));
            geocoder.setService(GeocoderNominatim.MAPQUEST_SERVICE_URL);
            try {
                List<Address> features = geocoder.getFromLocationName(constraint.toString(), 10);
                results.values = features;
                results.count = features.size();
                return results;
            } catch (IOException e) {
                e.printStackTrace();
                return results;
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results != null && results.count > 0) {
                features = (List<Address>) results.values;
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
