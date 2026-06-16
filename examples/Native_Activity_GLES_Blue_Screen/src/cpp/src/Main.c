#include "EGL.h"
#include "GLES.h"
#include "Main.h"

static EGLState egl;

void Engine_Init(ANativeWindow* window) {
    EGL_Init(&egl, window);
    GLES_Init();
}

void Engine_Resize(int w, int h) {
    GLES_Resize(w, h);
}

void Engine_Draw() {
    GLES_Clear(0.1f, 0.2f, 0.3f, 1.0f); // bluish bg 😎
    EGL_Swap(&egl);
}