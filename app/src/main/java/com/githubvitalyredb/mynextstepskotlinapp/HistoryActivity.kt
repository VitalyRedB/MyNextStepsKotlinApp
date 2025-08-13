package com.githubvitalyredb.mynextstepskotlinapp

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val marqueeTextView = findViewById<MarqueeTextView>(R.id.marqueeTextView)
        marqueeTextView.setScrollSpeed(3f) // Чем больше значение — тем быстрее прокрутка

        val exitButton = findViewById<Button>(R.id.exitButton)
        exitButton.setOnClickListener {
            finish()
        }

        val historyContainer = findViewById<LinearLayout>(R.id.historyContainer)
        drawChart(historyContainer)
    }

    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun drawChart(container: LinearLayout) {
        val sharedPreferences = getSharedPreferences("StepCounterPrefs", Context.MODE_PRIVATE)
        val allEntries = sharedPreferences.all
        val historyData = mutableMapOf<String, Int>()

        val MAX_STEPS_FOR_CHART = 20000
        var actualMaxSteps = 0


        for ((key, value) in allEntries) {
            if (key.startsWith("dailyHistory_") && value is Int) {
                val date = key.removePrefix("dailyHistory_")
                historyData[date] = value
                if (value > actualMaxSteps) {
                    actualMaxSteps = value
                }
            }
        }

        // 1. Получаем доступ к SharedPreferences, используя то же имя, что и при сохранении
        //val sharedPreferences = getSharedPreferences("StepCounterPrefs", Context.MODE_PRIVATE)

        // 2. Считываем количество шагов за текущий день.
        // Если данных нет, вернется значение по умолчанию: 0.
        val totalDailySteps = sharedPreferences.getInt("totalDailySteps", 0)

        // 3. Считываем дату последнего сохранения.
        // Если данных нет, вернется пустая строка "".
        val lastSaveDate = sharedPreferences.getString("lastSaveDate", "")
        // Проверяем, что дата существует, и добавляем данные в историю
        if (lastSaveDate != null && lastSaveDate.isNotEmpty()) {
            historyData[lastSaveDate] = totalDailySteps
        }

        // --- ДОБАВЛЕННЫЙ КОД: Вывод содержимого historyData в Logcat ---
        Log.e("HistoryData", "Содержимое historyData: $historyData")
        // --- КОНЕЦ ДОБАВЛЕННОГО КОДА ---

        val maxSteps = if (actualMaxSteps > MAX_STEPS_FOR_CHART) {
            MAX_STEPS_FOR_CHART
        } else {
            actualMaxSteps
        }

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val sortedHistoryList = historyData.toList().sortedByDescending { (dateString, _) ->
            dateFormat.parse(dateString)
        }

        val availableBarWidth = resources.displayMetrics.widthPixels / 3 * 2 - 16.dpToPx() * 3 - 8.dpToPx() * 2

        for ((date, steps) in sortedHistoryList) {
            val entryLayout = LinearLayout(this)
            entryLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
            }
            entryLayout.orientation = LinearLayout.HORIZONTAL
            entryLayout.gravity = Gravity.CENTER_VERTICAL

            val dateTextView = TextView(this)
            dateTextView.text = date
            dateTextView.setTextColor(Color.WHITE)
            dateTextView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            entryLayout.addView(dateTextView)

            val stepsForCalculation = if (steps > MAX_STEPS_FOR_CHART) MAX_STEPS_FOR_CHART else steps
            val barWidth = if (maxSteps > 0) {
                (availableBarWidth * (stepsForCalculation.toFloat() / maxSteps.toFloat())).toInt()
            } else 0

            val barView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, // стартуем с 0 для анимации
                    24.dpToPx()
                ).apply {
                    marginStart = 8.dpToPx()
                    marginEnd = 8.dpToPx()
                }
                setBackgroundResource(R.drawable.rounded_rectangle_orange)
            }
            entryLayout.addView(barView)

            // Запускаем анимацию роста ширины бара
            animateBarWidth(barView, barWidth)

            val stepsTextView = TextView(this)
            stepsTextView.text = "$steps"
            stepsTextView.setTextColor(Color.WHITE)
            stepsTextView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            entryLayout.addView(stepsTextView)

            container.addView(entryLayout)
        }
    }

    private fun animateBarWidth(view: View, targetWidth: Int, duration: Long = 1600) {
        val animator = ValueAnimator.ofInt(0, targetWidth)
        animator.duration = duration
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            val params = view.layoutParams
            params.width = animatedValue
            view.layoutParams = params
        }
        animator.start()
    }
}


