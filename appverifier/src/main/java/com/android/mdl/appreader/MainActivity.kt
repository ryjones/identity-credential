package com.android.mdl.appreader

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.mdl.appreader.MainActivity.Const.LOCATION_REQUEST_CODE
import com.android.mdl.appreader.databinding.ActivityMainBinding
import com.android.mdl.appreader.util.logDebug
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.elevation.SurfaceColors
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var mAdapter: NfcAdapter? = null
    private var mPendingIntent: PendingIntent? = null
    private val navController by lazy {
        Navigation.findNavController(this, R.id.nav_host_fragment)
    }
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    object Const {
        const val LOCATION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val color = SurfaceColors.SURFACE_2.getColor(this)
        window.statusBarColor = color
        window.navigationBarColor = color
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupDrawerLayout()

        mAdapter = NfcAdapter.getDefaultAdapter(this)
        // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
        // will fill in the intent with the details of the discovered tag before delivering to
        // this activity.
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ).toTypedArray(),
                LOCATION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { lastKnownLocation ->
            lastKnownLocation?.let{
                VerifierApp.presentationLogStoreInstance.locationLatitude = lastKnownLocation.latitude
                VerifierApp.presentationLogStoreInstance.locationLongitude = lastKnownLocation.longitude
            }
        }
    }

    private fun setupDrawerLayout() {
        binding.nvSideDrawer.setupWithNavController(navController)
        NavigationUI.setupActionBarWithNavController(this, navController, binding.dlMainDrawer)
    }

    override fun onResume() {
        super.onResume()
        mAdapter?.enableForegroundDispatch(this, mPendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        mAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        logDebug("New intent on Activity $intent")
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, binding.dlMainDrawer)
    }

    override fun onBackPressed() {
        if (binding.dlMainDrawer.isDrawerOpen(GravityCompat.START)) {
            binding.dlMainDrawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}