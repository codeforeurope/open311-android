package org.open311.android.adapters;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.open311.android.R;
import org.open311.android.fragments.RequestsFragment;
import org.open311.android.filters.RequestsFilter;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> implements Filterable {

    private List<ServiceRequest> requests;
    private ArrayList<ServiceRequest> filteredRequests;
    private Context context;

    public void setRequests(List<ServiceRequest> requests) {
        this.requests = requests;

        if (requests != null) {
            setFilteredRequests(requests);
        }
    }

    private void setFilteredRequests(List<ServiceRequest> requests) {
        ArrayList<ServiceRequest> filteredRequests = new ArrayList<ServiceRequest>(requests);
        this.filteredRequests.clear();
        for (final ServiceRequest sr : filteredRequests) {
            this.filteredRequests.add(sr);
        }
        Collections.sort(this.filteredRequests, new Comparator<ServiceRequest>() {
            @Override
            public int compare(ServiceRequest serviceRequest, ServiceRequest t1) {
                return serviceRequest.getUpdatedDatetime().compareTo(t1.getUpdatedDatetime());
            }
        });
        this.notifyDataSetChanged();
    }

    public RequestsAdapter(List<ServiceRequest> requests, RequestsFragment.OnListFragmentInteractionListener mListener) {
        this.requests = requests;
        if (requests != null) {
            filteredRequests = new ArrayList<ServiceRequest>(requests);
        } else {
            filteredRequests = null;
        }
        context = null;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View v = LayoutInflater.from(context).inflate(R.layout.cardview_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        String id = filteredRequests.get(position).getServiceRequestId();
        Date requested = filteredRequests.get(position).getRequestedDatetime();
        Date updated = filteredRequests.get(position).getUpdatedDatetime();
        Date expected = filteredRequests.get(position).getExpectedDatetime();
        String description = filteredRequests.get(position).getDescription();
        ServiceRequest.Status status = filteredRequests.get(position).getStatus();
        String notes = filteredRequests.get(position).getStatusNotes();

        //Location
        Long addressId = filteredRequests.get(position).getAddressId();
        String address = filteredRequests.get(position).getAddress();
        Float lat = filteredRequests.get(position).getLatitude();
        Float lon = filteredRequests.get(position).getLongitude();
        Integer postcode = filteredRequests.get(position).getZipCode();

        //Media
        URL url = filteredRequests.get(position).getMediaUrl();
        if (url != null) {
            Glide.with(context).load(url.toString()).fitCenter().centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.requestImage);
            holder.requestImage.setVisibility(View.VISIBLE);
        } else {
            holder.requestImage.setImageDrawable(null);
            holder.requestImage.setVisibility(View.GONE);
        }

        String name = filteredRequests.get(position).getServiceName();
        holder.requestName.setText(name);
        holder.requestAddress.setText(address);
        holder.requestId.setText(id);

        if (status != null) {
            holder.requestStatus.setText(context.getText(getResId(status)));
            holder.requestDescription.setVisibility(View.VISIBLE);
        } else {
            holder.requestStatus.setVisibility(View.GONE);
        }
        if (updated != null) {
            holder.requestUpdated.setText(getElapsedTime(updated));
        }
        if (description != null && description.length() > 0) {
            holder.requestDescription.setText(description);
            holder.requestDescription.setVisibility(View.VISIBLE);

        } else {
            holder.requestDescription.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        if (filteredRequests != null) {
            return filteredRequests.size();
        } else {
            return this.requests.size();
        }
    }

    @Override
    public Filter getFilter() {
        return new RequestsFilter(this, requests);
    }

    private CharSequence getElapsedTime(Date updated) {
        return DateUtils.getRelativeTimeSpanString(
                updated.getTime(),
                (new Date()).getTime(),
                DateUtils.SECOND_IN_MILLIS);
    }

    private int getResId(ServiceRequest.Status status) {
        int resId;
        switch (status) {
            case OPEN:
                resId = R.string.status_open;
                break;
            case CLOSED:
                resId = R.string.status_closed;
                break;
            default:
                resId = R.string.status_unknown;
        }
        return resId;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final CardView requestCardView;
        final TextView requestId;
        final TextView requestName;
        final TextView requestDescription;
        //public final TextView requestRequested;
        final TextView requestUpdated;
        final TextView requestStatus;
        final TextView requestAddress;
        final ImageView requestImage;

        ViewHolder(View view) {
            super(view);
            mView = view;
            requestCardView = (CardView) view.findViewById(R.id.requests_list);
            requestId = (TextView) view.findViewById(R.id.request_id);
            requestName = (TextView) view.findViewById(R.id.request_title);
            requestAddress = (TextView) view.findViewById(R.id.request_address);
            requestDescription = (TextView) view.findViewById(R.id.request_description);
            requestUpdated = (TextView) view.findViewById(R.id.request_updated);
            requestImage = (ImageView) view.findViewById(R.id.request_image);
            requestStatus = (TextView) view.findViewById(R.id.request_status);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + requestDescription.getText() + "'";
        }
    }
}
