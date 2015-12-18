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
import org.codeforamerica.open311.facade.data.ServiceDefinition;
import org.codeforamerica.open311.facade.exceptions.APIWrapperException;
import org.codeforamerica.open311.internals.caching.AndroidCache;

import java.util.ArrayList;
import java.util.List;

public class GetServiceDefinitionService extends IntentService {

    public GetServiceDefinitionService() {
        super("GetServiceDefinition-service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        ResultReceiver rec = intent.getParcelableExtra("receiver");
        String serviceCode = intent.getStringExtra("serviceCode");
        City city = (City) intent.getSerializableExtra("city");
        Bundle bundle = new Bundle();

        try {
            APIWrapperFactory factory = new APIWrapperFactory(city).setCache(AndroidCache.getInstance(getApplicationContext()));
            APIWrapper wrapper = factory.build();
            ServiceDefinition result = wrapper.getServiceDefinition(serviceCode);
            bundle.putParcelable("ServiceList", result);
        } catch (APIWrapperException e) {
            e.printStackTrace();
            rec.send(Activity.RESULT_CANCELED, null);
        }

        rec.send(Activity.RESULT_OK, bundle);
    }

}

