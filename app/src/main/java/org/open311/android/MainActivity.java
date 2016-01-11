package org.open311.android;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.codeforamerica.open311.facade.City;
import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.open311.android.adapters.RequestsAdapter;
import org.open311.android.adapters.ViewPagerAdapter;
import org.open311.android.dummy.DummyContent;
import org.open311.android.fragments.IntroFragment;
import org.open311.android.fragments.ItemFragment;
import org.open311.android.fragments.RequestsFragment;
import org.open311.android.fragments.SettingsFragment;
import org.open311.android.receivers.ServiceRequestsReceiver;
import org.open311.android.services.GetServiceRequestsService;

import java.util.ArrayList;
import java.util.List;

import static org.open311.android.helpers.Utils.*;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        SwipeRefreshLayout.OnRefreshListener,
        IntroFragment.OnFragmentInteractionListener,
        RequestsFragment.OnListFragmentInteractionListener,
        SettingsFragment.OnFragmentInteractionListener,
        ItemFragment.OnListFragmentInteractionListener, SearchView.OnQueryTextListener, FragmentManager.OnBackStackChangedListener {
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    public SharedPreferences getSettings() {
        return settings;
    }

    public void setSettings(SharedPreferences settings) {
        this.settings = settings;
    }

    protected SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = openSettings(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(this);
        //((MenuItem)findViewById(R.id.list_recent_messages)).setChecked(true);
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

    /**
     * Handle navigation view item clicks here.
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        Fragment fragment = null;
        Class fragmentClass = null;
        int id = item.getItemId();
        if (id == R.id.nav_reports_my) {
            fragmentClass = ItemFragment.class;
        } else if (id == R.id.nav_reports_recent) {
            fragmentClass = RequestsFragment.class;
        } else if (id == R.id.nav_settings) {
            fragmentClass = SettingsFragment.class;
        } else if (id == R.id.nav_about) {
            fragmentClass = IntroFragment.class;
        }

        try {
            if (fragmentClass != null) {
                fragment = (Fragment) fragmentClass.newInstance();
            }

            // Insert the fragment by replacing any existing fragment
            if (fragment != null) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.flContent, fragment).addToBackStack(null).commit();
            }
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(item.getTitle());
                if (toggle != null) {
                    toggle.setDrawerIndicatorEnabled(true);
                }
            }
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onListFragmentInteraction(DummyContent.DummyItem item) {

    }

    @Override
    public void onListFragmentInteraction(ServiceRequest item) {

    }

    @Override
    public void onRefresh() {

    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public void onBackStackChanged() {

    }

    @Override
    public boolean onSupportNavigateUp() {
        //This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveSettings(this);
    }

    public void setupGetRequestsServiceReceiver(final RequestsAdapter adapter) {
        // Initialize
        ServiceRequestsReceiver receiver;

        // Service Interfacing
        receiver = new ServiceRequestsReceiver(new Handler());
        receiver.setReceiver(new ServiceRequestsReceiver.Receiver() {
            @Override
            public List<ServiceRequest> onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == Activity.RESULT_OK) {
                    ArrayList<ServiceRequest> result = resultData.getParcelableArrayList("Requests");
                    if (result != null) {
                        Log.d("open311", result.toString());
                        //Update the adapter with the result.
                        adapter.appendRequests(result);
                        return result;
                    } else {
                        Log.w("open311", "No data received!");
                        return null;
                    }
                } else {
                    Log.e("open311", resultData.toString());
                    return null;
                }
            }
        });

        // Activate
        Intent i = new Intent(getApplicationContext(), GetServiceRequestsService.class);
        i.putExtra("receiver", receiver);
        i.putExtra("endpointUrl", settings.getString("endpointUrl", "http://311.baltimorecity.gov/open311/v2"));
        i.putExtra("jurisdictionId", settings.getString("jurisdictionId", "baltimorecity.gov"));
        startService(i);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), MainActivity.this);
        adapter.addFragment(new RequestsFragment(), "ONE");
        adapter.addFragment(new ItemFragment(), "TWO");
        adapter.addFragment(new IntroFragment(), "THREE");
        adapter.addFragment(new SettingsFragment(), "FOUR");
        viewPager.setAdapter(adapter);
    }
}
