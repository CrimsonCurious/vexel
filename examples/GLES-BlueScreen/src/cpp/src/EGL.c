#include "EGL.h"
#include <android/native_window.h>

int EGL_Init(EGLState* e, ANativeWindow* window) {
    const EGLint configAttribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_RED_SIZE,   8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE,  8,
        EGL_ALPHA_SIZE, 8,
        EGL_NONE
    };

    const EGLint ctxAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };

    EGLint numConfigs;
    EGLConfig config;
    EGLint format;

    e->display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (e->display == EGL_NO_DISPLAY) return 0;

    if (!eglInitialize(e->display, NULL, NULL)) return 0;

    eglBindAPI(EGL_OPENGL_ES_API);

    if (!eglChooseConfig(e->display, configAttribs, &config, 1, &numConfigs))
        return 0;

    eglGetConfigAttrib(e->display, config, EGL_NATIVE_VISUAL_ID, &format);
    ANativeWindow_setBuffersGeometry(window, 0, 0, format);

    e->surface = eglCreateWindowSurface(e->display, config, window, NULL);
    if (e->surface == EGL_NO_SURFACE) return 0;

    e->context = eglCreateContext(
        e->display,
        config,
        EGL_NO_CONTEXT,
        ctxAttribs
    );
    if (e->context == EGL_NO_CONTEXT) return 0;

    if (!eglMakeCurrent(
        e->display,
        e->surface,
        e->surface,
        e->context
    )) return 0;

    return 1;
}

void EGL_Swap(EGLState* e) {
    eglSwapBuffers(e->display, e->surface);
}

void EGL_Destroy(EGLState* e) {
    if (!e || e->display == EGL_NO_DISPLAY) return;

    eglMakeCurrent(
        e->display,
        EGL_NO_SURFACE,
        EGL_NO_SURFACE,
        EGL_NO_CONTEXT
    );

    if (e->context) eglDestroyContext(e->display, e->context);
    if (e->surface) eglDestroySurface(e->display, e->surface);

    eglTerminate(e->display);
}