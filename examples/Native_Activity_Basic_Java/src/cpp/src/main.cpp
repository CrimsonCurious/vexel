#include <jni.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_nativec_MainActivity_getMessage(
    JNIEnv* env,
    jobject thiz
) {
    return env->NewStringUTF("Hello World");
}
