cmake_minimum_required(VERSION 3.22.1)
project("drone_controller")

# путь к заголовкам
include_directories(mavlink)

# Создаём нативную библиотеку
add_library(
        drone_controller
        SHARED
        drone_controller.cpp
)

# Подключаем системные библиотеки
find_library(log-lib log)
find_library(android-lib android)

# Линкуем зависимости
target_link_libraries(
        drone_controller
        ${log-lib}
        ${android-lib}
)