package com.githubvitalyredb.mynextstepskotlinapp

// --- Импорт необходимых библиотек ---
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.util.Calendar
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.util.Log

class MainActivity : AppCompatActivity(), SensorEventListener {

    // --- Константы для ключей SharedPreferences (для надежности) ---
    private val HISTORY_KEY_PREFIX = "dailyHistory_"
    private val LAST_RESULT_KEY = "lastResultKey"
    private val PREVIOUS_RESULT_KEY = "previousResultKey"
    private val TOTAL_DAILY_STEPS_KEY = "totalDailySteps"
    private val LAST_SAVE_DATE_KEY = "lastSaveDate"
    private val DAY_RESET_INITIAL_STEPS_KEY = "dayResetInitialSteps"

    // --- Переменные для датчиков ---
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    // --- Переменные для текущего замера ---
    private var initialSteps = 0f // Начальное значение шагов с сенсора для текущего замера
    private var runningSteps = 0f // Количество шагов, сделанных в текущем замере
    private var startTime: Long = 0L // Время начала текущего замера

    // --- Элементы UI ---
    private lateinit var stepsTextView: TextView
    private lateinit var lastResultTextView: TextView
    private lateinit var previousResultTextView: TextView
    private lateinit var resetButton: Button
    private lateinit var historyButton: Button
    private lateinit var developerImageView: ImageView

    // --- Результаты замеров (сохраняются в SharedPreferences) ---
    private var lastResult = "нет данных"
    private var previousResult = "нет данных"

    // --- Ежедневный счетчик ---
    private lateinit var sharedPreferences: SharedPreferences
    private var totalDailySteps = 0
    private lateinit var dailyStepsTextView: TextView
    private lateinit var currentTimeTextView: TextView
    private var dayResetInitialSteps = 0f // Значение шагов с сенсора в начале дня

    // --- Handler для работы с потоками и временем ---
    private val handler = Handler(Looper.getMainLooper())

