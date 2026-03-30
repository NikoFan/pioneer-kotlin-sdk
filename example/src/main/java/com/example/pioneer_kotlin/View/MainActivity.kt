package com.example.pioneer_kotlin.View

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pioneer_kotlin.ViewModel.MainVM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
// import androidx.

class MainActivity : AppCompatActivity() {
    // private lateinit var pioneer: Pioneer

    // Объявление view model
    private val viewmodelInstance: MainVM = MainVM()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Обязательно: создаём минимальный UI, чтобы Activity не закрылась
        setContentView(TextView(this).apply {
            text = "Testing LED...\nCheck Logcat"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
        })
    }


    // @Composable

//        // 2. Инициализация
//        pioneer = Pioneer(
//            ip = "192.168.4.1",
//            mavlinkPort = 8001,
//            logger = true
//        )
//
//        // 3. Тестовая последовательность
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                Log.d("LED_TEST", "=== START LED TEST ===")
//                // Даём время на установку соединения
//                delay(2000)
//
//
//                // 🔴 Включаем красный на 3 секунды
//                Log.d("LED_TEST", "Sending LED RED...")
//                val result = pioneer.ledControl(255, LedColor.Companion.RED)
//                Log.d("LED_TEST", "LED RED send result: $result")
//
//                // ❗ Ждём, пока пакет дойдёт и дрон обработает
//                delay(3000)
//                pioneer.blinkLeds(
//                    color = LedColor.Companion.WHITE
//                )
//
//                // ⚪ Выключаем
//                Log.d("LED_TEST", "Sending LED OFF...")
//                pioneer.ledControl(255, LedColor.Companion.OFF)
//
//                // Ждём перед дизармом
//                delay(500)
//
//                // Показываем результат в UI
//                launch(Dispatchers.Main) {
//                    Toast.makeText(this@MainActivity, "Test done! Check LEDs", Toast.LENGTH_SHORT).show()
//                }
//
//            } catch (e: Exception) {
//                Log.e("LED_TEST", "Error: ${e.message}", e)
//                launch(Dispatchers.Main) {
//                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
//                }
//            }
//        }



    override fun onDestroy() {

        super.onDestroy()
    }

}