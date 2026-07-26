#include <jni.h>
#include <android/native_window_jni.h>
#include "Main.h"

JNIEXPORT void JNICALL
Java_com_gles_bluescreen_MainActivity_nativeInit(
        JNIEnv* env, jobject obj, jobject surface) {

    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    Engine_Init(window);
}

JNIEXPORT void JNICALL
Java_com_gles_bluescreen_MainActivity_nativeResize(
        JNIEnv* env, jobject obj, jint w, jint h) {
    Engine_Resize(w, h);
}

JNIEXPORT void JNICALL
Java_com_gles_bluescreen_MainActivity_nativeStep(
        JNIEnv* env, jobject obj) {
    Engine_Draw();
}