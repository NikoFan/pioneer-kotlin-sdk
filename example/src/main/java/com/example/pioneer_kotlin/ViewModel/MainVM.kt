package com.example.pioneer_kotlin.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import io.github.NikoFan.pioneer.Pioneer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainVM : ViewModel() {
    // Создание подключения к Pioneer
    private val pioneerInstance: Pioneer = Pioneer(
        ip = "192.168.4.1",
        mavlinkPort = 8001,
        logger = true
    )

    fun onDestroyApp(){
        // Добавляем небольшую задержку, чтобы последние пакеты ушли
        viewModelScope.launch(Dispatchers.IO) {
            delay(200)
            pioneerInstance.close()
            Log.d("LED_TEST", "Connection closed")
        }
    }


}