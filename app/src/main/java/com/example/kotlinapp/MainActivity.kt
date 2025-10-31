package com.example.kotlinapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.kotlinapp.R
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)



        if (savedInstanceState == null) {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {

                navController.navigate(R.id.action_loginFragment_to_mainFragment)
            }

        }



        bottomNavView.setupWithNavController(navController)


        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.mainFragment,
                R.id.profileFragment,
                R.id.eventDetailFragment -> {
                    bottomNavView.visibility = View.VISIBLE
                }

                else -> {
                    bottomNavView.visibility = View.GONE
                }
            }
        }
    }
}