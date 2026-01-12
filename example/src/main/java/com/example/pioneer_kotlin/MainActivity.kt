package com.example.pioneer_example

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.NikoFan.pioneer.Pioneer
import kotlinx.coroutines.Dispatchers  // ← добавь импорт
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    private lateinit var pioneer: Pioneer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pioneer = Pioneer(
            ip = "192.168.4.1",
            mavlinkPort = 8001,
            logger = true
        )

        lifecycleScope.launch(Dispatchers.IO) {
            Log.d("PioneerApp", "Подключён к дрону: ${pioneer.isConnected()}")
            Log.d("PioneerApp", "✅ Начинаем тест: ARM...")

            val armSuccess = pioneer.arm()
            Log.d("PioneerApp", "Результат ARM: $armSuccess")
            Log.d("Logs", pioneer.getLogs().joinToString("\n"))

            if (armSuccess) {
                Log.d("PioneerApp", "✅ Двигатели ЗАПУЩЕНЫ. Ждём 3 сек...")
                delay(3000)

                Log.d("PioneerApp", "🛑 Выполняем DISARM...")
                val disarmSuccess = pioneer.disarm()
                Log.d("Logs", pioneer.getLogs().joinToString("\n"))
                Log.d("PioneerApp", "Результат DISARM: $disarmSuccess")

                if (disarmSuccess) {
                    Log.d("PioneerApp", "✅ Двигатели ОСТАНОВЛЕНЫ.")
                    Log.d("Logs", pioneer.getLogs().joinToString("\n"))
                } else {
                    Log.e("PioneerApp", "❌ Ошибка при DISARM!")
                    Log.d("Logs", pioneer.getLogs().joinToString("\n"))
                }
            } else {
                Log.e("PioneerApp", "❌ Не удалось выполнить ARM!")
                Log.d("Logs", pioneer.getLogs().joinToString("\n"))
            }
        }
    }


    override fun onDestroy() {
        pioneer.close()
        super.onDestroy()
    }

}