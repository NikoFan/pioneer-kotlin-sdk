/**
 * Pioneer Kotlin SDK — Native MAVLink Controller
 *
 * Отвечает за:
 * - Управление UDP-сокетом для связи с дроном
 * - Отправку MAVLink-команд (ARM, DISARM, TAKEOFF, LAND, LED)
 * - Фоновую отправку HEARTBEAT (критично для GEOSCAN Pioneer)
 *
 * Архитектура:
 * - Все JNI-методы имеют префикс `Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_*`
 * - Сетевые операции выполняются в отдельных потоках, чтобы не блокировать UI
 * - Логирование через Android Logcat с тегом "PioneerKotlinSDK"
 */

#include <jni.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>           // для close()
#include <string.h>
#include <thread>
#include <chrono>

// MAVLink C-библиотека
#include "mavlink/common/mavlink.h"

// ============================================================================
// КОНСТАНТЫ И МАКРОСЫ
// ============================================================================

// Кастомная команда для управления светодиодами (стандарт MAVLink)
// Источник: https://mavlink.io/en/messages/common.html#MAV_CMD_USER_1
#ifndef MAV_CMD_USER_1
#define MAV_CMD_USER_1 31010
#endif

// Коди результатов MAVLink-команд (для удобства чтения)
// Полная спецификация: https://mavlink.io/en/messages/common.html#MAV_RESULT
#define MAV_RESULT_ACCEPTED              0
#define MAV_RESULT_TEMPORARILY_REJECTED  1
#define MAV_RESULT_DENIED                2
#define MAV_RESULT_UNSUPPORTED           3
#define MAV_RESULT_FAILED                4
#define MAV_RESULT_IN_PROGRESS           5
#define MAV_RESULT_CANCELLED             6

// ============================================================================
// ЛОГИРОВАНИЕ (Android NDK)
// ============================================================================

#include <android/log.h>
#define LOG_TAG "PioneerKotlinSDK"

// Макросы для разных уровней логирования (отключаются в релизе через #ifdef)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================================
// ГЛОБАЛЬНОЕ СОСТОЯНИЕ СОЕДИНЕНИЯ
// ============================================================================

// UDP-сокет и адрес назначения (дрон)
// ⚠️ Глобальные переменные упрощают JNI, но требуют осторожности при многопоточности
static int sockfd = -1;
static struct sockaddr_in dest_addr;

// Флаг для управления фоновым потоком HEARTBEAT
static volatile bool heartbeat_running = false;


// ============================================================================
// ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
// ============================================================================

/**
 * Отправка MAVLink HEARTBEAT-пакета
 *
 * Критично для GEOSCAN Pioneer: дрон игнорирует команды, если не получает
 * регулярные HEARTBEAT от GCS (Ground Control Station).
 *
 * @see https://mavlink.io/en/messages/common.html#HEARTBEAT
 */
static void send_heartbeat() {
    if (sockfd == -1) return;

    mavlink_message_t msg;
    uint8_t buf[MAVLINK_MAX_PACKET_LEN];

    // Формируем HEARTBEAT от GCS (тип 6 = MAV_TYPE_GCS)
    mavlink_msg_heartbeat_pack(
            255,                    // system_id: 255 = "любой" (для GCS)
            0,                      // component_id: 0 = "не указан"
            &msg,
            MAV_TYPE_GCS,           // тип отправителя: наземная станция
            MAV_AUTOPILOT_INVALID,  // автопилот: не используется
            0, 0, 0                 // base_mode, custom_mode, system_status
    );

    // Сериализуем сообщение в байтовый буфер
    uint16_t len = mavlink_msg_to_send_buffer(buf, &msg);

    // Отправляем через UDP
    sendto(sockfd, buf, len, 0, (struct sockaddr*)&dest_addr, sizeof(dest_addr));

    LOGD("HEARTBEAT sent (%d bytes)", len);
}

/**
 * Фоновый поток: периодическая отправка HEARTBEAT
 *
 * Запускается в initNative(), останавливается в closeNative().
 * Интервал: 1 секунда (рекомендация MAVLink: 1-2 Гц).
 */
