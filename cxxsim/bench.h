#ifndef BENCH_H
#define BENCH_H

#include <Top.h>
#include <cxxrtl/cxxrtl_vcd.h>

#include "uart.h"

#ifndef CLOCK_NAME
#define CLOCK_NAME clk
#endif

#define JOINER(x, y) x##y
#define REJOINER(x, y) JOINER(x, y)
#define CLOCK_WIRE REJOINER(p_, CLOCK_NAME)

#define CLOCK_HZ 3000000

class Bench {
public:
  Bench(cxxrtl_design::p_top &top, cxxrtl::vcd_writer &vcd);

  int run();

  uint64_t cycle_number() const;

private:
  void cycle();
  void step();

  cxxrtl_design::p_top &_top;
  UART _uart;

  cxxrtl::vcd_writer &_vcd;
  uint64_t _vcd_time;
};

#endif
