package com.example.supervisor_seerem.UI;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.supervisor_seerem.R;
import com.example.supervisor_seerem.UI.util.MapInfoWindowAdapter;
import com.example.supervisor_seerem.model.CONSTANTS;
import com.example.supervisor_seerem.model.DocumentManager;
import com.example.supervisor_seerem.model.ModelLocation;
import com.example.supervisor_seerem.model.Site;
import com.example.supervisor_seerem.model.Worker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class  SiteMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private boolean isLocationPermissionsGranted = false;
    private GoogleMap siteMap;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 10000;
    private static final float DEFAULT_ZOOM = 15f; // 15 = street level view
    private static final String LAUNCH_FROM_ACTIVITY = "Launched map from other activities";
    private String previousActivity;

    private DocumentManager documentManager = DocumentManager.getInstance();
    private List<DocumentSnapshot> sitesList = new ArrayList<>();
    private Site clickedSite;
    private List<DocumentSnapshot> workersList = new ArrayList<>();
    private Worker clickedWorker;

    private EditText searchInputEditText;
    private ImageView myLocationImageView;
    private DrawerLayout drawer;

    public static Intent launchMapIntent(Context context) {
        Intent mapIntent = new Intent(context, SiteMapActivity.class);
        return mapIntent;
    }

    public static Intent launchMapWithZoomToLocation(Context context, String fromActivity) {
        Intent mapIntent = new Intent(context, SiteMapActivity.class);
        mapIntent.putExtra(LAUNCH_FROM_ACTIVITY, fromActivity);
        return mapIntent;
    }

    private void getPreviousActivityName() {
        Intent intent = getIntent();
        previousActivity = intent.getStringExtra(LAUNCH_FROM_ACTIVITY);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        siteMap = googleMap;

        if (isLocationPermissionsGranted) {
            if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // if somehow the permissions are still not granted...
                return;
            }

            // otherwise, if everything's okay...
            setupMapComponents();
            siteMap.setMyLocationEnabled(true);
            siteMap.getUiSettings().setMyLocationButtonEnabled(false);

            Log.d("FROM MAP: ", "previousActivity is: " + previousActivity);
            if (previousActivity != null && previousActivity.equals("SiteInfo")){ // if the user clicks on a site from the list of worksites
                zoomToSiteLocation();
            } else if (previousActivity != null && previousActivity.equals("WorkerInfo")) { // if the user clicks on a worker from the list of workers
               zoomToWorkerPosition();
            } else {
                // otherwise, just show my current location and all workers' and worksites' locations
                getDeviceLocation();
                showAllWorkersPositions();
                showAllWorksitesLocations();
            }
        }
    }

    private void setupSidebarNavigationDrawer() {
        drawer = findViewById(R.id.sidebar_drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.sidebar_navigation_view);

        // customized toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_for_sidebar);
        setSupportActionBar(toolbar);

        // header
        View headerView = navigationView.getHeaderView(0);
        String savedFirstName = documentManager.getCurrentUser().getFirstName();
        String savedLastName = documentManager.getCurrentUser().getLastName();
        TextView sidebarFullName = (TextView) headerView.findViewById(R.id.sidebar_header_fullname_textview);
        sidebarFullName.setText(savedFirstName + " " + savedLastName);

        final SharedPreferences loginSharedPreferences = getSharedPreferences("LoginData", Context.MODE_PRIVATE);
        String savedUsername = loginSharedPreferences.getString("username", null);
        TextView sidebarUsername = (TextView) headerView.findViewById(R.id.sidebar_header_username_textview);
        sidebarUsername.setText(savedUsername);

        // onClickListener
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.sidebar_user:
                        Intent userIntent = UserInfoActivity.launchUserInfoIntent(SiteMapActivity.this);
                        startActivity(userIntent);
                        finish();
                        break;

                    case R.id.sidebar_overtime:
                        Intent overtimeIntent = AddOvertimeActivity.launchAddOvertimeIntent(SiteMapActivity.this);
                        startActivity(overtimeIntent);
                        break;

                    case R.id.sidebar_day_leave:
                        Intent dayLeaveIntent = AddDayLeaveActivity.launchAddDayLeaveIntent(SiteMapActivity.this);
                        startActivity(dayLeaveIntent);
                        break;

                    case R.id.sidebar_search:
                        break;

                    case R.id.sidebar_all_workers:
                        Intent workerIntent = WorkerInfoActivity.launchWorkerInfoIntent(SiteMapActivity.this);
                        startActivity(workerIntent);
                        finish();
                        break;

                    case R.id.sidebar_company:
                        break;

                    case R.id.sidebar_ui_preferences:
                        Intent uiPrefsIntent = UIPreferencesActivity.launchUIPreferencesIntent(SiteMapActivity.this);
                        startActivity(uiPrefsIntent);
                        break;

                    case R.id.sidebar_light_dark_mode:
                        Intent changeThemeIntent = ChangeThemeActivity.launchChangeThemeIntent(SiteMapActivity.this);
                        startActivity(changeThemeIntent);
                        break;

                    case R.id.sidebar_languages:
                        Intent changeLanguageIntent = ChangeLanguageActivity.launchChangeLanguageIntent(SiteMapActivity.this);
                        startActivity(changeLanguageIntent);
                        break;

                    case R.id.sidebar_change_password:
                        Intent changePasswordIntent = ChangePasswordActivity.launchChangePasswordIntent(SiteMapActivity.this);
                        startActivity(changePasswordIntent);
                        break;
                }
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });
    }

    private void setupNavigationBar() {
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.bottomNavigationBar);
        navigation.setSelectedItemId(R.id.mapNavigation);

        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch(menuItem.getItemId()) {
                    case R.id.workerNavigation:
                        Intent workerIntent = WorkerInfoActivity.launchWorkerInfoIntent(SiteMapActivity.this);
                        finish();
                        startActivity(workerIntent);
                        overridePendingTransition(0,0);
                        return true;

                    case R.id.siteNavigation:
                        Intent siteIntent = SiteInfoActivity.launchSiteInfoIntent(SiteMapActivity.this);
                        finish();
                        startActivity(siteIntent);
                        overridePendingTransition(0,0);
                        return true;

                    case R.id.mapNavigation:
                        // home activity --> do nothing
                        return true;

                    case R.id.sensorNavigation:
                        Intent sensorIntent = SensorsUsageActivity.launchSensorUsageIntent(SiteMapActivity.this);
                        finish();
                        startActivity(sensorIntent);
                        overridePendingTransition(0,0);
                        return true;

                    case R.id.userNavigation:
                        Intent userIntent = UserInfoActivity.launchUserInfoIntent(SiteMapActivity.this);
                        finish();
                        startActivity(userIntent);
                        overridePendingTransition(0,0);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            finishAffinity();
            Intent intent = UserInfoActivity.launchUserInfoIntent(SiteMapActivity.this);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_map);
        setupNavigationBar();
        setupSidebarNavigationDrawer();
        getPreviousActivityName();

        sitesList.clear();
        sitesList.addAll(documentManager.getSites());
        Log.d("FROM MAP", "onCreate(): sitesList.size() = " + sitesList.size());
        workersList.clear();
        workersList.addAll(documentManager.getWorkers());
        Log.d("FROM MAP", "onCreate(): workersList.size() = " + workersList.size());

        searchInputEditText = (EditText) findViewById(R.id.searchInputEditText);
        myLocationImageView = (ImageView) findViewById(R.id.myLocationImageView);

        checkGooglePlayServicesAvailable();
        getLocationPermission();
    }

    private void checkGooglePlayServicesAvailable() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(SiteMapActivity.this);

        if (available != ConnectionResult.SUCCESS) {
            if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
                // no services, but can deal with it
                Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(SiteMapActivity.this, available, ERROR_DIALOG_REQUEST);
                dialog.show();
            } else {
                Toast.makeText(SiteMapActivity.this,
                                R.string.map_noServices_error_message,
                                Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocationPermission() {
        String[] permissions = {FINE_LOCATION, COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                isLocationPermissionsGranted = true;
                initializeMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(SiteMapActivity.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        isLocationPermissionsGranted = false;

        switch(requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result == PackageManager.PERMISSION_GRANTED) {
                            isLocationPermissionsGranted = true;
                            break;
                        } else {
                            isLocationPermissionsGranted = false;
                        }
                    }
                    if (isLocationPermissionsGranted) {
                        initializeMap();
                    }
                }
        }
    }

    private void setupMapComponents() {
        searchInputEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionID, KeyEvent keyEvent) {
                if (actionID == EditorInfo.IME_ACTION_SEARCH ||
                        actionID == EditorInfo.IME_ACTION_DONE ||
                        actionID == KeyEvent.ACTION_DOWN ||
                        actionID == KeyEvent.KEYCODE_ENTER) {
                    goToSearchedLocation();
                }
                return false;
            }
        });

        myLocationImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                previousActivity = "SiteMap";
                siteMap.clear();
                Log.d("FROM MAP", "Clear map and go to my location!");
                getDeviceLocation();
                showAllWorkersPositions();
                showAllWorksitesLocations();
            }
        });
        hideSoftKeyboard();
    }

    private void goToSearchedLocation() {
        String searchInput = searchInputEditText.getText().toString();
        Geocoder geocoder = new Geocoder(SiteMapActivity.this);

        List<Address> addresses = new ArrayList<>();
        try {
            addresses = geocoder.getFromLocationName(searchInput, 1);
        } catch (IOException e) {
            Log.e("SiteMapActivity", "goToSearchedLocation(): IOException" + e.getMessage());
        }

        if (addresses.size() > 0) {
            Address address = addresses.get(0);
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()),
                    DEFAULT_ZOOM,
                    address.getAddressLine(0));
        }
    }

    private void getDeviceLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (isLocationPermissionsGranted) {
                Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Location currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                        DEFAULT_ZOOM, getResources().getString(R.string.map_info_window_user_location_title));
                        } else {
                            Toast.makeText(SiteMapActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("SiteMapActivity error", "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void showAllWorksitesLocations() {
        for (DocumentSnapshot site : sitesList) {
            Site newSite = createSite(site);
            LatLng latLng = new LatLng(newSite.getLocation().getLatitude(), newSite.getLocation().getLongitude());
            String title = getResources().getString(R.string.map_info_window_site_location_title);
            displayWorksiteMarker(newSite, latLng, title);
        }
    }

    private void showAllWorkersPositions() {
        for (DocumentSnapshot worker : workersList) {
            Worker newWorker = createWorker(worker);
            LatLng latLng = new LatLng(newWorker.getLocation().getLatitude(), newWorker.getLocation().getLongitude());
            String title = getResources().getString(R.string.map_info_window_worker_location_title);
            displayWorkerMarker(newWorker, latLng, title);
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        // zoom to the specific latLng
        siteMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        siteMap.setInfoWindowAdapter(new MapInfoWindowAdapter(SiteMapActivity.this));

        if (previousActivity != null && previousActivity.equals("SiteInfo")) {
            displayWorksiteMarker(clickedSite, latLng, title);
        } else if (previousActivity != null && previousActivity.equals("WorkerInfo")) {
            displayWorkerMarker(clickedWorker, latLng, title);
        } else {
            displayUserMarker(latLng, title);
        }

        hideSoftKeyboard();
    }

    private void zoomToSiteLocation() {
        Intent intent = getIntent();
        String clickedSiteID = intent.getStringExtra("SITE ID FROM SiteInfoActivity");
        Log.d("FROM MAP", "clickedSiteID = " + clickedSiteID);

        DocumentSnapshot currentSite = null;
        Log.d("FROM MAP", "zoomToSiteLocation(): sitesList.size() = " + sitesList.size());
        for (DocumentSnapshot site : sitesList) {
            Log.d("FROM MAP", site.toString());
            Log.d("FROM MAP", "siteID = " + site.getString(CONSTANTS.ID_KEY));
            if (site.getString(CONSTANTS.ID_KEY).equals(clickedSiteID)) {
                currentSite = site;
                break;
            }
        }

        if (currentSite != null) {
            clickedSite = createSite(currentSite);
            System.out.println(clickedSite.toString());
            moveCamera(new LatLng(clickedSite.getLocation().getLatitude(), clickedSite.getLocation().getLongitude()),
                    DEFAULT_ZOOM, getResources().getString(R.string.map_info_window_site_location_title));
        }
    }

    private void zoomToWorkerPosition() {
        Intent intent = getIntent();
        String clickedWorkerID = intent.getStringExtra("WorkerID FROM WorkerInfoActivity");
        Log.d("FROM MAP", "clickedWorkerID = " + clickedWorkerID);

        DocumentSnapshot currentWorker = null;
        Log.d("FROM MAP", "zoomToSiteLocation(): workersList.size() = " + workersList.size());
        for (DocumentSnapshot worker : workersList) {
            Log.d("FROM MAP", worker.toString());
            Log.d("FROM MAP", "workerID = " + worker.getString(CONSTANTS.ID_KEY));
            if (worker.getString(CONSTANTS.ID_KEY).equals(clickedWorkerID)) {
                currentWorker = worker;
                break;
            }
        }

        if (currentWorker != null) {
            clickedWorker = createWorker(currentWorker);
            System.out.println(clickedWorker.toString());
            moveCamera(new LatLng(clickedWorker.getLocation().getLatitude(), clickedWorker.getLocation().getLongitude()),
                    DEFAULT_ZOOM, getResources().getString(R.string.map_info_window_worker_location_title));
        }
    }

    private void displayWorksiteMarker(Site site, LatLng latLng, String title) {
        String info = "Site;" + site.getID() +
                ";" + site.getSiteName() +
                ";" + site.getProjectID() +
                ";" + site.getOperationHour();

        // set worksite's marker's color to green
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet(info)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        siteMap.addMarker(options);
        Log.d("FROM MAP", "Marker's color is green");
    }

    private void displayWorkerMarker(Worker worker, LatLng latLng, String title) {
        String info = "Worker;" + worker.getEmployeeID() +
                ";" + worker.getFirstName() + " " + worker.getLastName() +
                ";" + worker.getCompanyID() +
                ";" + worker.getSupervisorID();

        // set worker's marker's color to blue
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet(info)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
        siteMap.addMarker(options);
        Log.d("FROM MAP", "Marker's color is violet");
    }

    private void displayUserMarker(LatLng latLng, String title) {
        String info = "User; " + getResources().getString(R.string.map_info_window_user_location_snippet);

        // set user's marker's color to red
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet(info)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        siteMap.addMarker(options);
        Log.d("FROM MAP", "Marker's color is red");
    }

    private Site createSite(DocumentSnapshot site) {
        String siteID = site.getString(CONSTANTS.ID_KEY);
        String siteName = site.getString(CONSTANTS.WORKSITE_NAME_KEY);
        ModelLocation siteLocation = new ModelLocation(site.getGeoPoint(CONSTANTS.LOCATION_KEY).getLatitude(),
                site.getGeoPoint(CONSTANTS.LOCATION_KEY).getLongitude());
        String projectID = site.getString(CONSTANTS.PROJECT_ID_KEY);
        ModelLocation masterpointLocation = new ModelLocation(site.getGeoPoint(CONSTANTS.MASTERPOINT_KEY).getLatitude(),
                site.getGeoPoint(CONSTANTS.MASTERPOINT_KEY).getLongitude());
        String hseLink = site.getString(CONSTANTS.HSE_LINK_KEY);
        String operationHour = site.getString(CONSTANTS.OPERATION_HRS_KEY);

        Site newSite = new Site(siteID, projectID, siteName, siteLocation,
                masterpointLocation,hseLink, operationHour);

        return newSite;
    }

    private Worker createWorker(DocumentSnapshot worker) {
        String workerID = worker.getString(CONSTANTS.ID_KEY);
        String firstName = worker.getString(CONSTANTS.FIRST_NAME_KEY);
        String lastName = worker.getString(CONSTANTS.LAST_NAME_KEY);
        String supervisorID = worker.getString(CONSTANTS.SUPERVISOR_ID_KEY);
        String siteID = worker.getString(CONSTANTS.WORKSITE_ID_KEY);
        String companyID = worker.getString(CONSTANTS.COMPANY_ID_KEY);
        ModelLocation workerPosition = new ModelLocation(worker.getGeoPoint(CONSTANTS.LOCATION_KEY).getLatitude(),
                worker.getGeoPoint(CONSTANTS.LOCATION_KEY).getLongitude());
        List<String> skills = new ArrayList<String>();
        String[] workerSkills = worker.getString(CONSTANTS.SKILLS_KEY).split(",");
        for (String skill : workerSkills) {
            skills.add(skill);
        }

        Worker newWorker = new Worker(workerID, firstName, lastName, supervisorID,
                siteID, companyID, workerPosition, skills);

        return newWorker;
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
}