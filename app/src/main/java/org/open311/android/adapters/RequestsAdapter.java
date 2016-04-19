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

import com.squareup.picasso.Picasso;

import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.open311.android.R;
import org.open311.android.fragments.RequestsFragment;
import org.open311.android.filters.RequestsFilter;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> implements Filterable {

    private List<ServiceRequest> requests;
    private ArrayList<ServiceRequest> filteredRequests;
    private final RequestsFragment.OnListFragmentInteractionListener mListener;
    private Context context;

    public ArrayList<ServiceRequest> getFilteredRequests() {
        return filteredRequests;
    }

    public List<ServiceRequest> getRequests() {
        return requests;
    }

    public void appendRequests(List<ServiceRequest> requests) {
        this.requests.addAll(requests);
        if (this.requests != null) {
            setFilteredRequests(this.requests);
        }
    }

    public void setRequests(List<ServiceRequest> requests) {
        this.requests = requests;

        if (requests != null) {
            setFilteredRequests(requests);
        }
    }

    public void setFilteredRequests(List<ServiceRequest> requests) {
        ArrayList<ServiceRequest> filteredRequests = new ArrayList<ServiceRequest>(requests);
        this.filteredRequests.clear();
        // TODO sort?
        for (final ServiceRequest sr : filteredRequests) {
            this.filteredRequests.add(sr);
        }
        // The latest version of the API returns a descending list, ordered by input date
        // Collections.reverse(this.filteredRequests);
        // TODO filter?
        this.notifyDataSetChanged();
    }

    public RequestsAdapter(List<ServiceRequest> requests, RequestsFragment.OnListFragmentInteractionListener mListener) {
        this.requests = requests;
        this.mListener = mListener;
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
            Picasso.with(context).load(url.toString()).fit().centerCrop().into(holder.requestImage);
            holder.requestImage.setVisibility(View.VISIBLE);
        } else {
            holder.requestImage.setImageDrawable(null);
            holder.requestImage.setVisibility(View.GONE);
        }
        //Service
        String code = filteredRequests.get(position).getServiceCode();
        String name = filteredRequests.get(position).getServiceName();
        String notice = filteredRequests.get(position).getServiceNotice();
        String agency = filteredRequests.get(position).getAgencyResponsible();


        holder.requestName.setText(name);
        holder.requestAddress.setText(address);
        if (status != null) {
            holder.requestStatus.setText(status.toString());
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final CardView requestCardView;
        public final TextView requestName;
        public final TextView requestDescription;
        //public final TextView requestRequested;
        public final TextView requestUpdated;
        public final TextView requestStatus;
        public final TextView requestAddress;
        public final ImageView requestImage;
        public ServiceRequest mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            requestCardView = (CardView) view.findViewById(R.id.requests_list);
            requestName = (TextView) view.findViewById(R.id.request_title);
            requestAddress = (TextView) view.findViewById(R.id.request_address);
            requestDescription = (TextView) view.findViewById(R.id.request_description);
            //requestRequested = (TextView) view.findViewById(R.id.request_requested);
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
