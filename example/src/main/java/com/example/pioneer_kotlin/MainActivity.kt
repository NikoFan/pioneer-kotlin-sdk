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

        pioneer = Pioneer(ip = "192.168.4.1", mavlinkPort = 8001, logger = true)

        // Запускаем в фоновом потоке!
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d("PioneerApp", "Подключён к дрону: ${pioneer.isConnected()}")
            Log.d("PioneerApp", "✅ Начинаем тест: ЗАПУСК двигателей (ARM)...")

            val armSuccess = pioneer.arm()
            Log.d("PioneerApp", "Результат ARM: $armSuccess")

            if (armSuccess) {
                Log.d("PioneerApp", "✅ Двигатели ЗАПУЩЕНЫ (ARM). Ждём 3 секунды...")
                delay(5000)



//                Log.d("PioneerApp", "🛑 Выполняем DISARM (остановка двигателей)...")
//                val disarmSuccess = pioneer.disarm()

                Log.d("PioneerApp", "🛬 Выполняем посадку (LAND)...")
                val landSuccess = pioneer.land() // ← ИСПОЛЬЗУЙ LAND ВМЕСТО DISARM
                Log.d("PioneerApp", "Результат LAND: $landSuccess")

                if (landSuccess) {
                    Log.d("PioneerApp", "✅ Посадка Произведена. Двигатели остановлены. Тест завершён успешно.")
                } else {
                    Log.e("PioneerApp", "⚠️ Ошибка при LAND!")
                }
            } else {
                Log.e("PioneerApp", "❌ Не удалось выполнить ARM!")
            }
        }
    }

    override fun onDestroy() {
        pioneer.close()
        super.onDestroy()
    }
}