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
            setPadding(50, 100, 50, 50)
        }

        val btnStart = Button(this).apply {
            text = "⚡ Iniciar Prompt Flutuante"
            textSize = 16f
            setOnClickListener {
                checkOverlayPermissionAndStart()
            }
        }

        val btnStop = Button(this).apply {
            text = "✕ Encerrar Serviço"
            textSize = 14f
            setOnClickListener {
                val intent = Intent(context, FloatingService::class.java)
                stopService(intent)
                Toast.makeText(context, "Serviço flutuante encerrado!", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(btnStart)
        layout.addView(btnStop)
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
            checkOverlayPermissionAndStart()
        }
    }
}
