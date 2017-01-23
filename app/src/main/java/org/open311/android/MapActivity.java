package org.open311.android;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.support.design.widget.FloatingActionButton;

import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.codeforamerica.open311.facade.Servers;
import org.codeforamerica.open311.facade.data.Server;
import org.open311.android.adapters.GeocoderAdapter;
import org.open311.android.helpers.Utils;
import org.open311.android.widgets.GeocoderView;
import org.open311.android.helpers.GeocoderNominatim;
import org.osmdroid.tileprovider.util.ManifestUtil;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static org.open311.android.helpers.Utils.getSettings;
import static org.open311.android.helpers.Utils.saveSetting;

public class MapActivity extends AppCompatActivity {
    private SharedPreferences settings;
    private MapView mapView;
    private MapboxMap map;

    private Float latitude = 0.0f;
    private Float longitude = 0.0f;
    private Address address;
    private String sourceType = "";
    FloatingActionButton gpsActionButton;
    float gpsActionButtonOrigin = 999.999f;
    FloatingActionButton submitActionButton;
    LocationServices locationServices;
    private GeocoderView autocomplete;
    private BottomSheetBehavior mBottomSheetBehavior;
    private static final String LOG_TAG = "MapActivity";
    private static final int PERMISSIONS_LOCATION = 201;
    private Context context;

    private enum source {
        GPS("GPS"),
        SEARCH("SEARCH"),
        CLICK("CLICK"),
        REVERSE_GEOCODE("REVERSE_GECODE");

        private final String value;

