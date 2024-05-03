#ifndef BENCH_H
#define BENCH_H

#include <cxxrtl/cxxrtl_vcd.h>
#include <kalaturg.h>

#include "uart.h"

#ifndef CLOCK_NAME
#define CLOCK_NAME clk
#endif

#define JOINER(x, y) x ## y
#define REJOINER(x, y) JOINER(x, y)
#define CLOCK_WIRE REJOINER(p_, CLOCK_NAME)

#define CLOCK_HZ 3000000

class Bench
{
public:
  Bench(cxxrtl_design::p_top &top, cxxrtl::vcd_writer &vcd);

  int run();

private:
  void cycle();

  cxxrtl_design::p_top &_top;
  UART _uart;

  uint64_t _vcd_time;
  cxxrtl::vcd_writer &_vcd;
};

#endif
