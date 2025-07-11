package com.unade.lsm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<BottomNavigationView>(R.id.bottomNav).apply {
            setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.devices -> {
                        loadFragment(DevicesFragment.newInstance())
                        true
                    }
                    else -> false
                }
            }
        }

        loadFragment(DevicesFragment.newInstance())
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.commit()
    }


}