        source(String val) {
            this.value = val;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        settings = getSettings(this);
        // Mapbox access token only needs to be configured once in your app
        MapboxAccountManager.start(this, getString(R.string.mapbox_api_key));
        setContentView(R.layout.activity_map);
        locationServices = LocationServices.getLocationServices(MapActivity.this);
        context = this;
        View bottomSheet = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Create a mapView
        mapView = (MapView) findViewById(R.id.mapview);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                Servers servers = new Servers();
                Server server = servers.getServer(settings.getString("current_server", getString(R.string.open311_endpoint)));
                if (server.getMap() != null) {
                    LatLng point = new LatLng(
                            settings.getFloat("map_latitude", server.getMap().getLat()),
                            settings.getFloat("map_longitude", server.getMap().getLon())
                    );
                    mapboxMap.setCameraPosition(new CameraPosition.Builder()
                            .target(point)
                            .zoom((double) settings.getFloat("map_zoom", server.getMap().getZoom()))
                            .build());
                } else {
                    LatLng point = new LatLng(
                            settings.getFloat("map_latitude", 0.0f),
                            settings.getFloat("map_longitude", 0.0f)
                    );
                    mapboxMap.setCameraPosition(new CameraPosition.Builder()
                            .target(point)
                            .zoom((double) settings.getFloat("map_zoom", 0.0f))
                            .build());
                }
                map = mapboxMap;
                map.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng point) {
                        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        sourceType = source.CLICK.value;
                        latitude = (float) point.getLatitude();
                        longitude = (float) point.getLongitude();

                        //updateMap, but don't center it
                        updateMap(false);
                    }
                });
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

                data.putExtra("address_string", Utils.formatAddress(address));
                data.putExtra("latitude", latitude);
                data.putExtra("longitude", longitude);
                data.putExtra("source", sourceType);
                saveSetting(MapActivity.this, "map_latitude", latitude);
                saveSetting(MapActivity.this, "map_longitude", longitude);
                saveSetting(MapActivity.this, "map_zoom", (float) map.getCameraPosition().zoom);
                setResult(RESULT_OK, data);
                finish();
            }
        });
        // Click listener that last user manually change address
        View addressDialog = findViewById(R.id.addressdialog);
        addressDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppTheme_Dialog);
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);

                final EditText streetBox = new EditText(context);
                streetBox.setHint(getString(R.string.street));
                streetBox.setText(address.getThoroughfare());
                layout.addView(streetBox);

                final EditText housenumberBox = new EditText(mapView.getContext());
                housenumberBox.setHint(getString(R.string.house_number));
                housenumberBox.setText(address.getSubThoroughfare());
                layout.addView(housenumberBox);
                builder
                        .setTitle(getString(R.string.edit_address))
                        .setView(layout);
                builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //get the housenumber and the street; update the address.
                        address.setSubThoroughfare(String.valueOf(housenumberBox.getText()));
                        address.setThoroughfare(String.valueOf(streetBox.getText()));
                        updateAddress(address);
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dialog.dismiss();
                    }
                });
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                builder.show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map, menu);
        MenuItem item = menu.findItem(R.id.action_search);

        autocomplete = (GeocoderView) item.getActionView();
        final GeocoderAdapter adapter = new GeocoderAdapter(this);
        autocomplete.setAdapter(adapter);
        // Custom adapter
        autocomplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                sourceType = source.SEARCH.value;
                address = adapter.getItem(position);
                autocomplete.setText(Utils.addressString(address));
                updateAddress(address);
                autocomplete.clearFocus();
                latitude = (float) address.getLatitude();
                longitude = (float) address.getLongitude();
                updateMap(true);
            }
        });

        return true;
    }

    private void updateAddress(Address result) {
        address = result;
        TextView AddressLine = (TextView) findViewById((R.id.address));
        TextView CoordsLine = (TextView) findViewById((R.id.coords));
        assert AddressLine != null;
        AddressLine.setText(Utils.formatAddress(address));
        assert CoordsLine != null;
        String coordText = String.format(Locale.getDefault(), "(%f, %f)", latitude, longitude);
        CoordsLine.setText(coordText);
        openBottomSheet();
    }

    private void updateMap(Boolean recenter) {
        map.clear();
        Double zoom = map.getCameraPosition().zoom;
        LatLng point = new LatLng(latitude, longitude);
        MarkerViewOptions marker = new MarkerViewOptions()
                .position(point);

        map.addMarker(marker);
        enableLocation(false);
        if (recenter) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(point))
                    .zoom(13)
                    .build();
            if (zoom > 13) {
                cameraPosition = new CameraPosition.Builder()
                        .target(point)
                        .build();
            }
            map.setCameraPosition(cameraPosition);
        }
        sourceType = source.GPS.value;
        new ReverseGeocodingTask().execute(point);
    }

    public void openBottomSheet() {
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
                        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        LatLng latlng = new LatLng(location);
                        // Move the map camera to where the user location is
                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(latlng)
                                .zoom(16)
                                .build();
                        map.setCameraPosition(cameraPosition);
                        //map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000, null);
                        gpsActionButton.setImageResource(R.drawable.ic_my_location);
                        sourceType = source.GPS.value;
                        // Start reverse geocoder
                        new ReverseGeocodingTask().execute(latlng);
                        enableLocation(false);
                    }
                }
            });
            gpsActionButton.setImageResource(R.drawable.ic_gps_not_fixed);
        } else {
            gpsActionButton.setImageResource(R.drawable.ic_location_disabled);
        }
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


    private class ReverseGeocodingTask extends AsyncTask<LatLng, Void, Address> {
        /**
         * Get services in the background.
         *
         * @param points the LatLng
         */
        @Override
        protected Address doInBackground(LatLng... points) {
            // This method is used to reverse geocode where the user has dropped the marker.
            final String userAgent = "open311/1.0 (reverse; info@open311.io)";
            LatLng point = points[0];
            GeocoderNominatim geocoder = new GeocoderNominatim(getBaseContext(), Locale.getDefault(), userAgent);
            geocoder.setKey(ManifestUtil.retrieveKey(getBaseContext(), "MAPQUEST_API_KEY"));
            geocoder.setService(GeocoderNominatim.MAPQUEST_SERVICE_URL);
            try {
                latitude = (float) point.getLatitude();
                longitude = (float) point.getLongitude();

                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses.size() > 0) {
                    sourceType = source.REVERSE_GEOCODE.value;
                    return addresses.get(0);

                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Address result) {
            Log.d(LOG_TAG, "update address");
            updateAddress(result);
        }
    }
}
