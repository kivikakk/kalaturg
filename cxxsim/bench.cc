#include "bench.h"
#include "simassert.h"

Bench::Bench(cxxrtl_design::p_top &top, cxxrtl::vcd_writer &vcd):
  _top(top),
  _uart(9600, top.p_io__rx, top.p_io__tx),
  _vcd(vcd),
  _vcd_time(0)
{}

int Bench::run()
{
  int c = CLOCK_HZ / 9600;

  step();

  // Check we remain stable for a while.
  for (int i = 0; i < c * 2; ++i)
    cycle();

  uint8_t input = rand() % 256;
  std::cout << "input:  ";
  for (int i = 7; i >= 0; --i)
    std::cout << (((input >> i) & 1) == 1 ? '1' : '0');
  std::cout << std::endl;

  _uart.transmit(input);

  // START + 8 bits.
  for (int i = 0; i < 9 * c; ++i)
    cycle();

  _uart.expect(input);
  
  for (int i = 0; i < c * 4 && !_uart.rx_busy(); ++i)
    cycle();
  simassert(_uart.rx_busy(), "didn't get START");

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

uint64_t Bench::cycle_number() const
{
  return _vcd_time >> 1;
}

void Bench::cycle()
{
  _uart.cycle();
  step();
}

void Bench::step()
{
  simassert(!_top.CLOCK_WIRE, "step when clock not low");
  _top.CLOCK_WIRE.set(true);
  _top.step();
  _vcd.sample(_vcd_time++);

  _top.CLOCK_WIRE.set(false);
  _top.step();
  _vcd.sample(_vcd_time++);
}
