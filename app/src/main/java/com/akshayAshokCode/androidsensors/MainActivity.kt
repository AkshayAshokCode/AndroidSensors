package com.akshayAshokCode.androidsensors

import android.content.IntentSender.SendIntentException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.databinding.ActivityMainBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task

// Add Gravity meter
// Add Heart rate meter
// Add pressure meter
// Add Relative Humidity
class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private val REQUEST_CODE = 11
    private lateinit var appUpdateManager: AppUpdateManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        appUpdateManager= AppUpdateManagerFactory.create(this)
        toolbar = binding.activityMainToolbar
        navigationView = binding.navView
        drawerLayout = binding.drawerLayout

        val navHostFrag =
            supportFragmentManager.findFragmentById(R.id.nav_host_frag) as NavHostFragment
        navController = navHostFrag.navController

        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        toolbar.setupWithNavController(navController, appBarConfiguration)
        navigationView.setupWithNavController(navController)

        checkUpdate()
    }

    private fun checkUpdate() {
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                // Request the update.
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        // Pass the intent that is returned by 'getAppUpdateInfo()'.
                        appUpdateInfo,
                        // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                        AppUpdateType.FLEXIBLE,
                        // The current activity making the update request.
                        this,
                        // Include a request code to later monitor this update request.
                        REQUEST_CODE
                    )
                } catch (e: SendIntentException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isOpen) {
            drawerLayout.close()
        } else {
            super.onBackPressed()
        }
    }
}