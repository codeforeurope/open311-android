package org.open311.android;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
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
import android.view.animation.AccelerateInterpolator;
import android.widget.AdapterView;
import android.widget.TextView;

import android.support.design.widget.FloatingActionButton;

import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.open311.android.adapters.GeocoderAdapter;
import org.open311.android.widgets.GeocoderView;
import org.osmdroid.bonuspack.location.GeocoderNominatim;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private MapboxMap map;
    private Float latitude = 0.0f;
    private Float longitude = 0.0f;
    private String addressString = "";
    private String sourceType = "";
    FloatingActionButton gpsActionButton;
    float gpsActionButtonOrigin = 999.999f;
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
        submitActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent data = new Intent();
                data.putExtra("address_string", addressString);
                data.putExtra("latitude", latitude);
                data.putExtra("longitude", longitude);
                data.putExtra("source", sourceType);
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
                Address result = adapter.getItem(position);
                autocomplete.setText(result.getFeatureName());
                updateAddress(result);
                autocomplete.clearFocus();
                updateMap(result.getLatitude(), result.getLongitude());
            }
        });

        return true;
    }

    private void updateAddress(LatLng result) {

        addressString = "";
        sourceType = "GPS";
        latitude = (float) result.getLatitude();
        longitude = (float) result.getLongitude();

        TextView AddressLine = (TextView) findViewById((R.id.address));
        TextView CoordsLine = (TextView) findViewById((R.id.coords));
        assert AddressLine != null;
        AddressLine.setText("");
        assert CoordsLine != null;
        String coordText = String.format(Locale.getDefault(), "(%f, %f)", result.getLatitude(), result.getLongitude());
        CoordsLine.setText(coordText);
        openBottomSheet();
    }

    private void updateAddress(Address result) {
        if(result.getFeatureName() != null) {
            addressString = result.getFeatureName();
        } else {
            addressString = "";
        }
        sourceType = "SEARCH";
        latitude = (float) result.getLatitude();
        longitude = (float) result.getLongitude();
        TextView AddressLine = (TextView) findViewById((R.id.address));
        TextView CoordsLine = (TextView) findViewById((R.id.coords));
        assert AddressLine != null;
        AddressLine.setText(addressString);
        assert CoordsLine != null;
        String coordText = String.format(Locale.getDefault(), "(%f, %f)", result.getLatitude(), result.getLongitude());
        CoordsLine.setText(coordText);
        openBottomSheet();
    }

    private void updateMap(double latitude, double longitude) {
        MarkerViewOptions marker = new MarkerViewOptions()
                .position(new LatLng(latitude, longitude));

        map.addMarker(marker);
        enableLocation(false);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(13)
                .build();
        map.setCameraPosition(cameraPosition);
        //map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000, null);

    }

    public void closeBottomSheet() {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        if (gpsActionButtonOrigin != 999.999f) {
            gpsActionButton.setTranslationY(gpsActionButtonOrigin);
        }
        submitActionButton.setVisibility(View.INVISIBLE);
    }

    public void openBottomSheet() {
        Log.d(LOG_TAG, Float.toString(gpsActionButton.getTranslationY()));
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        //The GPS action button can only move once.
        if (gpsActionButtonOrigin == 999.999f) {
            gpsActionButtonOrigin = gpsActionButton.getTranslationY();
            gpsActionButton.animate().translationYBy(-200).setInterpolator(new AccelerateInterpolator(2)).start();
        }
        submitActionButton.setVisibility(View.VISIBLE);
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
                        map.setCameraPosition(cameraPosition);
                        //map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000, null);
                        gpsActionButton.setImageResource(R.drawable.ic_my_location);
                        // Start reverse geocoder
                        new ReverseGeocodingTask().execute(latlng);
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

    private class ReverseGeocodingTask extends AsyncTask<LatLng, Void, Void> {
        /**
         * Get services in the background.
         *
         * @param points the LatLng
         */
        @Override
        protected Void doInBackground(LatLng... points) {
            // This method is used to reverse geocode where the user has dropped the marker.
            final String userAgent = "open311_reverse/1.0";
            LatLng point = points[0];
            GeocoderNominatim geocoder = new GeocoderNominatim(getApplicationContext(), userAgent);
            String theAddress;
            try {
                double dLatitude = point.getLatitude();
                double dLongitude = point.getLongitude();
                List<Address> addresses = geocoder.getFromLocation(dLatitude, dLongitude, 1);
                StringBuilder sb = new StringBuilder();
                if (addresses.size() > 0) {
                    Address address = addresses.get(0);
                    updateAddress(address);
                } else {
                    updateAddress(point);
                }
            } catch (IOException e) {
                e.printStackTrace();
                updateAddress(point);
            }
            return null;
        }// reverseGeocode

    }
}
