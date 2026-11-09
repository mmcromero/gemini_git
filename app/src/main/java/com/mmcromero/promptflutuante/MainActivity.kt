package com.mmcromero.promptflutuante

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val btnStart = Button(this).apply {
            text = "1. Ativar Serviço Flutuante"
            setOnClickListener {
                FloatingService.isMasterEnabled = true
                checkPermissionsAndStart()
            }
        }

        val btnStop = Button(this).apply {
            text = "2. Desativar Serviço Flutuante"
            setOnClickListener {
                FloatingService.isMasterEnabled = false
                val intent = Intent(context, FloatingService::class.java)
                stopService(intent)
                Toast.makeText(context, "Serviço totalmente desativado!", Toast.LENGTH_SHORT).show()
            }
        }

        val btnUsagePermission = Button(this).apply {
            text = "3. Permitir Detecção de Apps (Acesso ao Uso)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                Toast.makeText(context, "Ative o 'Prompt Flutuante' na lista", Toast.LENGTH_LONG).show()
            }
        }

        layout.addView(btnStart)
        layout.addView(btnStop)
        layout.addView(btnUsagePermission)
        setContentView(layout)
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 1234)
        } else if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Por favor, ative a permissão de Acesso ao Uso", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            startFloatingService()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingService::class.java)
        startService(intent)
        Toast.makeText(this, "Serviço flutuante ativado!", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234) {
            checkPermissionsAndStart()
        }
    }
}
