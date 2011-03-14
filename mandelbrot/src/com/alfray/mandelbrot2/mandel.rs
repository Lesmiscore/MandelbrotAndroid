#pragma version(1)

#pragma rs java_package_name(com.alfray.mandelbrot2)

int *result;

void mandel2(
    double x_start, double x_step,
    double y_start, double y_step,
    int sx, int sy,
    int max_iter) {

    int *ptr = result;

    // the "naive" mandelbrot computation. nothing fancy.
    double x_begin = x_start;
    int i, j, k;
    for(j = 0, k = 0; j < sy; ++j, y_start += y_step) {
        x_start = x_begin;
        for(i = 0; i < sx; ++i, ++k, x_start += x_step) {
            double x = x_start;
            double y = y_start;
            double x2 = x * x;
            double y2 = y * y;
            int iter = 0;
            while (x2 + y2 < 4 && iter < max_iter) {
                double xtemp = x2 - y2 + x_start;
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
