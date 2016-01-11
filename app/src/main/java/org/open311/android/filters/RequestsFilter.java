package org.open311.android.filters;


import android.widget.Filter;

import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.open311.android.adapters.RequestsAdapter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RequestsFilter extends Filter {

    private final RequestsAdapter adapter;
    private final List<ServiceRequest> originalList;
    private final List<ServiceRequest> filteredList;

    public RequestsFilter(RequestsAdapter adapter, List<ServiceRequest> originalList) {
        super();
        this.adapter = adapter;
        this.originalList = new LinkedList<ServiceRequest>(originalList);
        this.filteredList = new ArrayList<ServiceRequest>();
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        filteredList.clear();
        final FilterResults results = new FilterResults();

        if (constraint.length() == 0) {
            filteredList.addAll(originalList);
        } else {
            for (final ServiceRequest sr : originalList) {
                final String text = sr.getServiceName().toLowerCase() + " " + sr.getDescription().toLowerCase();
                if (text.contains(constraint)) {
                    filteredList.add(sr);
                }
            }
        }
        results.values = filteredList;
        results.count = filteredList.size();
        return results;
    }

    // TODO Sort and stuff
    //Collections.sort(requests, new Comparator<ServiceRequest>() {
    //    public int compare(ServiceRequest emp1, ServiceRequest emp2) {
    //        return emp1.getServiceName().compareToIgnoreCase(emp2.getServiceName());
    //    }
    //});

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        adapter.setRequests((ArrayList<ServiceRequest>) results.values);
        adapter.notifyDataSetChanged();
    }
}

