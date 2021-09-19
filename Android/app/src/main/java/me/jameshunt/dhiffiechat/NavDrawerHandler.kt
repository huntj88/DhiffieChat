package me.jameshunt.dhiffiechat

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.customview.widget.Openable
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import me.jameshunt.dhiffiechat.ui.compose.injectedViewModel
import me.jameshunt.dhiffiechat.ui.home.HomeViewModel
import java.lang.ref.WeakReference

fun Fragment.openNavDrawer() {
    (activity as? MainActivity)?.let {
        val viewModel: HomeViewModel by it.injectedViewModel()
        it.findViewById<DrawerLayout>(R.id.drawer_layout)
            ?.openDrawer(GravityCompat.START)
            .also {
                activity?.findViewById<TextView>(R.id.navigationAliasText)?.text = viewModel.alias.value?.orElse(null)?.alias
            }
    }
}

/**
 * Most of this file is Taken from the compose lib, but modified to fit my use cases
 * Duplicated because you can only have one NavigationItemSelectedListener, and existing one isn't exposed in any way
 */

class NavDrawerHandler(private val activity: MainActivity) {
    private val viewModel: HomeViewModel by activity.injectedViewModel()

    fun init() {
        val navHostFragment = activity.supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        setupWithNavController(
            navigationView = activity.findViewById<NavigationView>(R.id.nav_view),
            navController = navHostFragment.navController,
            onScanSelected = { viewModel.scanSelected() },
            onShareSelected = { viewModel.shareSelected() }
        )

        viewModel.alias.observe(activity) {
            activity.findViewById<TextView>(R.id.navigationAliasText)?.text = it.orElse(null)?.alias
        }
    }

    fun handleBack(onSuperBackPressed: () -> Unit) {
        val drawerLayout = activity.findViewById<DrawerLayout>(R.id.drawer_layout)
        when (drawerLayout.isOpen) {
            true -> drawerLayout.close()
            false -> onSuperBackPressed()
        }
    }

    private fun setupWithNavController(
        navigationView: NavigationView,
        navController: NavController,
        onScanSelected: () -> Unit,
        onShareSelected: () -> Unit
    ) {
        navigationView.setNavigationItemSelectedListener { item ->

            /** custom part starts **/
            if (item.itemId == R.id.scanQR) {
                onScanSelected()
                return@setNavigationItemSelectedListener true
            }

            if (item.itemId == R.id.shareQR) {
                onShareSelected()
                return@setNavigationItemSelectedListener true
            }
            /** custom part ends **/

            val handled = NavigationUI.onNavDestinationSelected(item, navController)
            if (handled) {
                val parent = navigationView.parent
                if (parent is Openable) {
                    (parent as Openable).close()
                } else {
                    val bottomSheetBehavior = findBottomSheetBehavior(navigationView)
                    if (bottomSheetBehavior != null) {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                }
            }
            handled
        }
        val weakReference = WeakReference(navigationView)
        navController.addOnDestinationChangedListener(object :
            NavController.OnDestinationChangedListener {
            override fun onDestinationChanged(
                controller: NavController,
                destination: NavDestination, arguments: Bundle?
            ) {
                val view = weakReference.get()
                if (view == null) {
                    navController.removeOnDestinationChangedListener(this)
                    return
                }
                val menu = view.menu
                var h = 0
                val size = menu.size()
                while (h < size) {
                    val item = menu.getItem(h)
                    item.isChecked = matchDestination(destination, item.itemId)
                    h++
                }
            }
        })
    }

    private fun findBottomSheetBehavior(view: View): BottomSheetBehavior<*>? {
        val params = view.layoutParams
        if (params !is CoordinatorLayout.LayoutParams) {
            val parent = view.parent
            return if (parent is View) {
                findBottomSheetBehavior(parent as View)
            } else null
        }
        val behavior = params
            .behavior
        return if (behavior !is BottomSheetBehavior<*>) {
            // We hit a CoordinatorLayout, but the View doesn't have the BottomSheetBehavior
            null
        } else behavior
    }

    private fun matchDestination(
        destination: NavDestination,
        @IdRes destId: Int
    ): Boolean {
        var currentDestination: NavDestination? = destination
        while (currentDestination!!.id != destId && currentDestination.parent != null) {
            currentDestination = currentDestination.parent
        }
        return currentDestination.id == destId
    }
}