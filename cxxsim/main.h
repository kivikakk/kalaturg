#ifndef MAIN_H
#define MAIN_H

#ifndef CLOCK_NAME
#define CLOCK_NAME clk
#endif

#define JOINER(x, y) x ## y
#define REJOINER(x, y) JOINER(x, y)
#define CLOCK_WIRE REJOINER(p_, CLOCK_NAME)

#endif
