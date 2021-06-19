package me.jameshunt.dhiffiechat

import android.os.Bundle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.navigation.NavigationView
import me.jameshunt.dhiffiechat.ui.compose.injectedViewModel
import me.jameshunt.dhiffiechat.ui.home.HomeViewModel

class MainActivity : FragmentActivity() {
    private val viewModel: HomeViewModel by injectedViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController
        findViewById<NavigationView>(R.id.nav_view).setupWithNavController(navController)
    }

    override fun onBackPressed() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        when (drawerLayout.isOpen) {
            true -> drawerLayout.close()
            false -> super.onBackPressed()
        }
    }

    private val navDrawerHandler = NavDrawerHandler()
    private fun NavigationView.setupWithNavController(navController: NavController) {
        navDrawerHandler.setupWithNavController(
            navigationView = this,
            navController = navController,
            onScanSelected = { viewModel.scanSelected() },
            onShareSelected = { viewModel.shareSelected() }
        )
    }
}

fun Fragment.openNavDrawer() {
    activity
        ?.findViewById<DrawerLayout>(R.id.drawer_layout)
        ?.openDrawer(GravityCompat.START)
}
