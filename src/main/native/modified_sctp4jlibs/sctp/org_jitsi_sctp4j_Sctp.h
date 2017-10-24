/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_jitsi_sctp4j_Sctp */

#ifndef _Included_org_jitsi_sctp4j_Sctp
#define _Included_org_jitsi_sctp4j_Sctp
#ifdef __cplusplus
extern "C" {
#endif
#undef org_jitsi_sctp4j_Sctp_MSG_NOTIFICATION
#define org_jitsi_sctp4j_Sctp_MSG_NOTIFICATION 8192L
/*
 * Class:     org_jitsi_sctp4j_Sctp
 * Method:    on_network_in
 * Signature: (J[BII)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_sctp4j_Sctp_on_1network_1in
  (JNIEnv *, jclass, jlong, jbyteArray, jint, jint);

/*
 * Class:     org_jitsi_sctp4j_Sctp
 * Method:    usrsctp_accept
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jitsi_sctp4j_Sctp_usrsctp_1accept
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_sctp4j_Sctp
 * Method:    usrsctp_close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_sctp4j_Sctp_usrsctp_1close
  (JNIEnv *, jclass, jlong);
  
/* 
 * Class:     org_jitsi_sctp4j_Sctp
 * Method:    usrsctp_shutdown
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_sctp4j_Sctp_usrsctp_1shutdown
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     org_jitsi_sctp4j_Sctp
 * Method:    usrsctp_connect
 * Signature: (JI)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jitsi_sctp4j_Sctp_usrsctp_1connect
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     org_jitsi_sctp4j_Sctp
 * Method:    usrsctp_finish
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_jitsi_sctp4j_Sctp_usrsctp_1finish
  (JNIEnv *, jclass);

/*
 * Class:     org_jitsi_sctp4j_Sctp
 * Method:    usrsctp_init
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jitsi_sctp4j_Sctp_usrsctp_1init
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_jitsi_sctp4j_Sctp
 * Method:    usrsctp_listen
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_sctp4j_Sctp_usrsctp_1listen
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_sctp4j_Sctp
 * Method:    usrsctp_send
 * Signature: (J[BIIZII)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_sctp4j_Sctp_usrsctp_1send
  (JNIEnv *, jclass, jlong, jbyteArray, jint, jint, jboolean, jint, jint);

/*
 * Class:     org_jitsi_sctp4j_Sctp
 * Method:    usrsctp_socket
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_sctp4j_Sctp_usrsctp_1socket
  (JNIEnv *, jclass, jint);

#ifdef __cplusplus
}
#endif
#endif
