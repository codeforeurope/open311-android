package org.open311.android.services;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;

import org.codeforamerica.open311.facade.APIWrapper;
import org.codeforamerica.open311.facade.APIWrapperFactory;
import org.codeforamerica.open311.facade.City;
import org.codeforamerica.open311.facade.EndpointType;
import org.codeforamerica.open311.facade.Format;
import org.codeforamerica.open311.facade.data.Service;
import org.codeforamerica.open311.facade.exceptions.APIWrapperException;
import org.codeforamerica.open311.internals.caching.AndroidCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GetServicesListService extends IntentService {

    public GetServicesListService() {
        super("GetServicesList-service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        ResultReceiver rec = intent.getParcelableExtra("receiver");
        String url = intent.getStringExtra("url");
        String jurisdiction_id = intent.getStringExtra("jurisdiction_id");
        Format format = intent.getParcelableExtra("format");
        City city = (City) intent.getSerializableExtra("city");
        EndpointType endpointtype = intent.getParcelableExtra("endpointtype");
        String apikey = intent.getStringExtra("apikey");
        Bundle bundle = new Bundle();

        try {
            APIWrapperFactory factory = new APIWrapperFactory(city).setCache(AndroidCache.getInstance(getApplicationContext()));
            APIWrapper wrapper = factory.build();
            List<Service> result = wrapper.getServiceList();
            ArrayList<Service> serviceList = new ArrayList<Service>();
            for (Service service : result) {
                serviceList.add(service);

            }
            Collections.sort(serviceList, new Comparator<Service>() {
                public int compare(Service emp1, Service emp2) {
                    return emp1.getServiceName().compareToIgnoreCase(emp2.getServiceName());
                }
            });

            bundle.putParcelableArrayList("ServiceList", serviceList);
        } catch (APIWrapperException e) {
            e.printStackTrace();
            rec.send(Activity.RESULT_CANCELED, null);
        }

        rec.send(Activity.RESULT_OK, bundle);
    }

}

