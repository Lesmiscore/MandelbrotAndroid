#pragma version(1)

#pragma rs java_package_name(com.alfray.mandelbrot2)

rs_allocation gIn;
rs_allocation gResult;
rs_script gScript;

typedef struct Params {
    double x_start;
    double x_step;
    double y_start;
    double y_step;
    int max_iter;
} Params_t;

void root(const void *in, int *out, const Params_t *usrData, uint32_t x, uint32_t y) {

    double x0 = usrData->x_start + usrData->x_step * x;
    double y0 = usrData->y_start + usrData->y_step * y;

    double x1 = x0;
    double y1 = y0;
    double x2 = x1 * x1;
    double y2 = y1 * y1;
    int iter = 0;
    while ((x2 + y2) < 4 && iter < usrData->max_iter) {
        double xtemp = x2 - y2 + x0;
        y1 = 2 * x1 * y1 + y0;
        x1 = xtemp;
        x2 = x1 * x1;
        y2 = y1 * y1;
        ++iter;
    }

    *out = iter;
}

void mandel2(
    double x_start, double x_step,
    double y_start, double y_step,
    int max_iter) {

    Params_t p;
    p.x_start = x_start;
    p.x_step = x_step;
    p.y_start = y_start;
    p.y_step = y_step;
    p.max_iter = max_iter;

    rsForEach(gScript, gIn /*null*/, gResult, &p);
}
