package org.open311.android.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.codeforamerica.open311.facade.APIWrapper;
import org.codeforamerica.open311.facade.APIWrapperFactory;
import org.codeforamerica.open311.facade.City;
import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.codeforamerica.open311.facade.data.operations.GETServiceRequestsFilter;
import org.codeforamerica.open311.facade.exceptions.APIWrapperException;
import org.codeforamerica.open311.internals.caching.NoCache;
import org.open311.android.R;
import org.open311.android.adapters.RequestsAdapter;
import org.open311.android.helpers.MyReportsFile;
import org.open311.android.helpers.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RequestsFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    private RequestsAdapter recyclerViewAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RequestsFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static RequestsFragment newInstance(int columnCount) {
        RequestsFragment fragment = new RequestsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        SharedPreferences settings = Utils.getSettings(getActivity());
        boolean hasReports = settings.getBoolean("has_reports", false);
        if (! hasReports) {
            return inflater.inflate(R.layout.fragment_intro, container, false);
        }

        View view = inflater.inflate(R.layout.fragment_requests_list, container, false);
        final RecyclerView listView = (RecyclerView) view.findViewById(R.id.requests_list);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        if (listView instanceof RecyclerView) {
            Context context = view.getContext();
            if (mColumnCount <= 1) {
                listView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                listView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            List<ServiceRequest> items = new ArrayList<ServiceRequest>();
            recyclerViewAdapter = new RequestsAdapter(items, mListener);
            listView.setAdapter(recyclerViewAdapter);
            updateServiceRequests();
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateServiceRequests();
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(ServiceRequest item);
    }

    private void updateServiceRequests() {
        RetrieveServiceRequestsTask bgTask = new RetrieveServiceRequestsTask();
        bgTask.setEndpointUrl("http://eindhoven.meldloket.nl/crm/open311/v2");
        bgTask.execute();
    }

    private class RetrieveServiceRequestsTask extends AsyncTask<Void, Void, Bundle> {

        private City city = null;
        private String endpointUrl = null;
        private String jurisdictionId = null;

        public void setCity(City city) {
            this.city = city;
        }

        public void setEndpointUrl(String endpointUrl) {
            this.endpointUrl = endpointUrl;
        }

        public void setJurisdictionId(String jurisdictionId) {
            this.jurisdictionId = jurisdictionId;
        }

        /**
         * Retrieve service requests in the background.
         *
         * @param ignore the parameters of the task.
         */
        @Override
        protected Bundle doInBackground(Void... ignore) {

            Bundle bundle = null;
            APIWrapperFactory factory;
            try {
                if (city != null) {
                    factory = new APIWrapperFactory(city).setCache(new NoCache()).withLogs();
                } else {
                    factory = new APIWrapperFactory(endpointUrl, jurisdictionId)
                            .setCache(new NoCache()).withLogs();
                }

                APIWrapper wrapper;
                wrapper = factory.build();
                GETServiceRequestsFilter filter = new GETServiceRequestsFilter();

                MyReportsFile file = new MyReportsFile(getContext());
                String id = file.getServiceRequestIds();
                if (id == null || id.isEmpty()) {
                    id = "9999"; // FIXME: 12-04-16 temporary fix; id cannot be empty
                }
                filter.setServiceRequestId(id);

                List<ServiceRequest> result = null;
                if (wrapper != null) {
                    result = wrapper.getServiceRequests(filter);
                }
                ArrayList<ServiceRequest> requests = new ArrayList<ServiceRequest>();
                if (result != null) {
                    for (ServiceRequest request : result) {
                        requests.add(request);

                    }
                }
                bundle = new Bundle();
                bundle.putParcelableArrayList("Requests", requests);
            } catch (APIWrapperException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bundle;
        }

        protected void onPostExecute(Bundle data) {
            ArrayList<ServiceRequest> result = data.getParcelableArrayList("Requests");
            if (result != null) {
                Log.d("open311", result.toString());
                // Update the adapter with the result.
                recyclerViewAdapter.setRequests(result);
            } else {
                Log.w("open311", "No data received!");
            }
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
