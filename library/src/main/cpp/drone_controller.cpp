#include <jni.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>
#include "mavlink/common/mavlink.h"

static int sockfd = -1;
static struct sockaddr_in dest_addr;

extern "C" JNIEXPORT void JNICALL
Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_initNative(
        JNIEnv *env, jobject thiz, jstring ip, jint port) {
const char *ip_str = env->GetStringUTFChars(ip, nullptr);

sockfd = socket(AF_INET, SOCK_DGRAM, 0);
dest_addr.sin_family = AF_INET;
dest_addr.sin_port = htons(port);
inet_pton(AF_INET, ip_str, &dest_addr.sin_addr);

env->ReleaseStringUTFChars(ip, ip_str);
}

extern "C" JNIEXPORT jboolean JNICALL
        Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_arm(
        JNIEnv *env, jobject thiz) {
if (sockfd == -1) return JNI_FALSE;

mavlink_message_t msg;
uint8_t buf[MAVLINK_MAX_PACKET_LEN];

// ARM: param1=1.0, target_system=1, target_component=0
mavlink_msg_command_long_pack(
255, 0,                 // GCS system/component
&msg,
1, 0,                   // target_system=1, target_component=0
MAV_CMD_COMPONENT_ARM_DISARM,
0,                      // confirmation
1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
);

uint16_t len = mavlink_msg_to_send_buffer(buf, &msg);
sendto(sockfd, buf, len, 0, (struct sockaddr*)&dest_addr, sizeof(dest_addr));
return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
        Java_io_github_NikoFan_pioneer_internal_MavlinkConnectionNdk_disarm(
        JNIEnv *env, jobject thiz) {
if (sockfd == -1) return JNI_FALSE;

mavlink_message_t msg;
uint8_t buf[MAVLINK_MAX_PACKET_LEN];

// DISARM: param1=0.0, target_system=1, target_component=0
mavlink_msg_command_long_pack(
255, 0,
&msg,
1, 0,
MAV_CMD_COMPONENT_ARM_DISARM,
0,
0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
);

uint16_t len = mavlink_msg_to_send_buffer(buf, &msg);
sendto(sockfd, buf, len, 0, (struct sockaddr*)&dest_addr, sizeof(dest_addr));
return JNI_TRUE;
}