#include "com_alfray_mandelbrot_NativeMandel.h"

#include <stdlib.h>   // for malloc
#include <string.h>   // for memcpy

/*
 * Class:     com_alfray_mandelbrot_NativeMandel
 * Method:    native_mandelbrot
 * Signature: (FFFII[II)I
 */
JNIEXPORT jint JNICALL Java_com_alfray_mandelbrot_NativeMandel_native_1mandelbrot
  (JNIEnv* env, jclass clazz,
   jfloat x_start, jfloat x_step, jfloat y_start,
   jint max_iter,
   jint size, jintArray result,
   jint in_last_ptr) {

  jint* last_ptr = (jint*)in_last_ptr;

  // see if we can reuse the temp storage (if it has the same size)
  if(last_ptr && last_ptr[0] != size) {
    free(last_ptr);
    last_ptr = NULL;
  }

  // alloc temp storage
  if(!last_ptr && size > 0) {
    last_ptr = (jint*)malloc(sizeof(jint) * (size + 1));
    last_ptr[0] = size;
  }

  // simply pass size=0 when caller need to dealloc the temp storage
  if(size <= 0) {
    return (jint)last_ptr;
  }

  jint* ptr = last_ptr + 1;

  for(; size; --size, x_start += x_step) {

    // the "naive" mandelbrot computation. nothing fancy.
    float x = x_start;
    float y = y_start;
    float x2 = x * x;
    float y2 = y * y;
    jint iter = 0;
    while (x2 + y2 < 4 && iter < max_iter) {
      float xtemp = x2 - y2 + x_start;
      y = 2 * x * y + y_start;
      x = xtemp;
      x2 = x * x;
      y2 = y * y;
      ++iter;
    }

    *(ptr++) = iter;
  }

  // update return array
  // c.f. http://java.sun.com/docs/books/performance/1st_edition/html/JPNativeCode.fm.html

  jint* dest = (jint*) (*env)->GetPrimitiveArrayCritical(env, result, NULL /* isCopy */);
  ptr = last_ptr;
  size = *(ptr++);
  memcpy(dest, ptr, size * sizeof(jint));
  (*env)->ReleasePrimitiveArrayCritical(env, result, dest, 0 /* mode */);

  // return temp storage ptr for reuse
  return (jint)last_ptr;
}



