package org.open311.android;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.open311.android.adapters.ViewPagerAdapter;
import org.open311.android.fragments.PolicyFragment;
import org.open311.android.fragments.ProfileFragment;
import org.open311.android.fragments.ReportFragment;
import org.open311.android.fragments.RequestsFragment;
import org.open311.android.helpers.Installation;

import static org.open311.android.helpers.Utils.*;

public class MainActivity extends AppCompatActivity
        implements
        NavigationView.OnNavigationItemSelectedListener,
        SwipeRefreshLayout.OnRefreshListener,
        RequestsFragment.OnListFragmentInteractionListener,
        SearchView.OnQueryTextListener,
        FragmentManager.OnBackStackChangedListener {
    private ActionBarDrawerToggle toggle;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private String installationId;
    private ReportFragment reportFragment;

    private static final String TAG = MainActivity.class.getSimpleName();

    protected SharedPreferences settings;

    public String getInstallationId() {
        return installationId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSettings(this);
        installationId = Installation.id(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.setDrawerIndicatorEnabled(false); // Hide the hamburger icon
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        //NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        //navigationView.setNavigationItemSelectedListener(this);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        if (savedInstanceState != null) {
            // Restore the fragment's instance
            reportFragment = (ReportFragment) getSupportFragmentManager().getFragment(
                    savedInstanceState, "reportFragment");
        }
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

        // Hide the search feature for now ...
        item.setVisible(false);
        // SearchView searchView = (SearchView) item.getActionView();
        // searchView.setOnQueryTextListener(this);

        // Add click listener to action item -> not yet implemented
        // See: res/menu/main.xml
        MenuItem actionItem = menu.findItem(R.id.action_settings);
        actionItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String msg = "Not yet implemented";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
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

    /**
     * Handle navigation view item clicks here.
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        Fragment fragment = null;
        Class fragmentClass = null;

        int id = item.getItemId();

        switch (id) {
            case R.id.nav_reports_my:
                fragmentClass = PolicyFragment.class;
                break;
            case R.id.nav_reports_recent:
                fragmentClass = RequestsFragment.class;
                break;
            case R.id.nav_settings:
                fragmentClass = ProfileFragment.class;
                break;
            case R.id.nav_about:
                fragmentClass = ReportFragment.class;
                break;
            default: // fragmentClass = null
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
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (reportFragment != null) {
            // Save the fragment's instance
            getSupportFragmentManager().putFragment(
                    savedInstanceState, "reportFragment", reportFragment);
        }
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

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), MainActivity.this);
        adapter.addFragment(new RequestsFragment(), "Requests");
        adapter.addFragment(new ReportFragment(), "Report");
        adapter.addFragment(new ProfileFragment(), "Profile");
        adapter.addFragment(new PolicyFragment(), "Policy");
        viewPager.setAdapter(adapter);
    }
}