static void heartbeat_loop() {
    while (heartbeat_running) {
        send_heartbeat();
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
}

/**
 * Универсальная отправка MAV_CMD_* команды с 2 параметрами
 *
 * Используется для: ARM, DISARM, TAKEOFF, LAND.
 *
 * @param target_system   ID целевой системы (0 = broadcast, 1 = autopilot)
 * @param target_component ID целевого компонента (0 = broadcast, 1 = autopilot)
 * @param command         MAVLink-код команды (например, MAV_CMD_COMPONENT_ARM_DISARM)
 * @param param1, param2  Первые два параметра команды (остальные заполняются 0)
 * @return true если пакет успешно отправлен в сокет (не гарантирует выполнение!)
 */
static bool send_command_long(uint8_t target_system, uint8_t target_component,
                              uint16_t command, float param1, float param2) {
    if (sockfd == -1) {
        LOGE("Socket not initialized");
        return false;
    }

    mavlink_message_t msg;
    uint8_t buf[MAVLINK_MAX_PACKET_LEN];

    // Формируем COMMAND_LONG через официальную MAVLink-функцию
    mavlink_msg_command_long_pack(
            255, 0, &msg,                    // GCS system/component ID
            target_system, target_component, // получатель
            command, 0,                      // команда + confirmation flag
            param1, param2, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f // param1..param7
    );

    uint16_t len = mavlink_msg_to_send_buffer(buf, &msg);
    ssize_t sent = sendto(sockfd, buf, len, 0,
                          (struct sockaddr*)&dest_addr, sizeof(dest_addr));

    if (sent != len) {
        LOGE("Send failed: %zd/%d bytes", sent, len);
        return false;
    }

    LOGD("Command %d sent to sys=%d/comp=%d", command, target_system, target_component);
    return true;
}

/**
 * Универсальная отправка MAV_CMD_* команды с 4 параметрами
 *
 * Используется для: LED control (MAV_CMD_USER_1), где нужно передать
 * ID светодиода + RGB-значения.
 *
 * @param param1..param4  Первые четыре параметра команды (остальные = 0)
 * @see send_command_long для деталей
 */
static bool send_command_long_4params(uint8_t target_system, uint8_t target_component,
                                      uint16_t command,
                                      float param1, float param2,
                                      float param3, float param4) {
    if (sockfd == -1) {
        LOGE("Socket not initialized");
        return false;
    }

    mavlink_message_t msg;
    uint8_t buf[MAVLINK_MAX_PACKET_LEN];

    mavlink_msg_command_long_pack(
            255, 0, &msg,
            target_system, target_component,
            command, 0,
            param1, param2, param3, param4, 0.0f, 0.0f, 0.0f
    );

    uint16_t len = mavlink_msg_to_send_buffer(buf, &msg);
    ssize_t sent = sendto(sockfd, buf, len, 0,
                          (struct sockaddr*)&dest_addr, sizeof(dest_addr));

    if (sent != len) {
        LOGE("Send failed: %zd/%d bytes", sent, len);
        return false;
    }

    LOGD("Command %d (4 params) sent", command);
    return true;
}

// ============================================================================
// JNI-МЕТОДЫ: ИНИЦИАЛИЗАЦИЯ И УПРАВЛЕНИЕ СОЕДИНЕНИЕМ
// ============================================================================

/**
 * Инициализация UDP-соединения с дроном
 *
 * Вызывается из Kotlin при создании MavlinkConnectionNdk.
 *
 * @param ip   IP-адрес дрона (обычно "192.168.4.1" для Pioneer)
 * @param port UDP-порт MAVLink (обычно 8001)
 */
extern "C" JNIEXPORT void JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_initNative(
        JNIEnv *env, jobject thiz, jstring ip, jint port) {

    const char *ip_str = env->GetStringUTFChars(ip, nullptr);

    // Создаём UDP-сокет
    sockfd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (sockfd == -1) {
        LOGE("Failed to create socket");
        env->ReleaseStringUTFChars(ip, ip_str);
        return;
    }

    // Настраиваем адрес назначения
    memset(&dest_addr, 0, sizeof(dest_addr));
    dest_addr.sin_family = AF_INET;
    dest_addr.sin_port = htons(port);
    inet_pton(AF_INET, ip_str, &dest_addr.sin_addr);

    env->ReleaseStringUTFChars(ip, ip_str);
    LOGI("Connected to %s:%d", ip_str, port);

    // Запускаем фоновый поток HEARTBEAT
    heartbeat_running = true;
    std::thread(heartbeat_loop).detach();  // detach: поток живёт до завершения процесса
}

/**
 * Закрытие соединения и очистка ресурсов
 *
 * Обязательно вызывать при завершении работы с дроном.
 */
extern "C" JNIEXPORT void JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_closeNative(
        JNIEnv *env, jobject thiz) {

    heartbeat_running = false;  // Останавливаем HEARTBEAT-поток

    if (sockfd != -1) {
        close(sockfd);  // Закрываем сокет
        sockfd = -1;
        LOGI("Socket closed");
    }
}

// ============================================================================
// JNI-МЕТОДЫ: УПРАВЛЕНИЕ СВЕТОДИОДАМИ (КАСТОМНАЯ ФУНКЦИЯ)
// ============================================================================

/**
 * Управление светодиодами дрона (простой режим, без ожидания ответа)
 *
 * Использует кастомную команду MAV_CMD_USER_1 (31010).
 *
 * Параметры:
 * - led_id: 0..3 = конкретный светодиод, 255 = все светодиоды
 * - r, g, b: интенсивность цвета (0..255)
 *
 * @return true если пакет успешно отправлен (не гарантирует, что дрон выполнил команду)
 * @see https://mavlink.io/en/messages/common.html#MAV_CMD_USER_1
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_ledControl(
        JNIEnv *env, jobject thiz,
        jint led_id, jint r, jint g, jint b) {

    // Валидация входных параметров (защита от некорректных данных)
    if (led_id != 255 && (led_id < 0 || led_id > 3)) {
        LOGW("Invalid led_id: %d (allowed: 0-3 or 255)", led_id);
        return JNI_FALSE;
    }
    if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
        LOGW("Invalid RGB: (%d,%d,%d) — values must be 0..255", r, g, b);
        return JNI_FALSE;
    }

    // Отправляем команду на Autopilot (system=1, component=1)
    bool success = send_command_long_4params(
            1, 1,                           // target: autopilot
            MAV_CMD_USER_1,                 // команда: пользовательская #1
            (float)led_id,                  // param1: ID светодиода
            (float)r, (float)g, (float)b    // param2-4: RGB-цвета
    );

    LOGI("LED control: %s (id=%d, rgb=%d,%d,%d)",
         success ? "sent" : "failed", led_id, r, g, b);
    return success ? JNI_TRUE : JNI_FALSE;
}

// ============================================================================
// JNI-МЕТОДЫ: УПРАВЛЕНИЕ ДРОНОМ (БАЗОВЫЕ КОМАНДЫ)
// ============================================================================

/**
 * Запуск двигателей (ARM)
 *
 * ⚠️ Требует: дрон на земле, все проверки пройдены.
 * @return true если команда отправлена успешно
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_arm(
        JNIEnv *env, jobject thiz) {

    // MAV_CMD_COMPONENT_ARM_DISARM: param1=1.0 = ARM, param2=0 = без принудительного обхода
    bool success = send_command_long(0, 0, MAV_CMD_COMPONENT_ARM_DISARM, 1.0f, 0.0f);
    LOGI("ARM: %s", success ? "sent" : "failed");
    return success ? JNI_TRUE : JNI_FALSE;
}

/**
 * Отключение двигателей (DISARM)
 *
 * ⚠️ Требует: дрон на земле или в безопасном состоянии.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_disarm(
        JNIEnv *env, jobject thiz) {

    bool success = send_command_long(0, 0, MAV_CMD_COMPONENT_ARM_DISARM, 0.0f, 0.0f);
    LOGI("DISARM: %s", success ? "sent" : "failed");
    return success ? JNI_TRUE : JNI_FALSE;
}

/**
 * Взлёт (TAKEOFF)
 *
 * ⚠️ Требует: дрон заармлен, режим полёта активен.
 * Примечание: для навигационных команд используем target=(1,1)
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_takeoff(
        JNIEnv *env, jobject thiz) {

    // MAV_CMD_NAV_TAKEOFF: param1=мин. угол тангажа, param2=не используется
    bool success = send_command_long(1, 1, MAV_CMD_NAV_TAKEOFF, 0.0f, 0.0f);
    LOGI("TAKEOFF: %s", success ? "sent" : "failed");
    return success ? JNI_TRUE : JNI_FALSE;
}

/**
 * Посадка (LAND)
 *
 * ⚠️ Дрон выполнит посадку в текущей точке или по заданному маршруту.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_land(
        JNIEnv *env, jobject thiz) {

    bool success = send_command_long(1, 1, MAV_CMD_NAV_LAND, 0.0f, 0.0f);
    LOGI("LAND: %s", success ? "sent" : "failed");
    return success ? JNI_TRUE : JNI_FALSE;
}
