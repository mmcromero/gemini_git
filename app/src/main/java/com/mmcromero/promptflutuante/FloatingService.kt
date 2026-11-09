package com.mmcromero.promptflutuante

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingService : Service() {

    companion object {
        var isMasterEnabled = true
        private const val CHANNEL_ID = "PromptFlutuanteChannel"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var menuView: View
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var menuParams: WindowManager.LayoutParams

    private var currentForegroundPackage: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private val checkAppRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, 800)
        }
    }

    private val aiAppPackages = setOf(
        "com.openai.chatgpt",
        "com.google.android.apps.bard",
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.search",
        "com.anthropic.claude.chat",
        "com.anthropic.claude",
        "com.anthropic.claudebot",
        "com.perplexity.perplexity",
        "com.microsoft.copilot"
    )

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

        startForegroundServiceNotification()
        setupBubbleView()
        setupMenuView()

        handler.post(checkAppRunnable)
    }

    private fun startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Serviço Prompt Flutuante",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Prompt Flutuante")
            .setContentText("Monitorando aplicativos de IA em segundo plano")
            .setSmallIcon(android.R.drawable.btn_star)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun checkForegroundApp() {
        if (!isMasterEnabled) {
            if (::bubbleView.isInitialized) bubbleView.visibility = View.GONE
            if (::menuView.isInitialized && menuView.parent != null) windowManager.removeView(menuView)
            return
        }

        val detectedPackage = getForegroundPackage()
        if (aiAppPackages.contains(detectedPackage)) {
            if (::bubbleView.isInitialized) bubbleView.visibility = View.VISIBLE
        } else {
            if (::bubbleView.isInitialized) bubbleView.visibility = View.GONE
            if (::menuView.isInitialized && menuView.parent != null) windowManager.removeView(menuView)
        }
    }

    private fun getForegroundPackage(): String {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val events = usm.queryEvents(time - 1000 * 10, time)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundPackage = event.packageName
            }
        }
        return currentForegroundPackage
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
        bubbleView.visibility = View.GONE
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
                550
            )
        }

        val listContainer = LinearLayout(this).apply {
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

            val switchBtn = SwitchCompat(this).apply {
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
            setupMenuView()
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
        handler.removeCallbacks(checkAppRunnable)
        if (::bubbleView.isInitialized && bubbleView.parent != null) {
            windowManager.removeView(bubbleView)
        }
        if (::menuView.isInitialized && menuView.parent != null) {
            windowManager.removeView(menuView)
        }
    }
}
