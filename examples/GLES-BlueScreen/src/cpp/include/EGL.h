#pragma once
#include <EGL/egl.h>
#include <android/native_window.h>

typedef struct {
    EGLDisplay display;
    EGLSurface surface;
    EGLContext context;
} EGLState;

int EGL_Init(EGLState* egl, ANativeWindow* window);
void EGL_Swap(EGLState* egl);
void EGL_Destroy(EGLState* egl);