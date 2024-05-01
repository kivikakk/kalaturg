#include <cstdlib>
#include <ctime>
#include <fstream>
#include <iostream>

#include <cxxrtl/cxxrtl_vcd.h>
#include <build/kalaturg.cc>

#ifndef CLOCK_NAME
#define CLOCK_NAME clk
#endif

#define JOINER(x, y) x ## y
#define REJOINER(x, y) JOINER(x, y)
#define CLOCK_WIRE REJOINER(p_, CLOCK_NAME)

using namespace cxxrtl_design;

void cycle(p_top &top, cxxrtl::vcd_writer &vcd, uint64_t &vcd_time) {
  assert(!top.CLOCK_WIRE);
  top.CLOCK_WIRE.set(true);
  top.step();
  vcd.sample(vcd_time++);

  top.CLOCK_WIRE.set(false);
  top.step();
  vcd.sample(vcd_time++);
}

int inner(p_top &top, cxxrtl::vcd_writer &vcd);

int main(int argc, char **argv) {
  int ret = 0;
  p_top top;
  debug_items di;
  top.debug_info(&di, nullptr, "top ");

  bool do_vcd = argc >= 2 && std::string(argv[1]) == "--vcd";
  cxxrtl::vcd_writer vcd;
  if (do_vcd)
    vcd.add(di);

  ret = inner(top, vcd);

  if (do_vcd) {
    std::ofstream of("cxxsim.vcd");
    of << vcd.buffer;
  }

  return ret;
}

int inner(p_top &top, cxxrtl::vcd_writer &vcd) {
  uint64_t vcd_time = 0;
  srand(time(0));

  int c = 3e6 / 9600;

  // Hold steady for a bit.
  top.p_rx.set(true);
  for (int i = 0; i < c * 2; ++i) {
    cycle(top, vcd, vcd_time);
    if (!top.p_tx) {
      std::cerr << "output desserted during init" << std::endl;
      return 1;
    }
  }

  // START bit.
  top.p_rx.set(false);
  for (int i = 0; i < c; ++i) {
    cycle(top, vcd, vcd_time);
    if (!top.p_tx) {
      std::cerr << "output desserted during START" << std::endl;
      return 1;
    }
  }

  bool inp[8];
  std::cout << "input:  ";
  for (int i = 0; i < 8; ++i) {
    inp[i] = rand() % 2;
    std::cout << (inp[i] ? '1' : '0');

    top.p_rx.set(inp[i]);
    for (int j = 0; j < c; ++j)
      cycle(top, vcd, vcd_time);
  }
  std::cout << std::endl;

  // Reassert/STOP and wait for their START.
  top.p_rx.set(true);
  bool starting = false;
  for (int i = 0; i < c * 4; ++i) {
    cycle(top, vcd, vcd_time);
    if (!top.p_tx) {
      starting = true;
      break;
    }
  }
  if (!starting) {
    std::cerr << "didn't get START" << std::endl;
    return 1;
  }

  // Wait for the rest of the START.
  for (int i = 0; i < c - 1; ++i)
    cycle(top, vcd, vcd_time);

  // Sample the middle of each bit.
  std::cout << "output: ";
  bool output[8];
  for (int i = 0; i < 8; ++i) {
    for (int j = 0; j < c / 2; ++j)
      cycle(top, vcd, vcd_time);
    output[i] = (bool)top.p_tx;
    std::cout << (output[i] ? '1' : '0');
    for (int j = 0; j < c - (c / 2); ++j)
      cycle(top, vcd, vcd_time);
  }
  std::cout << std::endl;

  // STOP bit and then make sure it stays that way.
  for (int i = 0; i < c * 4; ++i) {
    cycle(top, vcd, vcd_time);
    if (!top.p_tx) {
      std::cerr << "no STOP bit" << std::endl;
      return 1;
    }
  }

  for (int i = 0; i < 8; ++i) {
    if (output[i] != inp[i]) {
      std::cerr << "output differed from input at bit " << i << std::endl;
      return 1;
    }
  }

  return 0;
}