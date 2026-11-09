package com.mmcromero.promptflutuante

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggleService: Button
    private var isServiceRunning = false

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == FloatingWindowService.ACTION_STATE_CHANGED) {
                isServiceRunning = intent.getBooleanExtra(FloatingWindowService.EXTRA_IS_ACTIVE, false)
                atualizarBotaoUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()
        setContentView(R.layout.activity_main)

        btnToggleService = findViewById(R.id.btn_toggle_pair_programming)

        btnToggleService.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                solicitarPermissaoOverlay()
            } else {
                if (isServiceRunning) {
                    pararServico()
                } else {
                    iniciarServico()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(FloatingWindowService.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stateReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(stateReceiver)
    }

    private fun solicitarPermissaoOverlay() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, 1234)
    }

    private fun iniciarServico() {
        val intent = Intent(this, FloatingWindowService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isServiceRunning = true
        atualizarBotaoUI()
    }

    private fun pararServico() {
        val intent = Intent(this, FloatingWindowService::class.java)
        stopService(intent)
        isServiceRunning = false
        atualizarBotaoUI()
    }

    private fun atualizarBotaoUI() {
        btnToggleService.text = if (isServiceRunning) "Desativar Pair Programming" else "Ativar Pair Programming"
    }
}
