package org.open311.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.codeforamerica.open311.facade.APIWrapper;
import org.codeforamerica.open311.facade.APIWrapperFactory;
import org.codeforamerica.open311.facade.EndpointType;
import org.codeforamerica.open311.facade.data.City;
import org.codeforamerica.open311.facade.data.Service;
import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.codeforamerica.open311.facade.exceptions.APIWrapperException;
import org.open311.android.adapters.ViewPagerAdapter;

import org.open311.android.fragments.CityFragment;
import org.open311.android.fragments.PolicyFragment;
import org.open311.android.fragments.ProfileFragment;
import org.open311.android.fragments.ReportFragment;
import org.open311.android.fragments.RequestsFragment;
import org.open311.android.helpers.Installation;

import java.io.IOException;
import java.util.List;

import static org.open311.android.helpers.Utils.*;

import io.tus.android.client.TusPreferencesURLStore;

public class MainActivity extends AppCompatActivity
        implements
        RequestsFragment.OnListFragmentInteractionListener,
        CityFragment.OnListFragmentInteractionListener,
        FragmentManager.OnBackStackChangedListener {
    private String installationId;

    private List<Service> services;

    private City currentCity;
    private ReportFragment reportFragment;
    private CityFragment cityFragment;
    private static final String LOG_TAG = "MainActivity";

    protected SharedPreferences settings;


    public List<Service> getServices() {
        return services;
    }

    public City getCurrentCity() {
        return currentCity;
    }

    public MainActivity setCurrentCity(City currentCity) {
        this.currentCity = currentCity;
        return this;
    }

    public String getInstallationId() {
        return installationId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);

        settings = getSettings(this);
        setCurrentCity(City.fromString(getString(R.string.open311_endpoint)));
        installationId = Installation.id(this);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);


        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        if (savedInstanceState != null) {
            Log.d(LOG_TAG, "Restoring reportFragment");
            // Restore the fragment's instance
            reportFragment = (ReportFragment) getSupportFragmentManager().getFragment(
                    savedInstanceState, "reportFragment");
        }
        new DownloadPictures().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem actionItem = menu.findItem(R.id.setting_general);
        actionItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                cityFragment = new CityFragment();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.cities_fragment, cityFragment)
                        .commit();
                View citiesView = findViewById(R.id.cities_fragment);
                assert citiesView != null;
                citiesView.bringToFront();
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListFragmentInteraction(ServiceRequest item) {

    }

    @Override
    public void onBackStackChanged() {
        Log.d(LOG_TAG, "onBackStackChanged");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onSaveInstance");
        super.onSaveInstanceState(savedInstanceState);
        if (reportFragment != null) {
            // Save the fragment's instance
            Log.d(LOG_TAG, "Saving reportFragment");
            getSupportFragmentManager().putFragment(
                    savedInstanceState, "reportFragment", reportFragment);
        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.d(LOG_TAG, "onSupportNavigateUp");
        //This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveSettings(this);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), MainActivity.this);
        adapter.addFragment(new ReportFragment());
        adapter.addFragment(new RequestsFragment());
        adapter.addFragment(new ProfileFragment());
        adapter.addFragment(new PolicyFragment());
        viewPager.setAdapter(adapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onListFragmentInteraction(City item) {

    }

    private class DownloadPictures extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;

        @Override
        protected String doInBackground(String... params) {

            APIWrapper wrapper;
            try {
                wrapper = new APIWrapperFactory(getCurrentCity(), EndpointType.PRODUCTION).build();
                publishProgress();
                services = wrapper.getServiceList();
                publishProgress();
                Thread.sleep(2000);

            } catch (APIWrapperException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;

        }

        @Override
        protected void onPostExecute(String result) {

            progressDialog.cancel();

            //Call your method that checks if the pictures were downloaded

        }

        @Override
        protected void onPreExecute() {

            progressDialog = new ProgressDialog(
                    MainActivity.this);

            progressDialog.setMessage(getString(R.string.contactingServer) + " " + getCurrentCity().getCityName());
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

        @Override
        protected void onProgressUpdate(Void... values) {
            if (services == null) {
                progressDialog.setMessage(getString(R.string.connectionEstablished));
            } else {
                if (services.size() > 0) {
                    progressDialog.setMessage(services.size() + " " + getString(R.string.servicesDownloaded));
                }
            }
        }

    }
}
