package com.mmcromero.promptflutuante

import android.app.AlertDialog
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private var menuView: View? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var menuParams: WindowManager.LayoutParams

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private lateinit var prefs: SharedPreferences

    private val generalRules = "Regras Gerais Permanentes: Mantenha as respostas em português, objetivas e estruturadas."

    data class PromptItem(
        val id: String,
        var title: String,
        var onPrompt: String,
        var offPrompt: String,
        var isEnabled: Boolean = false
    )

    private val promptList = mutableListOf<PromptItem>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("PromptsFlutuantesPrefs", Context.MODE_PRIVATE)

        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        loadPrompts()
        setupBubbleView()
    }

    private fun loadPrompts() {
        promptList.clear()
        val jsonString = prefs.getString("prompts_json", null)
        if (jsonString.isNullOrEmpty()) {
            promptList.add(
                PromptItem(
                    id = "1",
                    title = "Pair Programming",
                    onPrompt = "SOBREPOR TUDO - Modo Pair Programming ATIVADO: Atue como meu copiloto e arquiteto de software. Diante de qualquer ideia, recurso ou problema, execute 100% da geração de código e solução técnica até o seu limite como IA, orientando-me com passos diretos apenas naquilo que exigir ação manual minha. Forneça instruções ultra precisas, links diretos em blocos de texto limpo individuais por URL, âncoras exatas de código para substituição e priorize facilidade de cópia e execução.",
                    offPrompt = "CANCELAR Modo Pair Programming: Encerre o modo estrito de pair programming e retorne ao comportamento padrão."
                )
            )
            promptList.add(
                PromptItem(
                    id = "2",
                    title = "Modo Celular",
                    onPrompt = "SOBREPOR TUDO - Modo Celular ATIVADO: Responda de forma extremamente curta, usando tópicos rápidos e frases diretas.",
                    offPrompt = "CANCELAR Modo Celular: Encerre o modo de respostas curtas e retorne ao comportamento padrão."
                )
            )
            promptList.add(
                PromptItem(
                    id = "3",
                    title = "Análise de Código",
                    onPrompt = "SOBREPOR TUDO - Modo Code Review ATIVADO: Foque exclusivamente na identificação de bugs, performance e boas práticas do código a seguir.",
                    offPrompt = "CANCELAR Modo Code Review: Desative a análise estrita de código e volte ao tom de conversa normal."
                )
            )
            promptList.add(
                PromptItem(
                    id = "4",
                    title = "Revisar Texto",
                    onPrompt = "SOBREPOR TUDO - Modo Revisor ATIVADO: Reescreva o texto a seguir corrigindo gramática e pontuação, mantendo o tom natural.",
                    offPrompt = "CANCELAR Modo Revisor: Encerre a revisão de texto e retorne ao comportamento padrão."
                )
            )
            savePrompts()
        } else {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    promptList.add(
                        PromptItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            onPrompt = obj.getString("onPrompt"),
                            offPrompt = obj.getString("offPrompt"),
                            isEnabled = false
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun savePrompts() {
        val array = JSONArray()
        promptList.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("onPrompt", item.onPrompt)
                put("offPrompt", item.offPrompt)
            }
            array.put(obj)
        }
        prefs.edit().putString("prompts_json", array.toString()).apply()
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

                        // Sincroniza a posição da janela se estiver aberta
                        if (menuView != null && menuView?.parent != null) {
                            updateMenuPosition()
                        }
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
                            if (menuView != null && menuView?.parent != null) {
                                updateMenuPosition()
                            }
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

    private fun updateMenuPosition() {
        val menuWidth = (screenWidth * 0.85).toInt().coerceAtMost(720)
        menuParams.x = if (bubbleParams.x < screenWidth / 2) {
            bubbleParams.x
        } else {
            bubbleParams.x + 130 - menuWidth
        }.coerceIn(10, screenWidth - menuWidth - 10)

        menuParams.y = (bubbleParams.y + 140).coerceIn(40, screenHeight - 850)
        if (menuView != null && menuView?.parent != null) {
            windowManager.updateViewLayout(menuView, menuParams)
        }
    }

    private fun buildMenuView(): View {
        val themedContext = ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
        
        val mainLayout = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
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

        val addBtn = Button(themedContext).apply {
            text = "＋"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 20f
            background = null
            setOnClickListener { showAddEditDialog(null) }
        }

        header.addView(title)
        header.addView(addBtn)
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
                setPadding(0, 12, 0, 12)
            }

            val promptTitle = TextView(themedContext).apply {
                text = prompt.title
                setTextColor(Color.WHITE)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { showAddEditDialog(prompt) }
            }

            val deleteBtn = Button(themedContext).apply {
                text = "🗑"
                textSize = 12f
                setTextColor(Color.parseColor("#FF5252"))
                background = null
                setOnClickListener { confirmDeletePrompt(prompt) }
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
            row.addView(deleteBtn)
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
                setMargins(0, 15, 0, 0)
            }
            setOnClickListener {
                stopSelf()
            }
        }
        mainLayout.addView(btnTurnOff)

        return mainLayout
    }

    private fun showAddEditDialog(prompt: PromptItem?) {
        val themedContext = ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog)
        val layout = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val editTitle = EditText(themedContext).apply {
            hint = "Título do Prompt"
            setText(prompt?.title ?: "")
        }

        val editOn = EditText(themedContext).apply {
            hint = "Prompt de Ativação (ON)"
            setText(prompt?.onPrompt ?: "")
        }

        val editOff = EditText(themedContext).apply {
            hint = "Prompt de Desativação (OFF)"
            setText(prompt?.offPrompt ?: "")
        }

        layout.addView(editTitle)
        layout.addView(editOn)
        layout.addView(editOff)

        val dialogBuilder = AlertDialog.Builder(themedContext)
            .setTitle(if (prompt == null) "Novo Prompt" else "Editar Prompt")
            .setView(layout)
            .setPositiveButton("Salvar") { _, _ ->
                val newTitle = editTitle.text.toString()
                val newOn = editOn.text.toString()
                val newOff = editOff.text.toString()

                if (newTitle.isNotEmpty()) {
                    if (prompt == null) {
                        promptList.add(
                            PromptItem(
                                id = System.currentTimeMillis().toString(),
                                title = newTitle,
                                onPrompt = newOn,
                                offPrompt = newOff
                            )
                        )
                    } else {
                        prompt.title = newTitle
                        prompt.onPrompt = newOn
                        prompt.offPrompt = newOff
                    }
                    savePrompts()
                    if (menuView != null && menuView?.parent != null) {
                        windowManager.removeView(menuView)
                        menuView = buildMenuView()
                        windowManager.addView(menuView, menuParams)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)

        val dialog = dialogBuilder.create()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            @Suppress("DEPRECATION")
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_PHONE)
        }
        dialog.show()
    }

    private fun confirmDeletePrompt(prompt: PromptItem) {
        val themedContext = ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog)
        val dialog = AlertDialog.Builder(themedContext)
            .setTitle("Excluir Prompt")
            .setMessage("Tem certeza que deseja excluir '${prompt.title}'?")
            .setPositiveButton("Excluir") { _, _ ->
                promptList.remove(prompt)
                savePrompts()
                if (menuView != null && menuView?.parent != null) {
                    windowManager.removeView(menuView)
                    menuView = buildMenuView()
                    windowManager.addView(menuView, menuParams)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            @Suppress("DEPRECATION")
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_PHONE)
        }
        dialog.show()
    }

    private fun toggleMenuVisibility() {
        if (menuView == null || menuView?.parent == null) {
            menuView = buildMenuView()
            bubbleView.alpha = 1.0f

            val menuWidth = (screenWidth * 0.85).toInt().coerceAtMost(720)
            
            menuParams = WindowManager.LayoutParams(
                menuWidth, WindowManager.LayoutParams.WRAP_CONTENT,
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                alpha = 0.80f
            }

            updateMenuPosition()
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
