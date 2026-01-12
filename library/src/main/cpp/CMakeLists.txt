# Минимальная версия CMake
cmake_minimum_required(VERSION 3.22.1)

# Название проекта (совпадает с именем .so файла)
project("drone_controller")

# Добавление папки mavlink как include-директорию
include_directories(mavlink)

# Создание нативной библиотеки SHARED (.so)
# Имя "drone_controller" должно совпадать с System.loadLibrary("drone_controller") в Kotlin
add_library(
        drone_controller
        SHARED
        drone_controller.cpp
)

# Подключение системных библиотек Android: log и android
find_library(log-lib log)
find_library(android-lib android)

# Линкуем зависимости
target_link_libraries(
        drone_controller
        ${log-lib}
        ${android-lib}
)