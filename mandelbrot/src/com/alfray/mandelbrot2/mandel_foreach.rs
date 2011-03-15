#pragma version(1)

#pragma rs java_package_name(com.alfray.mandelbrot2)

rs_allocation gIn;
rs_allocation gResult;
rs_script gScriptRoot;

#include "mandel_params.rsh"

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

    rsForEach(gScriptRoot, gIn /*null*/, gResult, &p);
}
