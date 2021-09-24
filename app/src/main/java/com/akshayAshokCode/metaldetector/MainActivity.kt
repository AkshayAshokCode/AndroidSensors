package com.akshayAshokCode.metaldetector

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.akshayAshokCode.metaldetector.databinding.ActivityMainBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        toolbar = binding.activityMainToolbar
        navigationView=binding.navView
        drawerLayout=binding.drawerLayout

        val navHostFrag =
            supportFragmentManager.findFragmentById(R.id.nav_host_frag) as NavHostFragment
        navController = navHostFrag.navController

        val appBarConfiguration= AppBarConfiguration(navController.graph,drawerLayout)
        toolbar.setupWithNavController(navController,appBarConfiguration)
        navigationView.setupWithNavController(navController)
    }

    override fun onBackPressed() {
        if(drawerLayout.isOpen){
            drawerLayout.close()
        }else {
            super.onBackPressed()
        }
    }
}