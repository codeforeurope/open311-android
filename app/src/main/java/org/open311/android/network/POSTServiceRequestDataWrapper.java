package org.open311.android.network;

import org.codeforamerica.open311.facade.data.Attribute;
import org.codeforamerica.open311.facade.data.operations.POSTServiceRequestData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Wrapper for POSTServiceRequestData
 *
 * uReport's Open311Client expects a field called 'address_string'
 * instead of 'address'.
 *
 */
public class POSTServiceRequestDataWrapper extends POSTServiceRequestData {

    private Map<String, String> parameters = new HashMap<String, String>();
    private static final String ADDRESS_TAG = "address_string";

    /**
     * Builds a POST service request data object from a lat & long location value.
     *
     * @param serviceCode
     *            Code of the service related to the request.
     * @param latitude
     *            Latitude using the (WGS84) projection.
     * @param longitude
     *            Longitude using the (WGS84) projection.
     * @param attributes
     *            List of attributes.
     */
    public POSTServiceRequestDataWrapper(
            String serviceCode, float latitude, float longitude, List<Attribute> attributes) {
        super(serviceCode, latitude, longitude, attributes);
    }

    /**
     * Sets an address parameter.
     *
     * @param address
     *            Human-readable address.
     * @return Same instance with the new parameter added.
     */
    @Override
    public POSTServiceRequestDataWrapper setAddress(String address) {
        tryToAddString(ADDRESS_TAG, address);
        return this;
    }

    /**
     * Sets the name.
     *
     * @param name
     *          The name of the person submitting the request.
     * @return Same instance with the new parameter added.
     */
    public POSTServiceRequestDataWrapper setName(String name) {
        this.setFirstName(name); // uReport will just use a (full) name field
        return this;
    }

    /**
     * Tries to add a pair (key, value) to the parameter list. Key and value has
     * to be valid parameters (not null and not empty).
     *
     * @param key
     *            Key of the pair.
     * @param value
     *            Value of the pair.
     */
    private void tryToAddString(String key, String value) {
        if (key != null && key.length() > 0 && value != null && value.length() > 0) {
            parameters.put(key, value);
        }
    }

    /**
     * Builds a map containing all the arguments.
     *
     * @return List of pairs (key, value) with the required arguments.
     */
    @Override
    public Map<String, String> getBodyRequestParameters() {
        Map<String, String> result = new HashMap<String, String>();
        result.putAll(super.getBodyRequestParameters());
        for (Entry<String, String> entry : parameters.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
