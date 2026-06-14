package com.akshayAshokCode.androidsensors.presentation

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.databinding.ActivityMainBinding
import com.akshayAshokCode.androidsensors.utils.AnalyticsManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private val TAG = "MainActivity"
    private lateinit var appUpdateManager: AppUpdateManager
    private var pendingToolbarEntry: NavBackStackEntry? = null
    private val toolbarShowObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) supportActionBar?.show()
    }

    // Modern replacement for the deprecated startUpdateFlowForResult(Activity, requestCode) API.
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.e(TAG, "Update flow failed: ${result.resultCode}")
        }
    }

    override fun onStart() {
        super.onStart()
        appUpdateManager.registerListener(listener)
        checkForAvailableUpdate()
    }

    override fun onResume() {
        super.onResume()
        // Check if an update finished downloading while the app was in the background.
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        toolbar = binding.activityMainToolbar
        setSupportActionBar(toolbar)
        navigationView = binding.navView
        drawerLayout = binding.drawerLayout

        binding.navView.setNavigationItemSelectedListener {
            Log.d(TAG, "Clicked Item:" + it.itemId)
            when (it.itemId) {
                R.id.inAppReview -> {
                    triggerInAppReview()
                    drawerLayout.closeDrawers()
                    true
                }

                R.id.sendFeedback -> {
                    AnalyticsManager.logFeedbackSent()

                    val subject = getString(R.string.feedback_subject)
                    val body = feedbackBody()
                    val encodedSubject = Uri.encode(subject)
                    val encodedBody = Uri.encode(body)
                    val email = getString(R.string.feedback_email)
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data =
                            "mailto:$email?subject=$encodedSubject&body=$encodedBody".toUri()
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

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.dashboard),
            drawerLayout
        )
        toolbar.setupWithNavController(navController, appBarConfiguration)

        // Toolbar visibility tracks the destination lifecycle rather than a fixed delay.
        // ON_RESUME fires immediately when transitions are off, and after the enter
        // transition ends when they are on — so this works correctly in both modes.
        val homeDestinations = setOf(R.id.dashboard)
        navController.addOnDestinationChangedListener { controller, destination, _ ->
            pendingToolbarEntry?.lifecycle?.removeObserver(toolbarShowObserver)
            pendingToolbarEntry = null

            if (destination.id in homeDestinations) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                supportActionBar?.hide()
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                controller.currentBackStackEntry?.also { entry ->
                    pendingToolbarEntry = entry
                    entry.lifecycle.addObserver(toolbarShowObserver)
                }
            }
        }

        // Create the update manager once — reused across onStart/onStop cycles.
        appUpdateManager = AppUpdateManagerFactory.create(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isOpen) {
                    drawerLayout.close()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun checkForAvailableUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    )
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Update check failed", exception)
            }
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
            getString(R.string.update_downloaded),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(getString(R.string.update_restart)) { appUpdateManager.completeUpdate() }
            show()
        }
    }

    override fun onStop() {
        appUpdateManager.unregisterListener(listener)
        super.onStop()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        Log.d(TAG, "Clicked Item:" + id)
        return true
    }

    fun triggerInAppReview() {
        AnalyticsManager.logReviewRequested()
        val manager = ReviewManagerFactory.create(this)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(this, task.result)
            }
        }
    }

    private fun feedbackBody(): String {
        return getString(
            R.string.feedback_body_template,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.VERSION.RELEASE,
            packageManager.getPackageInfo(packageName, 0).versionName
        )
    }
}