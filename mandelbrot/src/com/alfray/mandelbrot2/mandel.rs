#pragma version(1)

#pragma rs java_package_name(com.alfray.mandelbrot2)

int *result;

void mandel2(
    float x_start, float x_step,
    float y_start, float y_step,
    int sx, int sy,
    int max_iter) {

    int *ptr = result;

    // the "naive" mandelbrot computation. nothing fancy.
    float x_begin = x_start;
    int i, j, k;
    for(j = 0, k = 0; j < sy; ++j, y_start += y_step) {
        x_start = x_begin;
        for(i = 0; i < sx; ++i, ++k, x_start += x_step) {
            float x = x_start;
            float y = y_start;
            float x2 = x * x;
            float y2 = y * y;
            int iter = 0;
            while (x2 + y2 < 4 && iter < max_iter) {
                float xtemp = x2 - y2 + x_start;
                y = 2 * x * y + y_start;
                x = xtemp;
                x2 = x * x;
                y2 = y * y;
                ++iter;
            }

            *(ptr++) = iter;
        } // i
    } // j
}
