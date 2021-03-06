package org.ttnmapper.phonesurveyor.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import org.ttnmapper.phonesurveyor.R
import org.ttnmapper.phonesurveyor.aggregates.AppAggregate
import org.ttnmapper.phonesurveyor.model.Gateway
import org.ttnmapper.phonesurveyor.services.MyService


class MainActivity: AppCompatActivity() {

    private val TAG = MainActivity::class.java.getName()

    private val RECORD_REQUEST_CODE = 101
    val PERMISSION_ALL = 1

    val settingsFragment = SettingsFragment()
    val mapFragment = MapFragment()
    val statsFragment = StatsFragment()

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_settings -> {
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.frame_fragmentholder, settingsFragment, "Settings")
                        .commit();
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_map -> {
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.frame_fragmentholder, mapFragment, "Map")
                        .commit();
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_stats -> {
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.frame_fragmentholder, statsFragment, "Stats")
                        .commit();
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Save a handle to the main activity in the app aggregate singleton
        // Needed to send updates to UI from service
        AppAggregate.mainActivity = this

        fragmentManager
                .beginTransaction()
                .replace(R.id.frame_fragmentholder, mapFragment, "Map")
                .commit();

        navigation.getMenu().getItem(1).setChecked(true);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        setupPermissions()
    }

    override fun onDestroy() {
        //AppAggregate.stopService()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu items for use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.action_bar_options, menu)

        val serviceClass = MyService::class.java
        if(AppAggregate.isServiceRunning(serviceClass)) {
            menu.findItem(R.id.action_start_stop).setTitle("Stop")
        } else {
            menu.findItem(R.id.action_start_stop).setTitle("Start")
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_start_stop -> {
            val serviceClass = MyService::class.java
            if(AppAggregate.isServiceRunning(serviceClass)) {
                AppAggregate.stopService()
                item.setTitle("Start")
            } else {
                AppAggregate.startService()
                item.setTitle("Stop")
            }
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    fun hasPermissions(context: Context?, permissions: List<String>): Boolean {
        if (context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    private fun setupPermissions() {
        val PERMISSIONS = listOf(android.Manifest.permission.WAKE_LOCK, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.ACCESS_FINE_LOCATION)

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS.toTypedArray(), PERMISSION_ALL)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            var packageName = packageName;

            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

            if (pm.isIgnoringBatteryOptimizations(packageName))
                //intent.setAction(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            else {
                var intent = Intent()
                intent.setAction(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            RECORD_REQUEST_CODE -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user")
                } else {
                    Log.i(TAG, "Permission has been granted by user")
                }
            }

            PERMISSION_ALL -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission has been denied by user")
                } else {
                    Log.i(TAG, "Permission has been granted by user")
                }
            }
        }
    }

    fun setMQTTStatusMessage(message: String) {
        mapFragment.setMQTTStatusMessage(message)
    }

    fun setGPSStatusMessage(message: String) {
        mapFragment.setGPSStatusMessage(message)
    }

    fun drawLineOnMap(startLat: Double, startLon: Double, endLat: Double, endLon: Double, colour: Long) {
        runOnUiThread(
                {
                    mapFragment.drawLineOnMap(startLat, startLon, endLat, endLon, colour)
                }
        )
    }

    fun drawPointOnMap(lat: Double, lon: Double, colour: Long) {
        runOnUiThread({
            mapFragment.drawPointOnMap(lat, lon, colour)
        })
    }

    fun addGatewayToMap(gateway: Gateway) {
        runOnUiThread({
            mapFragment.addGatewayToMap(gateway)
        })

    }
}
