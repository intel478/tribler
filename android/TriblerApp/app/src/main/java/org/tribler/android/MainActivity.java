package org.tribler.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.facebook.stetho.Stetho;

import org.tribler.android.restapi.EventStream;
import org.tribler.android.restapi.IRestApi;
import org.tribler.android.restapi.TriblerService;
import org.tribler.android.restapi.json.EventsStartEvent;
import org.tribler.android.restapi.json.ShutdownAck;
import org.tribler.android.service.Triblerd;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends BaseActivity implements Handler.Callback {

    public static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 101;
    public static final int SEARCH_ACTIVITY_REQUEST_CODE = 102;

    static {
        // Backwards compatibility for vector graphics
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.main_progress)
    View progressView;

    @BindView(R.id.main_progress_status)
    TextView statusBar;

    private ActionBarDrawerToggle _navToggle;
    private ConnectivityManager _connectivityManager;
    private Handler _eventHandler;

    private void initService() {
        // Check network connection before starting service
        if (MyUtils.isNetworkConnected(_connectivityManager)) {

            Triblerd.start(this); // Run normally
            //Twistd.start(this); // Run profiler
            //NoseTestService.start(this); // Run tests
            //ExperimentService.start(this); // Run experiment

        } else {
            Toast.makeText(this, R.string.info_no_connection, Toast.LENGTH_LONG).show();
        }
    }

    private void killService() {
        Triblerd.stop(this);
        //Twistd.stop(this);
        //NoseTestService.stop(this);
        //ExperimentService.stop(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hamburger icon
        _navToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(_navToggle);
        _navToggle.syncState();

        Stetho.initializeWithDefaults(getApplicationContext()); //DEBUG

        initConnectionManager();
        initService();

        // Start listening to events on the main thread so the gui can be updated
        _eventHandler = new Handler(Looper.getMainLooper(), this);
        EventStream.addHandler(_eventHandler);

        if (!EventStream.isReady()) {
            // Show loading indicator
            progressView.setVisibility(View.VISIBLE);
            statusBar.setText(getText(R.string.status_opening_eventstream));

            EventStream.openEventStream();
        }

        handleIntent(getIntent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        drawer.removeDrawerListener(_navToggle);
        EventStream.removeHandler(_eventHandler);
        super.onDestroy();
        _navToggle = null;
        _connectivityManager = null;
        _eventHandler = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handleMessage(Message message) {
        if (message.obj instanceof EventsStartEvent) {
            // Hide loading indicator
            progressView.setVisibility(View.GONE);
            statusBar.setText("");
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        switch (action) {

            case Intent.ACTION_MAIN:
                drawer.openDrawer(GravityCompat.START);
                // Open drawer only once
                intent.setAction(null);
                return;

            case ConnectivityManager.CONNECTIVITY_ACTION:
            case WifiManager.NETWORK_STATE_CHANGED_ACTION:
            case WifiManager.WIFI_STATE_CHANGED_ACTION:

                // Warn user if connection is lost
                if (!MyUtils.isNetworkConnected(_connectivityManager)) {
                    Toast.makeText(MainActivity.this, R.string.warning_lost_connection, Toast.LENGTH_LONG).show();
                }
                return;

            case Intent.ACTION_SHUTDOWN:
                shutdown();
                return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            case CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE:
                switch (resultCode) {

                    case Activity.RESULT_OK:
                        Toast.makeText(this, String.format("Video saved to: %s", data.getData()), Toast.LENGTH_LONG).show();
                        //TODO: create torrent file and add to own channel
                        return;

                    case Activity.RESULT_CANCELED:
                        return;

                    default:
                        Toast.makeText(this, R.string.error_capture_video, Toast.LENGTH_LONG).show();
                        return;
                }

            case SEARCH_ACTIVITY_REQUEST_CODE:
                // Update view
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_main);
                if (fragment instanceof ListFragment) {
                    ((ListFragment) fragment).reload();
                }
                return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Add items to the action bar (if it is present)
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void initConnectionManager() {
        _connectivityManager =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        Observer observer = new Observer<Intent>() {

            public void onNext(Intent intent) {
                handleIntent(intent);
            }

            public void onCompleted() {
            }

            public void onError(Throwable e) {
            }
        };

        // Listen for connectivity changes
        rxSubs.add(RxBroadcast.fromBroadcast(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
                .subscribe(observer));

        // Listen for network state changes
        rxSubs.add(RxBroadcast.fromBroadcast(this, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION))
                .subscribe(observer));

        // Listen for Wi-Fi state changes
        rxSubs.add(RxBroadcast.fromBroadcast(this, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
                .subscribe(observer));
    }

    /**
     * @param newFragmentClass The desired fragment class
     * @return True if fragment is switched, false otherwise
     */
    private boolean switchFragment(Class newFragmentClass) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        // Check if current fragment is desired fragment
        Fragment current = fragmentManager.findFragmentById(R.id.fragment_main);
        if (!newFragmentClass.isInstance(current)) {
            String tag = newFragmentClass.getName();
            // Check if desired fragment is already instantiated
            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment == null) {
                try {
                    fragment = (Fragment) newFragmentClass.newInstance();
                    fragment.setRetainInstance(true);
                } catch (InstantiationException ex) {
                    Log.e("switchFragment", newFragmentClass.getName(), ex);
                } catch (IllegalAccessException ex) {
                    Log.e("switchFragment", newFragmentClass.getName(), ex);
                }
            }
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_main, fragment, tag)
                    .commit();
            return true;
        }
        return false;
    }

    /**
     * @return Fragment that was removed, if any
     */
    @Nullable
    private Fragment removeFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_main);
        if (fragment != null) {
            fragmentManager
                    .beginTransaction()
                    .remove(fragment)
                    .commit();
        }
        return fragment;
    }

    public void btnSearchClicked(MenuItem item) {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivityForResult(intent, SEARCH_ACTIVITY_REQUEST_CODE);
    }

    public void navSubscriptionsClicked(MenuItem item) {
        drawer.closeDrawer(GravityCompat.START);
        if (switchFragment(SubscribedFragment.class)) {
            // Set title
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(getText(R.string.action_subscriptions));
            }
        }
    }

    public void navMyChannelClicked(MenuItem item) {
        drawer.closeDrawer(GravityCompat.START);
        if (switchFragment(MyChannelFragment.class)) {
            // Set title
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(getText(R.string.action_my_channel));
            }
        }
    }

    public void navMyPlaylistsClicked(MenuItem item) {
    }

    public void navPopularClicked(MenuItem item) {
        drawer.closeDrawer(GravityCompat.START);
        if (switchFragment(PopularFragment.class)) {
            // Set title
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(getText(R.string.action_popular_channels));
            }
        }
    }

    public void navCaptureVideoClicked(MenuItem item) {
        drawer.closeDrawer(GravityCompat.START);
        // Check if device has camera
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // Obtain output file
            try {
                File output = MyUtils.getOutputVideoFile(this);
                Intent captureIntent = MyUtils.videoCaptureIntent(Uri.fromFile(output));
                startActivityForResult(captureIntent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
            } catch (IOException ex) {
                Log.e("getOutputVideoFile", getString(R.string.error_output_file), ex);
                Toast.makeText(this, R.string.error_output_file, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void navBeamClicked(MenuItem item) {
        drawer.closeDrawer(GravityCompat.START);
        File apk = new File(this.getPackageResourcePath());
        Intent beamIntent = MyUtils.beamIntent(Uri.fromFile(apk));
        startActivity(beamIntent);
    }

    public void navSettingsClicked(MenuItem item) {
    }

    public void navFeedbackClicked(MenuItem item) {
        drawer.closeDrawer(GravityCompat.START);
        String url = getString(R.string.app_feedback_url);
        Intent browse = MyUtils.viewIntent(Uri.parse(url));
        // Ask user to open url
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(url);
        builder.setPositiveButton(getText(R.string.action_go), (dialog, which) -> {
            startActivity(browse);
        });
        builder.setNegativeButton(getText(R.string.action_cancel), (dialog, which) -> {
            // Do nothing
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void navShutdownClicked(MenuItem item) {
        drawer.closeDrawer(GravityCompat.START);
        Intent shutdown = new Intent(Intent.ACTION_SHUTDOWN);
        // Ask user to confirm shutdown
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.dialog_shutdown));
        builder.setPositiveButton(getText(R.string.action_shutdown_short), (dialog, which) -> {
            onNewIntent(shutdown);
        });
        builder.setNegativeButton(getText(R.string.action_cancel), (dialog, which) -> {
            // Do nothing
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void btnMyChannelAddClicked(MenuItem item) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_main);
        if (fragment instanceof MyChannelFragment) {
            MyChannelFragment mychannel = (MyChannelFragment) fragment;
            mychannel.addToChannel();
        }
    }

    public void btnMyChannelEditClicked(MenuItem item) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_main);
        if (fragment instanceof MyChannelFragment) {
            MyChannelFragment mychannel = (MyChannelFragment) fragment;
            mychannel.editChannel();
        }
    }

    private void shutdown() {
        // Clear view
        removeFragment();

        // Show loading indicator
        progressView.setVisibility(View.VISIBLE);
        statusBar.setText(getText(R.string.status_shutting_down));

        EventStream.closeEventStream();

        String baseUrl = getString(R.string.service_url) + ":" + getString(R.string.service_port_number);
        String authToken = getString(R.string.service_auth_token);
        IRestApi service = TriblerService.createService(baseUrl, authToken);

        rxSubs.add(service.shutdown()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ShutdownAck>() {

                    public void onNext(ShutdownAck response) {
                        Log.v("shutdown", response.toString());
                    }

                    public void onCompleted() {
                        // Stop MainActivity
                        finish();
                        Process.killProcess(Process.myPid());
                    }

                    public void onError(Throwable e) {
                        // Kill process
                        killService();

                        // Stop MainActivity
                        finish();
                        Process.killProcess(Process.myPid());
                    }
                }));
    }

}
