package org.open311.android.receivers;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;

import org.codeforamerica.open311.facade.data.ServiceRequest;

import java.util.List;

@SuppressLint("ParcelCreator")
public class ServiceRequestsReceiver  extends ResultReceiver {
    private Receiver receiver;

    public ServiceRequestsReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    public interface Receiver {
        List<ServiceRequest> onReceiveResult(int resultCode, Bundle resultData);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (receiver != null) {
            receiver.onReceiveResult(resultCode, resultData);
        }
    }
}
