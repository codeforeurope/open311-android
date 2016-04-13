package org.open311.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.open311.android.MainActivity;
import org.open311.android.R;
import org.open311.android.adapters.RequestsAdapter;

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
        View view = inflater.inflate(R.layout.fragment_requests_list, container, false);
        RecyclerView listView = (RecyclerView) view.findViewById(R.id.requests_list);
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
            MainActivity myActivity = (MainActivity) getActivity();
            myActivity.setupGetRequestsServiceReceiver(recyclerViewAdapter);
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                stopRefreshAnimation();
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

    private void stopRefreshAnimation() {
        swipeRefreshLayout.setRefreshing(false);
    }
}
