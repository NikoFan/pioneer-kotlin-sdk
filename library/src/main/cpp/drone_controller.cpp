#include <jni.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>
#include <vector>
#include <string>
#include "mavlink/common/mavlink.h"

// Логгирование
#include <android/log.h>
#define LOG_TAG "MAVLINK_NATIVE"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Глобальные переменные для UDP-соединения
static int sockfd = -1;
static struct sockaddr_in dest_addr;

// Вектор для хранения логов (максимум 100 сообщений)
static std::vector<std::string> flight_logs;
const size_t MAX_LOGS = 100;

/**
 * Вспомогательная функция: добавить сообщение в лог
 */
void log_message(const char* msg) {
    if (flight_logs.size() >= MAX_LOGS) {
        flight_logs.erase(flight_logs.begin()); // Удаляем старое
    }
    flight_logs.push_back(std::string(msg));
}



/**
 * JNI: инициализация соединения с дроном
 * Вызывается из Kotlin: initNative(ip, port)
 */
extern "C" JNIEXPORT void JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_initNative(
        JNIEnv *env, jobject thiz, jstring ip, jint port) {
    const char *ip_str = env->GetStringUTFChars(ip, nullptr);

    // Создание UDP-сокет
    sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    dest_addr.sin_family = AF_INET;
    dest_addr.sin_port = htons(port);
    inet_pton(AF_INET, ip_str, &dest_addr.sin_addr);

    env->ReleaseStringUTFChars(ip, ip_str);
    log_message("UDP Соединение установлено");
}

/**
 * Вспомогательная функция: отправка команды COMMAND_LONG
 */
bool send_command_long(uint8_t target_system, uint8_t target_component,
                       uint16_t command, float param1, float param2) {
    if (sockfd == -1) {
        LOGE("Сокет не инициализирован");
        return false;
    }

    mavlink_message_t msg;
    uint8_t buf[MAVLINK_MAX_PACKET_LEN];

    // Формирование команды
    mavlink_msg_command_long_pack(
            255, 0,                 // GCS system/component ID
            &msg,
            target_system,
            target_component,
            command,
            0,                      // confirmation
            param1, param2, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
    );

    // Конвертируем в байты и отправляем
    uint16_t len = mavlink_msg_to_send_buffer(buf, &msg);

    // Логируем HEX
    char hex_str[1024];
    char* p = hex_str;
    for (int i = 0; i < len; i++) {
        p += sprintf(p, "%02X ", buf[i]);
    }
    LOGD("Отправка команды %d: %s", command, hex_str);


    ssize_t sent = sendto(sockfd, buf, len, 0, (struct sockaddr*)&dest_addr, sizeof(dest_addr));
    if (sent != len) {
        LOGE("Ошибка отправки: sent=%zd, expected=%d", sent, len);
        return false;
    }
    LOGD("Команда %d отправлена успешно", command);
    return true;
}

/**
 * JNI: запуск двигателей (ARM)
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_arm(
        JNIEnv *env, jobject thiz) {
    bool success = send_command_long(0, 0, MAV_CMD_COMPONENT_ARM_DISARM, 1.0f, 0.0f);
    log_message(success ? "ARM: отправлено" : "ARM: ошибка");
    return success ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI: остановка двигателей (DISARM)
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_disarm(
        JNIEnv *env, jobject thiz) {
    bool success = send_command_long(0, 0, MAV_CMD_COMPONENT_ARM_DISARM, 0.0f, 0.0f);
    log_message(success ? "DISARM: отправлено" : "DISARM: ошибка");
    return success ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI: взлёт (TAKEOFF)
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_takeoff(
        JNIEnv *env, jobject thiz) {
    bool success = send_command_long(1, 1, MAV_CMD_NAV_TAKEOFF, 0.0f, 0.0f);
    log_message(success ? "TAKEOFF: отправлено" : "TAKEOFF: ошибка");
    return success ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI: посадка (LAND)
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_land(
        JNIEnv *env, jobject thiz) {
    bool success = send_command_long(1, 1, MAV_CMD_NAV_LAND, 0.0f, 0.0f);
    log_message(success ? "LAND: отправлено" : "LAND: ошибка");
    return success ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI: получить все логи полёта
 * Возвращает массив строк (jobjectArray)
 */
extern "C" JNIEXPORT jobjectArray JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_getFlightLogs(
        JNIEnv *env, jobject thiz) {
    // Создаём массив Java-строк
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray logs = env->NewObjectArray(flight_logs.size(), stringClass, nullptr);

    for (size_t i = 0; i < flight_logs.size(); ++i) {
        env->SetObjectArrayElement(logs, i, env->NewStringUTF(flight_logs[i].c_str()));
    }

    return logs;
}

/**
 * JNI: очистить логи
 */
extern "C" JNIEXPORT void JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_clearLogs(
        JNIEnv *env, jobject thiz) {
    flight_logs.clear();
}