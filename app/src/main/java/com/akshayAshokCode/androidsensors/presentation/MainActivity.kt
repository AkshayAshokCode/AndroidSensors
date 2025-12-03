package com.akshayAshokCode.androidsensors.presentation

import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatDelegate
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
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import androidx.core.net.toUri

// Add Gravity meter
// Add Heart rate meter
// Add pressure meter
// Add Relative Humidity
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private val REQUEST_CODE = 11
    private val TAG = "MainActivity"
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onStart() {
        super.onStart()
        checkUpdate()
    }

    override fun onResume() {
        super.onResume()
        if (appUpdateManager != null) {
            appUpdateManager
                .appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    // If the update is downloaded but not installed,
                    // notify the user to complete the update.
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate()
                    } else {
                        Log.d(TAG, "State of update: ${appUpdateInfo.installStatus()}")
                    }
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        toolbar = binding.activityMainToolbar
        navigationView = binding.navView
        drawerLayout = binding.drawerLayout

        binding.navView.setNavigationItemSelectedListener {
            Log.d(TAG, "Clicked Item:" + it.itemId)
            when (it.itemId) {
                R.id.inAppReview -> {
                    val manager = ReviewManagerFactory.create(this)
                    val request = manager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // We got the ReviewInfo object
                            val reviewInfo = task.result
                            val flow = manager.launchReviewFlow(this, reviewInfo)
                            flow.addOnCompleteListener { task ->
                                // The flow has finished. The API does not indicate whether the user
                                // reviewed or not, or even whether the review dialog was shown. Thus, no
                                // matter the result, we continue our app flow.
                            }
                        } else {
                            // There was some problem, log or handle the error code.
                        }
                    }
                    drawerLayout.closeDrawers()
                    true
                }

                R.id.sendFeedback -> {
                    val subject = "AndroidSensors App Feedback"
                    val body = "Device: ${Build.MODEL}\n\nFeedback:\n"
                    val encodedSubject = Uri.encode(subject)
                    val encodedBody = Uri.encode(body)
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data =
                            "mailto:akshayashokan1054@gmail.com?subject=$encodedSubject&body=$encodedBody".toUri()
                    }
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Log.e(TAG, "No email app found on device")
                    }
                    drawerLayout.closeDrawers()
                    true
                }

                else -> {
                    false
                }
            }
        }
        binding.navView.bringToFront()
        val navHostFrag =
            supportFragmentManager.findFragmentById(R.id.nav_host_frag) as NavHostFragment
        navController = navHostFrag.navController

        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        toolbar.setupWithNavController(navController, appBarConfiguration)
        // navigationView.setupWithNavController(navController)
    }

    private fun checkUpdate() {
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.unregisterListener(listener)
        }

        appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this,
                        REQUEST_CODE
                    )
                } catch (e: SendIntentException) {
                    Log.e(TAG, "Update flow failed", e)
                }
            } else {
                Log.e(TAG, "No update available or not allowed")
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Update check failed", exception)
        }

        appUpdateManager.registerListener(listener)
    }

    // Create a listener to track request state updates.
    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // After the update is downloaded, show a notification
            // and request user confirmation to restart the app.
            popupSnackbarForCompleteUpdate()
        }
        // Log state or install the update.
        Log.d(TAG, "State of update: ${state.installStatus()}")
    }

    // Displays the snackbar notification and call to action.
    private fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            findViewById(android.R.id.content),
            "An update has just been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("RESTART") { appUpdateManager.completeUpdate() }
            show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            Log.d(TAG, "onActivityResult: Updated to Latest Features")
            if (resultCode != RESULT_OK) {
                Log.e(TAG, "onActivityResult: app download failed")
            }
        }
    }

    override fun onStop() {
        if (appUpdateManager != null) {
            appUpdateManager.unregisterListener(listener)
        }
        super.onStop()
    }

    override fun onBackPressed() {
        if (drawerLayout.isOpen) {
            drawerLayout.close()
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        Log.d(TAG, "Clicked Item:" + id)
        return true
    }
}