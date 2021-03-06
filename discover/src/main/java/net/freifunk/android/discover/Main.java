/*
 * Main.java
 *
 * Copyright (C) 2014  Philipp Dreimann
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */

package net.freifunk.android.discover;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.model.LatLng;

import net.freifunk.android.discover.model.Community;
import net.freifunk.android.discover.model.NodeMap;
import net.freifunk.android.discover.model.Node;
import net.freifunk.android.discover.model.MapMaster;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class Main extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, GmapsFragment.Callbacks {

    private static final int RESULT_SETTINGS = 1;
    private static final String TAG = "Main";

    private static final Collection<Node> nodes = Collections.synchronizedSet(new HashSet<Node>());
    private static final List<Community> communities = Collections.synchronizedList(new ArrayList<Community>());
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private GmapsFragment mMapFragment;
    private RequestQueue mRequestQueue;
    private Fragment mCommunityFragment;
    private ArrayAdapter<Community> mCommunityAdapter;

    private TimerTask updateTask = null;

    private static void setDefaultUncaughtExceptionHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {

                    Log.e("EXCEPTION", "Uncaught Exception detected in thread {}" + t + " -- " + e);
                    Log.e("EXCEPTION", "STACK", e);

                    //e.getStackTrace()
                }
            });
        } catch (SecurityException e) {
            Log.e("EXCEPTION","Could not set the Default Uncaught Exception Handler", e);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDefaultUncaughtExceptionHandler();
        setContentView(R.layout.activity_main);
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        updateDirectory();
        updateMaps();

    }


    private LatLng getLocation() {
        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        // Showing status
        if (status != ConnectionResult.SUCCESS) { // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();

        } else { // Google Play Services are available

            // Getting LocationManager object from System Service LOCATION_SERVICE
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            // Creating a criteria object to retrieve provider
            Criteria criteria = new Criteria();

            // Getting the name of the best provider
            String provider = locationManager.getBestProvider(criteria, true);

            // Getting Current Location
            Location location = locationManager.getLastKnownLocation(provider);
            return new LatLng(location.getLatitude(), location.getLongitude());
        }
        return new LatLng(0, 0);
    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (position) {
            case 0:
                fragmentManager.beginTransaction().replace(R.id.container, GmapsFragment.newInstance(GmapsFragment.NODES_TYPE)).commit();
                break;
            case 1:
                Intent intent = new Intent(this, CommunityListActivity.class);
                startActivity(intent);
                break;
            default:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                        .commit();
                break;
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
/*           case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
                */
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {

            case R.id.action_reload:
                RequestQueueHelper requestHelper = RequestQueueHelper.getInstance(this.getApplicationContext());
                int queueSize = requestHelper.size();

                if (queueSize == 0 && updateTask != null) {
                    updateTask.run();
                }
                else {
                    Log.w(TAG, "No action performed at the moment - QueueSize is " + queueSize);
                }
                break;
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                return true;
         }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMarkerClicked(Object o) {

    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((Main) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }





    void updateDirectory() {
        if (Community.communities.size() > 0)
            return;

        String URL = "";// "https://raw.githubusercontent.com/freifunk/directory.api.freifunk.net/master/directory.json";
        /*
        rq.add(new JsonObjectRequest(URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    Iterator citykeys = jsonObject.keys();
                    while (citykeys.hasNext()) {
                        String cityName = citykeys.next().toString();

                        String detailUrl = jsonObject.getString(cityName);
                        Community comm = new Community(cityName, detailUrl);
                        comm.populate(rq, new Community.CommunityReady() {
                            @Override
                            public void ready(Community c) {
                                Community.communities.add(c);
                            }
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, volleyError.toString());
            }
        }));
        */
    }

    void updateMaps() {
        final String URL = "https://raw.githubusercontent.com/NiJen/AndroidFreifunkNord/master/MapUrls.json";

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        final ConnectivityManager connManager = (ConnectivityManager) getSystemService(this.getBaseContext().CONNECTIVITY_SERVICE);
        final NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        MapMaster mapMaster = MapMaster.getInstance();
        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this.getApplicationContext());
        final RequestQueueHelper requestHelper = RequestQueueHelper.getInstance(this.getApplicationContext());

        final HashMap<String, NodeMap> mapList = databaseHelper.getAllNodeMaps();

        final boolean sync_wifi = sharedPrefs.getBoolean("sync_wifi", true);
        final int sync_frequency = Integer.parseInt(sharedPrefs.getString("sync_frequency", "0"));

        if (sync_wifi == true) {
            Log.d(TAG, "Performing online update ONLY via wifi, every " + sync_frequency + " minutes");
        } else {
            Log.d(TAG, "Performing online update ALWAYS, every " + sync_frequency + " minutes");
        }

        updateTask = new TimerTask() {
            @Override
            public void run() {
                /* load from database */
                for (NodeMap map : mapList.values()) {
                    map.loadNodes();
                }

                /* load from web */
                if (connManager.getActiveNetworkInfo() != null && (sync_wifi == false || mWifi.isConnected() == true)) {
                    Log.d(TAG, "Performing online update. Next update at " + scheduledExecutionTime());
                    requestHelper.add(new JsonObjectRequest(URL, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            try {
                                MapMaster mapMaster = MapMaster.getInstance();

                                Iterator mapkeys = jsonObject.keys();
                                while (mapkeys.hasNext()) {
                                    String mapName = mapkeys.next().toString();
                                    String mapUrl = jsonObject.getString(mapName);

                                    NodeMap m = new NodeMap(mapName, mapUrl);
                                    databaseHelper.addNodeMap(m);

                                    // only update, if not already found in database
                                    if (!mapList.containsKey(m.getMapName())) {
                                        m.loadNodes();
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, e.toString());
                            }
                            finally {
                                requestHelper.RequestDone();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            Log.e(TAG, volleyError.toString());
                            requestHelper.RequestDone();
                        }
                    }));
                } else {
                    Log.d(TAG, "Online update is skipped. Next try at " + scheduledExecutionTime());
                }
            }
        };


        Timer timer = new Timer();

        if (sync_frequency > 0) {
            timer.schedule(updateTask, 0, (sync_frequency * 60 * 1000));
        }
        else {
            timer.schedule(updateTask, 0);
        }

    }
}
