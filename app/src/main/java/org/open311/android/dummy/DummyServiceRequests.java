package org.open311.android.dummy;

import org.codeforamerica.open311.facade.data.ServiceRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.open311.android.helpers.Utils.*;

public class DummyServiceRequests {
    /**
     * An array of sample (dummy) items.
     */
    public static final List<ServiceRequest> ITEMS = new ArrayList<ServiceRequest>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static final Map<String, ServiceRequest> ITEM_MAP = new HashMap<String, ServiceRequest>();

    private static final int COUNT = 1;

    static {
        // Add some sample items.
        for (int i = 1; i <= COUNT; i++) {
            addItem(createDummyItem(i));
        }
    }

    private static void addItem(ServiceRequest item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.getServiceRequestId(), item);
    }

    private static ServiceRequest createDummyItem(int position) {
        URL url = null;
        final String encodedURL;
        try {
            encodedURL = "http://lorempixel.com/400/200";
            url = new URL(encodedURL);
        } catch (MalformedURLException e) {
            url = null;
        }
        return new ServiceRequest(String.valueOf(position), ServiceRequest.Status.OPEN, randomString(20), randomString(5),
                randomString(20), randomString(120), randomString(2), randomString(12), randomDate(), randomDate(), randomDate(), randomString(10), 0L,
                0, 0F, 0F, url);
    }

    private static String makeDetails(int position) {
        StringBuilder builder = new StringBuilder();
        builder.append("Details about Item: ").append(position);
        for (int i = 0; i < position; i++) {
            builder.append("\nMore details information here.");
        }
        return builder.toString();
    }

}
