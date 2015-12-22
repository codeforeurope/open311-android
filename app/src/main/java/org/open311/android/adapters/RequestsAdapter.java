package org.open311.android.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.open311.android.R;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> implements Filterable {

    private final List<ServiceRequest> requests;
    private final ArrayList<ServiceRequest> filteredRequests;

    public RequestsAdapter(List<ServiceRequest> requests) {
        this.requests = requests;
        filteredRequests = new ArrayList<>(requests);
    }

    @Override
    public RequestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.request_cardview, parent, false);
        return new RequestViewHolder(v);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onBindViewHolder(RequestViewHolder holder, int position) {
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
            new DownloadImageTask(holder.requestImage)
                    .execute(url.toString());
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

        holder.requestUpdated.setText(DateUtils.getRelativeTimeSpanString(updated.getTime(), (new Date()).getTime(), DateUtils.SECOND_IN_MILLIS));
        holder.requestRequested.setText(DateUtils.getRelativeTimeSpanString(requested.getTime(), (new Date()).getTime(), DateUtils.SECOND_IN_MILLIS));
        if (updated.equals(requested)) {
            holder.requestUpdated.setVisibility(View.GONE);
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
        return filteredRequests.size();
    }

    @Override
    public Filter getFilter() {
        return new RequestsFilter(this, requests);
    }

    class RequestsFilter extends Filter {

        private final RequestsAdapter adapter;
        private final List<ServiceRequest> originalList;
        private final List<ServiceRequest> filteredList;

        private RequestsFilter(RequestsAdapter adapter, List<ServiceRequest> originalList) {
            super();
            this.adapter = adapter;
            this.originalList = new LinkedList<>(originalList);
            this.filteredList = new ArrayList<>();
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

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            adapter.filteredRequests.clear();
            adapter.filteredRequests.addAll((ArrayList<ServiceRequest>) results.values);
            adapter.notifyDataSetChanged();
        }
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        CardView requestCardView;
        TextView requestName;
        TextView requestDescription;
        TextView requestRequested;
        TextView requestUpdated;
        ImageView requestImage;

        RequestViewHolder(View itemView) {
            super(itemView);
            requestCardView = (CardView) itemView.findViewById(R.id.service_list);
            requestName = (TextView) itemView.findViewById(R.id.request_title);
            requestDescription = (TextView) itemView.findViewById(R.id.request_description);
            requestRequested = (TextView) itemView.findViewById(R.id.request_requested);
            requestUpdated = (TextView) itemView.findViewById(R.id.request_updated);
            requestImage = (ImageView) itemView.findViewById(R.id.request_image);
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}
