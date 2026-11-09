package com.mmcromero.promptflutuante

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private var menuView: View? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var menuParams: WindowManager.LayoutParams

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    private val generalRules = "Regras Gerais Permanentes: Mantenha as respostas em português, objetivas e estruturadas."

    data class PromptItem(
        val title: String,
        val onPrompt: String,
        val offPrompt: String,
        var isEnabled: Boolean = false
    )

    private val promptList = mutableListOf(
        PromptItem(
            title = "Pair Programming",
            onPrompt = "SOBREPOR TUDO - Modo Pair Programming ATIVADO: Atue como meu copiloto e arquiteto de software. Diante de qualquer ideia, recurso ou problema, execute 100% da geração de código e solução técnica até o seu limite como IA, orientando-me com passos diretos apenas naquilo que exigir ação manual minha. Forneça instruções ultra precisas, links diretos em blocos de texto limpo individuais por URL, âncoras exatas de código para substituição e priorize facilidade de cópia e execução.",
            offPrompt = "CANCELAR Modo Pair Programming: Encerre o modo estrito de pair programming e retorne ao comportamento padrão."
        ),
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

        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        setupBubbleView()
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
            alpha = 0.45f
        }

        bubbleParams = WindowManager.LayoutParams(
            130, 130,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - 140
            y = 400
        }

        button.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        button.alpha = 1.0f
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
                        } else {
                            if (bubbleParams.x < screenWidth / 2) {
                                bubbleParams.x = 10
                            } else {
                                bubbleParams.x = screenWidth - 140
                            }
                            windowManager.updateViewLayout(bubbleView, bubbleParams)
                        }

                        if (menuView == null || menuView?.parent == null) {
                            button.alpha = 0.45f
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

    private fun buildMenuView(): View {
        val themedContext = ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
        
        val mainLayout = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(35, 35, 35, 35)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(Color.parseColor("#1E1E2E"))
                setStroke(2, Color.parseColor("#444466"))
            }
        }

        val header = LinearLayout(themedContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(themedContext).apply {
            text = "⚡ Prompts IA"
            textSize = 16f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = Button(themedContext).apply {
            text = "✕"
            setTextColor(Color.GRAY)
            textSize = 14f
            background = null
            setOnClickListener { toggleMenuVisibility() }
        }

        header.addView(title)
        header.addView(closeBtn)
        mainLayout.addView(header)

        val scrollView = ScrollView(themedContext).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(0, 10, 0, 10)
            }
        }

        val listContainer = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
        }

        val thumbColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.parseColor("#00E676"), Color.parseColor("#B0BEC5"))
        )
        val trackColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.parseColor("#1B5E20"), Color.parseColor("#455A64"))
        )

        promptList.forEach { prompt ->
            val row = LinearLayout(themedContext).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 16, 0, 16)
            }

            val promptTitle = TextView(themedContext).apply {
                text = prompt.title
                setTextColor(Color.WHITE)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val switchBtn = SwitchCompat(themedContext).apply {
                isChecked = prompt.isEnabled
                thumbTintList = thumbColors
                trackTintList = trackColors
                setOnClickListener {
                    val newState = isChecked
                    prompt.isEnabled = newState

                    val textToCopy = if (newState) {
                        prompt.onPrompt
                    } else {
                        "${prompt.offPrompt}\n\n$generalRules"
                    }

                    val toastMsg = if (newState) {
                        "${prompt.title} ATIVADO (Copiado!)"
                    } else {
                        "${prompt.title} DESATIVADO (Reset Copiado!)"
                    }

                    copyToClipboard(textToCopy, toastMsg)
                    toggleMenuVisibility()
                }
            }

            row.addView(promptTitle)
            row.addView(switchBtn)
            listContainer.addView(row)
        }

        scrollView.addView(listContainer)
        mainLayout.addView(scrollView)

        val btnTurnOff = Button(themedContext).apply {
            text = "DESATIVAR APP"
            textSize = 12f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f
                setColor(Color.parseColor("#FF3333"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }
            setOnClickListener {
                stopSelf()
            }
        }
        mainLayout.addView(btnTurnOff)

        return mainLayout
    }

    private fun toggleMenuVisibility() {
        if (menuView == null || menuView?.parent == null) {
            menuView = buildMenuView()
            bubbleView.alpha = 1.0f

            val menuWidth = (screenWidth * 0.82).toInt().coerceAtMost(700)
            
            menuParams = WindowManager.LayoutParams(
                menuWidth, WindowManager.LayoutParams.WRAP_CONTENT,
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                alpha = 0.80f  // Adiciona a transparência de 80% pedida
                
                // Calcula a posição do menu baseado em onde a bolinha está na tela
                val bubbleCenterY = bubbleParams.y + (bubbleView.height / 2)
                
                // Exibe o menu sempre alinhado debaixo ou ao lado da bolinha respeitando limites
                x = if (bubbleParams.x < screenWidth / 2) {
                    bubbleParams.x + 140 // Abre para a direita se a bolinha estiver na esquerda
                } else {
                    bubbleParams.x - menuWidth - 20 // Abre para a esquerda se a bolinha estiver na direita
                }.coerceIn(10, screenWidth - menuWidth - 10)
                
                y = (bubbleCenterY - 120).coerceIn(40, screenHeight - 850)
            }

            windowManager.addView(menuView, menuParams)
        } else {
            windowManager.removeView(menuView)
            menuView = null
            bubbleView.alpha = 0.45f
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
        if (menuView != null && menuView?.parent != null) {
            windowManager.removeView(menuView)
            menuView = null
        }
    }
}
