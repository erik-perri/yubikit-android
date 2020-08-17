package com.yubico.yubikit.android.app

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.yubico.yubikit.android.YubiKitManager
import com.yubico.yubikit.android.transport.nfc.NfcConfiguration
import com.yubico.yubikit.android.transport.usb.UsbConfiguration
import com.yubico.yubikit.android.transport.usb.UsbSession
import com.yubico.yubikit.android.transport.usb.UsbSessionListener
import com.yubico.yubikit.utils.Logger
import kotlinx.android.synthetic.main.dialog_about.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    private val viewModel: MainViewModel by viewModels()

    private lateinit var yubikit: YubiKitManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Logger.setLogger(object : Logger() {
            override fun logDebug(message: String) {
                Log.d("yubikit", message);
            }

            override fun logError(message: String, throwable: Throwable) {
                Log.e("yubikit", message, throwable)
            }
        })

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.nav_mgmt, R.id.nav_otp, R.id.nav_piv, R.id.nav_oath), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        yubikit = YubiKitManager(this)

        viewModel.handleYubiKey.observe(this, Observer {
            if (it) {
                Logger.d("Enable listening")
                yubikit.startUsbDiscovery(UsbConfiguration(), object : UsbSessionListener {
                    override fun onSessionReceived(session: UsbSession, hasPermission: Boolean) {
                        Logger.d("USB Session started $session, $hasPermission, current: ${viewModel.yubiKey.value}")
                        if (hasPermission) {
                            viewModel.yubiKey.value = session
                        }
                    }

                    override fun onRequestPermissionsResult(session: UsbSession, isGranted: Boolean) {
                        Logger.d("Permission result $session, $isGranted, current: ${viewModel.yubiKey.value}")
                        if (isGranted) {
                            viewModel.yubiKey.value = session
                        }
                    }

                    override fun onSessionRemoved(session: UsbSession) {
                        Logger.d("Session removed $session")
                        if (viewModel.yubiKey.value == session) {
                            viewModel.yubiKey.value = null
                        }
                    }
                })
                yubikit.startNfcDiscovery(NfcConfiguration(), this) { session ->
                    Logger.d("NFC Session started $session")
                    viewModel.yubiKey.apply {
                        value = session
                        postValue(null)
                    }
                }
            } else {
                Logger.d("Disable listening")
                yubikit.stopNfcDiscovery(this)
                yubikit.stopUsbDiscovery()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> {
                AlertDialog.Builder(this)
                        .setView(R.layout.dialog_about)
                        .create().apply {
                            setOnShowListener {
                                version.text = String.format(Locale.getDefault(), getString(R.string.version), BuildConfig.VERSION_NAME);
                            }
                        }.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        Logger.d("ON RESUME ACTIVITY")
        if (viewModel.handleYubiKey.value == true) {
            yubikit.startNfcDiscovery(NfcConfiguration(), this) { session ->
                Logger.d("NFC Session started $session")
                viewModel.yubiKey.apply {
                    value = session
                    postValue(null)
                }
            }
        }
    }

    override fun onPause() {
        Logger.d("ON PAUSE ACTIVITY")
        yubikit.stopNfcDiscovery(this)
        super.onPause()
    }

    override fun onDestroy() {
        Logger.d("ON DESTROY ACTIVITY")
        viewModel.yubiKey.value = null
        yubikit.stopUsbDiscovery()
        super.onDestroy()
    }
}