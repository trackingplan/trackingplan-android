package com.trackingplan.examples.customerioexample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.trackingplan.client.sdk.Trackingplan
import com.trackingplan.examples.customerioexample.databinding.ActivityMainBinding
import io.customer.sdk.CustomerIO
import io.customer.sdk.data.model.Region

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Trackingplan.init("YOUR_TP_ID")
            // .environment("PRODUCTION")
            // .sourceAlias("Android Example")
            // .customDomains(customDomains)
            .enableDebug()
            .dryRun()
            .start(this)

        CustomerIO.Builder(
            siteId = BuildConfig.CUSTOMER_IO_SITE_ID,
            apiKey = BuildConfig.CUSTOMER_IO_API_KEY,
            appContext = this.application)
            // .setRegion(Region.EU)
            .autoTrackScreenViews(true)
            .build()

        CustomerIO.instance()
            .identify(
                identifier = "TEST1",
                attributes = mapOf("first_name" to "Nick", "email" to "test@example.com")
            )


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}