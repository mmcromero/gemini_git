package com.mmcromero.promptflutuante

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var menuView: View
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var menuParams: WindowManager.LayoutParams

    // Estrutura de dados dos prompts
    data class PromptItem(val title: String, val text: String, var isEnabled: Boolean = true)

    private val promptList = mutableListOf(
        PromptItem("Modo Celular", "Modo Celular: Responda de forma objetiva, usando frases curtas e apresentando sugestões de próximos passos."),
        PromptItem("Análise de Código", "Analise o seguinte trecho de código, identifique possíveis falhas e sugira a melhoria mantendo o estilo original:"),
        PromptItem("Revisar & Corrigir", "Por favor, revise e corrija a gramática e a clareza do texto a seguir mantendo o tom natural:")
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        setupBubbleView()
        setupMenuView()
    }

    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun setupBubbleView() {
        // Criando a bolinha flutuante circular
        val button = Button(this).apply {
            text = "⚡"
            textSize = 18f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#6200EE"))
            }
        }

        bubbleParams = WindowManager.LayoutParams(
            140, 140,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        // Lógica para diferenciar Arraste de Clique
        button.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = bubbleParams.x
                        initialY = bubbleParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        bubbleParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        bubbleParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = abs(event.rawX - initialTouchX)
                        val diffY = abs(event.rawY - initialTouchY)
                        // Se moveu menos de 10 pixels, é um CLIQUE
                        if (diffX < 10 && diffY < 10) {
                            toggleMenuVisibility()
                        }
                        return true
                    }
                }
                return false
            }
        })

        bubbleView = button
        windowManager.addView(bubbleView, bubbleParams)
    }

    private fun setupMenuView() {
        // Layout principal do menu expandido
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(Color.parseColor("#1E1E2E"))
                setStroke(2, Color.parseColor("#444466"))
            }
        }

        // Cabeçalho (Título + Fechar)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "⚡ Prompts IA"
            textSize = 16f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = Button(this).apply {
            text = "✕"
            setTextColor(Color.GRAY)
            textSize = 14f
            background = null
            setOnClickListener { toggleMenuVisibility() }
        }

        header.addView(title)
        header.addView(closeBtn)
        mainLayout.addView(header)

        // Lista rolável de prompts
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                450
            )
        }

        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        promptList.forEach { prompt ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 16, 0, 16)
            }

            val promptTitle = TextView(this).apply {
                text = prompt.title
                setTextColor(Color.WHITE)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val switchBtn = Switch(this).apply {
                isChecked = prompt.isEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    prompt.isEnabled = isChecked
                }
            }

            val copyBtn = Button(this).apply {
                text = "Copiar"
                textSize = 12f
                setOnClickListener {
                    if (prompt.isEnabled) {
                        copyToClipboard(prompt.text)
                        toggleMenuVisibility()
                    } else {
                        Toast.makeText(context, "Prompt desativado!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            row.addView(promptTitle)
            row.addView(switchBtn)
            row.addView(copyBtn)
            listContainer.addView(row)
        }

        scrollView.addView(listContainer)
        mainLayout.addView(scrollView)

        menuView = mainLayout

        menuParams = WindowManager.LayoutParams(
            650, WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }
    }

    private fun toggleMenuVisibility() {
        if (menuView.parent == null) {
            menuParams.x = bubbleParams.x
            menuParams.y = bubbleParams.y + 150
            windowManager.addView(menuView, menuParams)
        } else {
            windowManager.removeView(menuView)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Prompt", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Prompt copiado com sucesso!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bubbleView.isInitialized && bubbleView.parent != null) {
            windowManager.removeView(bubbleView)
        }
        if (::menuView.isInitialized && menuView.parent != null) {
            windowManager.removeView(menuView)
        }
    }
}
