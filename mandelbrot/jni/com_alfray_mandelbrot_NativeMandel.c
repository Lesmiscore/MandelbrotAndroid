#include "com_alfray_mandelbrot_NativeMandel.h"

JNIEXPORT jlong JNICALL Java_com_alfray_mandelbrot_NativeMandel_add
(JNIEnv *env, jclass c, jlong a, jlong b) {
  return a+b;
}