    // --- Runnable для обновления времени в реальном времени ---
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateCurrentTime()
            handler.postDelayed(this, 1000)
        }
    }

    // --- Runnable для периодического сохранения данных (каждые 3 минуты) ---
    // Сохраняет только текущие данные, так как история обрабатывается при запуске
    private val periodicSaveRunnable = object : Runnable {
        override fun run() {
            with(sharedPreferences.edit()) {
                putInt(TOTAL_DAILY_STEPS_KEY, totalDailySteps)
                putString(LAST_SAVE_DATE_KEY, SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()))
                apply()
            }
            handler.postDelayed(this, 3 * 60 * 1000)
        }
    }

    // --- Жизненный цикл активности ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация элементов UI
        stepsTextView = findViewById(R.id.stepsTextView)
        lastResultTextView = findViewById(R.id.lastResultTextView)
        previousResultTextView = findViewById(R.id.previousResultTextView)
        resetButton = findViewById(R.id.resetButton)
        historyButton = findViewById(R.id.historyButton)
        dailyStepsTextView = findViewById(R.id.dailyStepsTextView)
        currentTimeTextView = findViewById(R.id.currentTimeTextView)
        developerImageView = findViewById(R.id.developerImageView)

        // Загрузка GIF-анимации с помощью Glide
        Glide.with(this)
            .asGif()
            .load(R.drawable.zoom_rotation_clockwise)
            .into(developerImageView)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("StepCounterPrefs", Context.MODE_PRIVATE)

        // Вызов метода, который загружает все данные и обновляет историю
        loadMemorySafe()

        // Восстановление состояния при повороте экрана
        if (savedInstanceState != null) {
            initialSteps = savedInstanceState.getFloat("initialSteps", 0f)
            runningSteps = savedInstanceState.getFloat("runningSteps", 0f)
            startTime = savedInstanceState.getLong("startTime", 0L)
        }

        // Загрузка сохраненных результатов замеров
        lastResult = sharedPreferences.getString(LAST_RESULT_KEY, "нет данных") ?: "нет данных"
        previousResult = sharedPreferences.getString(PREVIOUS_RESULT_KEY, "нет данных") ?: "нет данных"

        // Обновление UI на основе текущих данных
        stepsTextView.text = "Шагов: ${runningSteps.toInt()}"
        lastResultTextView.text = lastResult
        previousResultTextView.text = previousResult

        // Инициализация сенсора шагов
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Обработчик нажатия на кнопку "Сброс"
        resetButton.setOnClickListener {
            if (runningSteps > 0) {
                val endTime = System.currentTimeMillis()
                val durationMillis = endTime - startTime
                val durationString = formatDuration(durationMillis)
                val newResult = "$durationString - ${runningSteps.toInt()} шагов"

                previousResult = lastResult
                lastResult = newResult

                // Сохранение результатов замеров в SharedPreferences
                with(sharedPreferences.edit()) {
                    putString(LAST_RESULT_KEY, lastResult)
                    putString(PREVIOUS_RESULT_KEY, previousResult)
                    apply()
                }
            }
            initialSteps = 0f
            runningSteps = 0f
            stepsTextView.text = "Шагов: 0"
            startTime = System.currentTimeMillis()
            lastResultTextView.text = lastResult
            previousResultTextView.text = previousResult
        }

        // Обработчик для кнопки "History"
        historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Запуск периодических задач
        handler.post(updateTimeRunnable)
        handler.post(periodicSaveRunnable)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Сохранение данных при повороте экрана, не зависящих от SharedPreferences
        outState.putFloat("initialSteps", initialSteps)
        outState.putFloat("runningSteps", runningSteps)
        outState.putLong("startTime", startTime)
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        handler.post(updateTimeRunnable)
        handler.post(periodicSaveRunnable)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(updateTimeRunnable)
        handler.removeCallbacks(periodicSaveRunnable)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsFromSensor = event.values[0]

            // Инициализация начальных шагов дня при первом получении данных с датчика
            if (dayResetInitialSteps == 0f) {
                dayResetInitialSteps = sharedPreferences.getFloat(DAY_RESET_INITIAL_STEPS_KEY, totalStepsFromSensor)
            }
            if (initialSteps == 0f) {
                initialSteps = totalStepsFromSensor
                startTime = System.currentTimeMillis()
            }

            // Расчет шагов за день
            totalDailySteps = (totalStepsFromSensor - dayResetInitialSteps).toInt()
            if (totalDailySteps < 0) totalDailySteps = 0

            dailyStepsTextView.text = "Шагов за день: $totalDailySteps"

            // Расчет шагов для текущего замера
            val newSteps = totalStepsFromSensor - initialSteps
            if (newSteps >= runningSteps) {
                runningSteps = newSteps
                stepsTextView.text = "Шагов: ${runningSteps.toInt()}"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Не используется, но необходим для интерфейса SensorEventListener
    }

    // --- Вспомогательные функции ---

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun updateCurrentTime() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        currentTimeTextView.text = currentTime
    }

    /**
     * Загружает данные из памяти, создает историю за последние 10 дней,
     * обрабатывает смену дня и очищает старые данные.
     */
    private fun loadMemorySafe() {
        // 1. Считываем все старые данные из SharedPreferences
        val allPrefs = sharedPreferences.all
        // Используем mutableMapOf, чтобы иметь возможность добавлять данные
        val oldHistory = allPrefs.filterKeys { it.startsWith(HISTORY_KEY_PREFIX) }.toMutableMap()
        val oldTotalDailySteps = allPrefs[TOTAL_DAILY_STEPS_KEY] as? Int ?: 0
        val oldLastSaveDateString = allPrefs[LAST_SAVE_DATE_KEY] as? String ?: ""

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val todayDateString = dateFormat.format(Date())

        // 2. ИСПРАВЛЕННАЯ ЛОГИКА: Сначала проверяем смену дня и сохраняем шаги вчерашнего дня в oldHistory
        if (oldLastSaveDateString != todayDateString && oldLastSaveDateString.isNotEmpty()) {
            val historyKey = "$HISTORY_KEY_PREFIX$oldLastSaveDateString"
            oldHistory[historyKey] = oldTotalDailySteps
        }

        // 3. Теперь безопасно очищаем SharedPreferences, так как все нужные данные сохранены
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }

        // 4. Формируем новую историю за последние 10 дней, используя обновленную карту oldHistory
        val calendar = Calendar.getInstance()
        val editor = sharedPreferences.edit()
        for (i in 9 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_MONTH, -i)
            val dayDateString = dateFormat.format(calendar.time)
            val historyKey = "$HISTORY_KEY_PREFIX$dayDateString"
            val steps = oldHistory[historyKey] as? Int ?: 0
            editor.putInt(historyKey, steps)
        }

        // 5. Загружаем и сохраняем остальные данные
        lastResult = allPrefs[LAST_RESULT_KEY] as? String ?: "нет данных"
        previousResult = allPrefs[PREVIOUS_RESULT_KEY] as? String ?: "нет данных"
        val oldDayResetInitialSteps = allPrefs[DAY_RESET_INITIAL_STEPS_KEY] as? Float ?: 0f

        if (oldLastSaveDateString == todayDateString) {
            totalDailySteps = oldTotalDailySteps
            dayResetInitialSteps = oldDayResetInitialSteps
        } else {
            totalDailySteps = 0
            dayResetInitialSteps = 0f
        }

        with(editor) {
            putInt(TOTAL_DAILY_STEPS_KEY, totalDailySteps)
            putFloat(DAY_RESET_INITIAL_STEPS_KEY, dayResetInitialSteps)
            putString(LAST_SAVE_DATE_KEY, todayDateString)
            putString(LAST_RESULT_KEY, lastResult)
            putString(PREVIOUS_RESULT_KEY, previousResult)
            apply()
        }

        dailyStepsTextView.text = "Шагов за день: $totalDailySteps"
    }
}