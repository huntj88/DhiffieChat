package me.jameshunt.dhiffiechat

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.customview.widget.Openable
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import java.lang.ref.WeakReference

/**
 * taken from the compose lib, but modified actions in the navigation bar that don't open a new screen
 */

class NavDrawerHandler {
    fun setupWithNavController(
        navigationView: NavigationView,
        navController: NavController,
        onScanSelected: () -> Unit,
        onShareSelected: () -> Unit
    ) {
        navigationView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.scanQR) {
                onScanSelected()
                return@setNavigationItemSelectedListener true
            }

            if (item.itemId == R.id.shareQR) {
                onShareSelected()
                return@setNavigationItemSelectedListener true
            }

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