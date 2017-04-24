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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import org.codeforamerica.open311.facade.Servers;
import org.codeforamerica.open311.facade.data.Server;
import org.codeforamerica.open311.facade.data.ServiceRequest;
import org.open311.android.adapters.ServersAdapter;
import org.open311.android.adapters.ViewPagerAdapter;

import org.open311.android.fragments.PolicyFragment;
import org.open311.android.fragments.ProfileFragment;
import org.open311.android.fragments.ReportFragment;
import org.open311.android.fragments.RequestsFragment;
import org.open311.android.helpers.Installation;

import java.util.List;

import static org.open311.android.fragments.ReportFragment.LOCATION_REQUEST;
import static org.open311.android.helpers.Utils.*;

public class MainActivity extends AppCompatActivity
        implements
        RequestsFragment.OnListFragmentInteractionListener,
        FragmentManager.OnBackStackChangedListener {
    private String installationId;
    private Servers servers = new Servers();
    private Server currentServer;
    private static final String LOG_TAG = "MainActivity";

    protected SharedPreferences settings;

    public Server getCurrentServer() {
        settings = getSettings(this);
        String cur_server = settings.getString("current_server", null);
        if (cur_server != null) {
            setCurrentServer(servers.getServer(cur_server));
        } else {
            setCurrentServer(servers.getServer(getString(R.string.open311_endpoint)));
        }
        return currentServer;
    }

    public MainActivity setCurrentServer(Server server) {
        this.currentServer = server;
        return this;
    }

    public String getInstallationId() {
        return installationId;
    }

    private boolean isFirstTime() {
        settings = getPreferences(MODE_PRIVATE);
        boolean ranBefore = settings.getBoolean("RanBefore", false);
        if (!ranBefore) {

            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("RanBefore", true);
            editor.apply();
            final View topLevelLayout = findViewById(R.id.top_layout);
            topLevelLayout.setVisibility(View.VISIBLE);
            final Button welcomeClose = (Button) findViewById(R.id.button_welcome_close);
            welcomeClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    topLevelLayout.setVisibility(View.INVISIBLE);
                }
            });
            topLevelLayout.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    topLevelLayout.setVisibility(View.INVISIBLE);
                    return false;
                }
            });
        }
        return ranBefore;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);

        installationId = Installation.id(this);
        setContentView(R.layout.activity_main);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);


        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        View topLevelLayout = findViewById(R.id.top_layout);
        if (isFirstTime()) {
            topLevelLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final List<Server> servers = new Servers().getCollection();
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem mapItem = menu.findItem(R.id.setting_map);
        if (currentServer.getMap() == null || currentServer.getMap().getRequestMapEnabled()) {
            mapItem.setVisible(true);
            mapItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    //Show the map
                    Intent intent = new Intent(MainActivity.this, MapActivity.class);
                    startActivityForResult(intent, LOCATION_REQUEST);
                    return false;
                }
            });
        } else {
            mapItem.setVisible(false);
        }

        MenuItem serverItem = menu.findItem(R.id.setting_general);
        serverItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
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
                builder.setAdapter(new ServersAdapter(MainActivity.this, servers),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int index) {
                                String result = null;
                                if (!currentServer.getName().equals(values[index])) {
                                    Log.d(LOG_TAG, "onCreateOptionsMenu - Selected Server: " + values[index]);
                                    result = saveSetting(MainActivity.this, "current_server", values[index]);

                                    //remove sharedSettings for Map so map resets
                                    removeSetting(MainActivity.this, "map_address_string");
                                    removeSetting(MainActivity.this, "map_latitude");
                                    removeSetting(MainActivity.this, "map.longitude");
                                    removeSetting(MainActivity.this, "map_zoom");

                                    getCurrentServer();
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
