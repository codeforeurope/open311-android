package org.open311.android.services;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;

import org.codeforamerica.open311.facade.APIWrapper;
import org.codeforamerica.open311.facade.APIWrapperFactory;
import org.codeforamerica.open311.facade.City;
import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.codeforamerica.open311.facade.data.operations.GETServiceRequestsFilter;
import org.codeforamerica.open311.facade.exceptions.APIWrapperException;
import org.codeforamerica.open311.internals.caching.NoCache;

import java.util.ArrayList;
import java.util.List;

public class GetServiceRequestsService extends IntentService {
    public GetServiceRequestsService() {
        super("GetServiceRequests-service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        ResultReceiver rec = intent.getParcelableExtra("receiver");
        City city = (City) intent.getSerializableExtra("city");

        Bundle bundle = new Bundle();
        APIWrapperFactory factory;
        try {
            if (city != null) {
                factory = new APIWrapperFactory(city).setCache(new NoCache()).withLogs();
            } else {
                factory = new APIWrapperFactory(intent.getStringExtra("endpointUrl"), intent.getStringExtra("jurisdictionId")).setCache(new NoCache()).withLogs();
            }

            APIWrapper wrapper = factory.build();
            GETServiceRequestsFilter filter = new GETServiceRequestsFilter();
            List<ServiceRequest> result = wrapper.getServiceRequests(filter);
            ArrayList<ServiceRequest> requests = new ArrayList<ServiceRequest>();
            for (ServiceRequest request : result) {
                requests.add(request);

            }
            bundle.putParcelableArrayList("Requests", requests);
        } catch (APIWrapperException e) {
            e.printStackTrace();
            rec.send(Activity.RESULT_CANCELED, null);
        }

        rec.send(Activity.RESULT_OK, bundle);
    }
}
