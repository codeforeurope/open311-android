package org.open311.android;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.codeforamerica.open311.facade.Servers;
import org.codeforamerica.open311.facade.data.City;
import org.codeforamerica.open311.facade.data.Server;
import org.codeforamerica.open311.facade.data.Service;
import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.open311.android.adapters.CitiesAdapter;
import org.open311.android.adapters.ViewPagerAdapter;

import org.open311.android.fragments.PolicyFragment;
import org.open311.android.fragments.ProfileFragment;
import org.open311.android.fragments.ReportFragment;
import org.open311.android.fragments.RequestsFragment;
import org.open311.android.helpers.Installation;

import java.util.List;

import static org.open311.android.helpers.Utils.*;

import io.tus.android.client.TusPreferencesURLStore;

public class MainActivity extends AppCompatActivity
        implements
        RequestsFragment.OnListFragmentInteractionListener,
        FragmentManager.OnBackStackChangedListener {
    private String installationId;

    private List<Service> services;

    private City currentCity;
    private static final String LOG_TAG = "MainActivity";

    protected SharedPreferences settings;

    public City getCurrentCity() {
        settings = getSettings(this);
        String cur_city = settings.getString("current_city", null);
        if (cur_city != null) {
            setCurrentCity(City.fromString(cur_city));
        } else {
            setCurrentCity(City.fromString(getString(R.string.open311_endpoint)));
        }
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

        installationId = Installation.id(this);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);


        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final List<Server> servers = new Servers().getCollection();
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem actionItem = menu.findItem(R.id.setting_general);
        actionItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                if (servers == null) {
                    String msg = getString(R.string.citiesListUnavailable);
                    Snackbar.make(findViewById(R.id.appbar), msg, Snackbar.LENGTH_SHORT)
                            .show();
                    return false;
                }
                final String[] values = new String[servers.size()];
                int index = 0;
                for (Server srv : servers) {
                    values[index] = srv.getName();
                    index++;
                }
                builder.setAdapter(new CitiesAdapter(MainActivity.this, servers),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int index) {
                                String result = null;
                                if (!currentCity.getCityName().equals(values[index])) {
                                    Log.d(LOG_TAG, "onCreateOptionsMenu - Selected City: " + values[index]);
                                    result = saveSetting(MainActivity.this, "current_city", values[index]);
                                    getCurrentCity();
                                    recreate();
                                }
                                if (result != null) {
                                    Snackbar.make(findViewById(R.id.appbar), result, Snackbar.LENGTH_SHORT)
                                            .show();
                                }
                            }
                        });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dialog.dismiss();
                    }
                });
                builder.show();
                return false;
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
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), MainActivity.this);
        adapter.addFragment(new ReportFragment());
        adapter.addFragment(new RequestsFragment());
        adapter.addFragment(new ProfileFragment());
        adapter.addFragment(new PolicyFragment());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                hideKeyBoard(MainActivity.this);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
    }
}
