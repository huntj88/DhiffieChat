package me.jameshunt.dhiffiechat

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    private val navDrawerHandler = NavDrawerHandler(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navDrawerHandler.init()
    }

    override fun onBackPressed() {
        navDrawerHandler.handleBack { super.onBackPressed() }
    }
}
