package org.open311.android.fragments;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewSwitcher;

import org.codeforamerica.open311.facade.APIWrapper;
import org.codeforamerica.open311.facade.APIWrapperFactory;
import org.codeforamerica.open311.facade.EndpointType;
import org.codeforamerica.open311.facade.data.Server;
import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.codeforamerica.open311.facade.data.operations.GETServiceRequestsFilter;
import org.codeforamerica.open311.facade.exceptions.APIWrapperException;
import org.open311.android.MainActivity;
import org.open311.android.R;
import org.open311.android.adapters.RequestsAdapter;
import org.open311.android.helpers.MyReportsFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.open311.android.helpers.Utils.getReports;

/**
 * Requests {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private static final String LOG_TAG = "RequestsFragment";

    private static final String ARG_COLUMN_COUNT = "column-count";

    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    private RequestsAdapter recyclerViewAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ViewSwitcher switcher; // this view can switch between the Intro and Requests list

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RequestsFragment() {
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
        Log.d(LOG_TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_requests_list, container, false);
        final RecyclerView listView = (RecyclerView) view.findViewById(R.id.requests_list);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        switcher = (ViewSwitcher) view.findViewById(R.id.view_switcher);
        if (listView != null) {
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
        bgTask.execute();
    }

    private class RetrieveServiceRequestsTask extends AsyncTask<Void, Void, Bundle> {

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
                MainActivity mActivity = (MainActivity) getActivity();
                Server mServer = mActivity.getCurrentServer();
                factory = new APIWrapperFactory(mServer, EndpointType.PRODUCTION);
                String[] mReports = getReports(mActivity, mServer.getName());
                if (mReports != null) {
                    Log.d(LOG_TAG, "RetrieveServiceRequestsTask doInBackground - Reports: " + TextUtils.join(", ", mReports));
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
                    PackageManager manager = getActivity().getPackageManager();
                    PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
                    wrapper.setHeader("User-Agent","open311-android/" + info.versionName);
                    wrapper.setHeader("open311-deviceid", ((MainActivity) getActivity()).getInstallationId());
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
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return bundle;
        }

        protected void onPostExecute(Bundle data) {
            if (data != null) {
                ArrayList<ServiceRequest> result = data.getParcelableArrayList("Requests");
                if (result != null) {
                    Log.d(LOG_TAG, "RetrieveServiceRequestsTask onPostExecute - Requests: " + result.toString());
                    // Update the adapter with the result.
                    recyclerViewAdapter.setRequests(result);
                    //
                    // Show the intro text when there are no results available,
                    // i.e. the user hasn't reported any issues yet.
                    //
                    if (result.size() == 0) {
                        switcher.setDisplayedChild(
                                switcher.indexOfChild(
                                        getActivity().findViewById(R.id.intro_container)));
                    }
                } else {
                    Log.w(LOG_TAG, "RetrieveServiceRequestsTask onPostExecute - No data received!");
                }
            } else {
                Log.w(LOG_TAG, "RetrieveServiceRequestsTask onPostExecute - No data received!");
            }
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
