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

    // Regra Geral Universal (Prompt Mestre)
    private val generalRules = "Regras Gerais Permanentes: Mantenha as respostas em português, objetivas e estruturadas."

    // Estrutura de dados com par de prompts (Ativação vs Reset)
    data class PromptItem(
        val title: String,
        val onPrompt: String,
        val offPrompt: String,
        var isEnabled: Boolean = true
    )

    private val promptList = mutableListOf(
        PromptItem(
            title = "Modo Celular",
            onPrompt = "SOBREPOR TUDO - Modo Celular ATIVADO: Responda de forma extremamente curta, usando tópicos rápidos e frases diretas.",
            offPrompt = "CANCELAR Modo Celular: Encerre o modo de respostas curtas e retorne ao comportamento padrão."
        ),
        PromptItem(
            title = "Análise de Código",
            onPrompt = "SOBREPOR TUDO - Modo Code Review ATIVADO: Foque exclusivamente na identificação de bugs, performance e boas práticas do código a seguir.",
            offPrompt = "CANCELAR Modo Code Review: Desative a análise estrita de código e volte ao tom de conversa normal."
        ),
        PromptItem(
            title = "Revisar Texto",
            onPrompt = "SOBREPOR TUDO - Modo Revisor ATIVADO: Reescreva o texto a seguir corrigindo gramática e pontuação, mantendo o tom natural.",
            offPrompt = "CANCELAR Modo Revisor: Encerre a revisão de texto e retorne ao comportamento padrão."
        )
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

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                500
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

            val copyBtn = Button(this).apply {
                textSize = 11f
            }

            fun updateButtonState() {
                if (prompt.isEnabled) {
                    copyBtn.text = "Ativar"
                    copyBtn.setBackgroundColor(Color.parseColor("#2E7D32"))
                } else {
                    copyBtn.text = "Resetar"
                    copyBtn.setBackgroundColor(Color.parseColor("#C62828"))
                }
            }

            val switchBtn = Switch(this).apply {
                isChecked = prompt.isEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    prompt.isEnabled = isChecked
                    updateButtonState()
                }
            }

            updateButtonState()

            copyBtn.setOnClickListener {
                val finalPrompt = if (prompt.isEnabled) {
                    prompt.onPrompt
                } else {
                    "${prompt.offPrompt}\n\n$generalRules"
                }
                copyToClipboard(finalPrompt, if (prompt.isEnabled) "Modo ativado copiado!" else "Instrução de reset copiada!")
                toggleMenuVisibility()
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
            680, WindowManager.LayoutParams.WRAP_CONTENT,
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

    private fun copyToClipboard(text: String, toastMessage: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Prompt", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
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
