#include "bench.h"

Bench::Bench(cxxrtl_design::p_top &top, cxxrtl::vcd_writer &vcd):
  _top(top),
  _uart(9600, top.p_io__rx, top.p_io__tx),
  _vcd(vcd),
  _vcd_time(0)
{}

int Bench::run()
{
  int c = CLOCK_HZ / 9600;

  for (int i = 0; i < c * 2; ++i) {
    cycle();
    if (!_top.p_io__tx) {
      std::cerr << "output desserted during init" << std::endl;
      return 1;
    }
  }

  uint8_t input = rand() % 256;
  std::cout << "input:  ";
  for (int i = 7; i >= 0; --i)
    std::cout << (((input >> i) & 1) == 1 ? '1' : '0');
  std::cout << std::endl;

  _uart.transmit(input);
  for (int i = 0; i < c; ++i) {
    cycle();
    if (!_top.p_io__tx) {
      std::cerr << "output desserted during START" << std::endl;
      return 1;
    }
  }

  for (int i = 0; i < 8; ++i) {
    for (int j = 0; j < c; ++j)
      cycle();
  }

  bool starting = false;
  for (int i = 0; i < c * 4; ++i) {
    cycle();
    if (!_top.p_io__tx) {
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
    cycle();

  // Sample the middle of each bit.
  std::cout << "output: ";
  uint8_t output = 0;
  for (int i = 0; i < 8; ++i) {
    for (int j = 0; j < c / 2; ++j)
      cycle();
    output = (output << 1) | (_top.p_io__tx ? 1 : 0);
    std::cout << (_top.p_io__tx ? '1' : '0');
    for (int j = 0; j < c - (c / 2); ++j)
      cycle();
  }
  std::cout << std::endl;

  // STOP bit and then make sure it stays that way.
  for (int i = 0; i < c * 4; ++i) {
    cycle();
    if (!_top.p_io__tx) {
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

void Bench::cycle()
{
  _uart.cycle();

  assert(!_top.CLOCK_WIRE);
  _top.CLOCK_WIRE.set(true);
  _top.step();
  _vcd.sample(_vcd_time++);

  _top.CLOCK_WIRE.set(false);
  _top.step();
  _vcd.sample(_vcd_time++);
}
