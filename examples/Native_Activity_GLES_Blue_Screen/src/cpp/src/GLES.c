#include "GLES.h"

void GLES_Init() {
    glDisable(GL_DEPTH_TEST);
}

void GLES_Resize(int w, int h) {
    glViewport(0, 0, w, h);
}

void GLES_Clear(float r, float g, float b, float a) {
    glClearColor(r, g, b, a);
    glClear(GL_COLOR_BUFFER_BIT);
}