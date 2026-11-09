package com.mmcromero.promptflutuante

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
                checkOverlayPermissionAndStart()
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

        val btnAccessibility = Button(this).apply {
            text = "3. Ativar Detecção de Apps (Acessibilidade)"
            setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(context, "Ative 'Prompt Flutuante' na lista de Acessibilidade", Toast.LENGTH_LONG).show()
            }
        }

        layout.addView(btnStart)
        layout.addView(btnStop)
        layout.addView(btnAccessibility)
        setContentView(layout)
    }

    private fun checkOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 1234)
        } else {
            startFloatingService()
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingService::class.java)
        startService(intent)
        Toast.makeText(this, "Serviço flutuante ativado!", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingService()
            } else {
                Toast.makeText(this, "Permissão negada para sobrepor apps", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
