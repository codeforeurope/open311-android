package org.open311.android;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import android.support.design.widget.FloatingActionButton;

import com.mapbox.services.commons.ServicesException;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.geocoding.v5.models.CarmenFeature;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.services.geocoding.v5.MapboxGeocoding;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.geocoding.v5.models.GeocodingResponse;


import org.open311.android.adapters.GeocoderAdapter;
import org.open311.android.widgets.GeocoderView;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private MapboxMap map;
    FloatingActionButton gpsActionButton;
    FloatingActionButton submitActionButton;
    LocationServices locationServices;
    private GeocoderView autocomplete;
    private BottomSheetBehavior mBottomSheetBehavior;
    private static final String LOG_TAG = "MapActivity";
    private static final int PERMISSIONS_LOCATION = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        locationServices = LocationServices.getLocationServices(MapActivity.this);
        View bottomSheet = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        // Create a mapView
        mapView = (MapView) findViewById(R.id.mapview);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
            }
        });
        gpsActionButton = (FloatingActionButton) findViewById(R.id.map_gps_button);
        submitActionButton = (FloatingActionButton) findViewById(R.id.submit);
        gpsActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (map != null) {
                    toggleGps(!map.isMyLocationEnabled());
                }
            }
        });
        submitActionButton.setOnClickListener(new View.OnClickListener(){
           @Override
           public void onClick(View v){
               Intent data = new Intent();
               data.putExtra("myData1", "Data 1 value");
               data.putExtra("myData2", "Data 2 value");
                // Activity finished ok, return the data
               setResult(RESULT_OK, data);
               finish();
           }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        MenuItem item = menu.findItem(R.id.action_search);

        autocomplete = (GeocoderView) item.getActionView();
        final GeocoderAdapter adapter = new GeocoderAdapter(this);
        autocomplete.setAdapter(adapter);
        // Custom adapter
        autocomplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CarmenFeature result = adapter.getItem(position);
                autocomplete.setText(result.getPlaceName());
                updateAddress(result);
                autocomplete.clearFocus();
                updateMap(result.asPosition().getLatitude(), result.asPosition().getLongitude());
            }
        });

        return true;
    }

    private void updateAddress(LatLng result) {
        TextView AddressLine = (TextView) findViewById((R.id.address));
        TextView CoordsLine = (TextView) findViewById((R.id.coords));
        assert AddressLine != null;
        AddressLine.setText("");
        assert CoordsLine != null;
        String coordText = String.format(Locale.getDefault(), "(%f, %f)", result.getLatitude(), result.getLongitude());
        CoordsLine.setText(coordText);
    }

    private void updateAddress(CarmenFeature result) {
        TextView AddressLine = (TextView) findViewById((R.id.address));
        TextView CoordsLine = (TextView) findViewById((R.id.coords));
        assert AddressLine != null;
        AddressLine.setText(result.getPlaceName());
        assert CoordsLine != null;
        String coordText = String.format(Locale.getDefault(), "(%f, %f)", result.asPosition().getLatitude(), result.asPosition().getLongitude());
        CoordsLine.setText(coordText);
    }

    private void updateMap(double latitude, double longitude) {
        // Marker
        MarkerViewOptions marker = new MarkerViewOptions()
                .position(new LatLng(latitude, longitude));

        map.addMarker(marker);

        // Animate map
        enableLocation(false);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(13)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000, null);
        openBottomSheet();
    }
    public void closeBottomSheet(){
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }
    public void openBottomSheet(){
        //submitActionButton.setVisibility(View.VISIBLE);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @UiThread
    public void toggleGps(boolean enableGps) {
        if (enableGps) {
            // Check if user has granted location permission
            if (!locationServices.areLocationPermissionsGranted()) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
            } else {
                enableLocation(true);
            }
        } else {
            enableLocation(false);
        }
    }


    private void reverseGeocode(final LatLng point) {
        // This method is used to reverse geocode where the user has dropped the marker.
        try {
            MapboxGeocoding client = new MapboxGeocoding.Builder()
                    .setAccessToken(this.getString(R.string.mapbox_api_key))
                    .setCoordinates(Position.fromCoordinates(point.getLongitude(), point.getLatitude()))
                    .setGeocodingType(GeocodingCriteria.TYPE_ADDRESS)
                    .build();

            client.enqueueCall(new Callback<GeocodingResponse>() {
                @Override
                public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {

                    List<CarmenFeature> results = response.body().getFeatures();
                    if (results.size() > 0) {
                        CarmenFeature feature = results.get(0);
                        // If the geocoder returns a result, we take the first in the list and update
                        // the dropped marker snippet with the information. Lastly we open the info
                        // window.
                        updateAddress(feature);
                        openBottomSheet();
                    } else {
                        updateAddress(point);
                        openBottomSheet();
                    }
                }

                @Override
                public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                    Log.e(LOG_TAG, "Geocoding Failure: " + t.getMessage());
                }
            });
        } catch (ServicesException e) {
            Log.e(LOG_TAG, "Error geocoding: " + e.toString());
            e.printStackTrace();
        }
    }// reverseGeocode

    private void enableLocation(boolean enabled) {
        if (enabled) {
            locationServices.addLocationListener(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        LatLng latlng = new LatLng(location);
                        // Move the map camera to where the user location is
                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(latlng)
                                .zoom(16)
                                .build();
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000, null);
                        gpsActionButton.setImageResource(R.drawable.ic_my_location);
                        // Start reverse geocoder
                        reverseGeocode(latlng);

                    }
                }
            });
            gpsActionButton.setImageResource(R.drawable.ic_gps_not_fixed);
        } else {
            gpsActionButton.setImageResource(R.drawable.ic_location_disabled);
        }
        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(enabled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_LOCATION: {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocation(true);
                }
            }
        }
    }
}
