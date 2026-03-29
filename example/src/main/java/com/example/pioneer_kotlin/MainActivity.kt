package com.example.pioneer_example

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.NikoFan.pioneer.LedColor
import io.github.NikoFan.pioneer.LedCommandResult
import io.github.NikoFan.pioneer.Pioneer
import kotlinx.coroutines.Dispatchers  // ← добавь импорт
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.log
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var pioneer: Pioneer
    private val _ledStatus = MutableStateFlow<LedCommandResult>(LedCommandResult.Sent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Обязательно: создаём минимальный UI, чтобы Activity не закрылась
        setContentView(android.widget.TextView(this).apply {
            text = "Testing LED...\nCheck Logcat"
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.BLACK)
        })

        // 2. Инициализация
        pioneer = Pioneer(
            ip = "192.168.4.1",
            mavlinkPort = 8001,
            logger = true
        )

        // 3. Тестовая последовательность
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("LED_TEST", "=== START LED TEST ===")
                // Даём время на установку соединения
                delay(2000)


                // 🔴 Включаем красный на 3 секунды
                Log.d("LED_TEST", "Sending LED RED...")
                val result = pioneer.ledControl(255, LedColor.RED)
                Log.d("LED_TEST", "LED RED send result: $result")

                // ❗ Ждём, пока пакет дойдёт и дрон обработает
                delay(3000)
                pioneer.blinkLeds(
                    color = LedColor.WHITE
                )

                // ⚪ Выключаем
                Log.d("LED_TEST", "Sending LED OFF...")
                pioneer.ledControl(255, LedColor.OFF)

                // Ждём перед дизармом
                delay(500)

                // Показываем результат в UI
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Test done! Check LEDs", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("LED_TEST", "Error: ${e.message}", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    override fun onDestroy() {
        // Добавляем небольшую задержку, чтобы последние пакеты ушли
        lifecycleScope.launch(Dispatchers.IO) {
            delay(200)
            pioneer.close()
            Log.d("LED_TEST", "Connection closed")
        }
        super.onDestroy()
    }

}