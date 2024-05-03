#include <cstdlib>
#include <ctime>
#include <fstream>
#include <iostream>

#include <cxxrtl/cxxrtl_vcd.h>
#include <kalaturg.h>

#include "main.h"
#include "uart.h"

using namespace cxxrtl_design;

static int bench(p_top &top, cxxrtl::vcd_writer &vcd);
static void cycle(p_top &top, UART &uart, cxxrtl::vcd_writer &vcd, uint64_t &vcd_time);

int main(int argc, char **argv)
{
  int ret = 0;

  p_top top;
  debug_items di;
  top.debug_info(&di, nullptr, "top ");

  bool do_vcd = argc >= 2 && std::string(argv[1]) == "--vcd";
  cxxrtl::vcd_writer vcd;
  if (do_vcd)
    vcd.add(di);

  ret = bench(top, vcd);

  if (do_vcd) {
    std::ofstream of("cxxsim.vcd");
    of << vcd.buffer;
  }

  return ret;
}

static int bench(p_top &top, cxxrtl::vcd_writer &vcd)
{
  uint64_t vcd_time = 0;
  srand(time(0));

  int c = CLOCK_HZ / 9600;
  UART uart(9600, top.p_io__rx, top.p_io__tx);

  for (int i = 0; i < c * 2; ++i) {
    cycle(top, uart, vcd, vcd_time);
    if (!top.p_io__tx) {
      std::cerr << "output desserted during init" << std::endl;
      return 1;
    }
  }

  uint8_t input = rand() % 256;
  std::cout << "input:  ";
  for (int i = 7; i >= 0; --i)
    std::cout << (((input >> i) & 1) == 1 ? '1' : '0');
  std::cout << std::endl;

  uart.transmit(input);
  for (int i = 0; i < c; ++i) {
    cycle(top, uart, vcd, vcd_time);
    if (!top.p_io__tx) {
      std::cerr << "output desserted during START" << std::endl;
      return 1;
    }
  }

  for (int i = 0; i < 8; ++i) {
    for (int j = 0; j < c; ++j)
      cycle(top, uart, vcd, vcd_time);
  }

  bool starting = false;
  for (int i = 0; i < c * 4; ++i) {
    cycle(top, uart, vcd, vcd_time);
    if (!top.p_io__tx) {
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
    cycle(top, uart, vcd, vcd_time);

  // Sample the middle of each bit.
  std::cout << "output: ";
  uint8_t output = 0;
  for (int i = 0; i < 8; ++i) {
    for (int j = 0; j < c / 2; ++j)
      cycle(top, uart, vcd, vcd_time);
    output = (output << 1) | (top.p_io__tx ? 1 : 0);
    std::cout << (top.p_io__tx ? '1' : '0');
    for (int j = 0; j < c - (c / 2); ++j)
      cycle(top, uart, vcd, vcd_time);
  }
  std::cout << std::endl;

  // STOP bit and then make sure it stays that way.
  for (int i = 0; i < c * 4; ++i) {
    cycle(top, uart, vcd, vcd_time);
    if (!top.p_io__tx) {
      std::cerr << "no STOP bit" << std::endl;
      return 1;
    }
  }

  if (output != input) {
    std::cerr << "output differed from input" << std::endl;
    return 1;
  }

  return 0;
}

static void cycle(p_top &top, UART &uart, cxxrtl::vcd_writer &vcd, uint64_t &vcd_time)
{
  // XXX: here or after?
  uart.cycle();

  assert(!top.CLOCK_WIRE);
  top.CLOCK_WIRE.set(true);
  top.step();
  vcd.sample(vcd_time++);

  top.CLOCK_WIRE.set(false);
  top.step();
  vcd.sample(vcd_time++);
}
