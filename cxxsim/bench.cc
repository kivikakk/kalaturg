#include <random>

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
  std::random_device rd;
  std::mt19937 mt(rd());
  std::uniform_int_distribution<uint8_t> dist(0, 255);

  unsigned int d = _uart.divisor();
  std::cout << "divisor: " << d << std::endl;

  step();
  step();

  // Check we remain stable for a while.
  for (int i = 0; i < d * 2; ++i)
    cycle();

  uint8_t input = dist(mt);
  std::cout << "input:  ";
  for (int i = 7; i >= 0; --i)
    std::cout << ((input >> i) & 1 ? '1' : '0');
  std::cout << std::endl;

  _uart.tx_send(input);

  // START + 8 bits.
  for (int i = 0; i < 9 * d; ++i)
    cycle();

  _uart.rx_expect();
  
  // Wait some time for START.  d*4 is arbitrary; depends how long the other end
  // takes.  (At least d*1 is needed assuming the other end waits for our STOP.)
  for (int i = 0; i < d * 4 && _uart.rx_state() == UART::rx_expecting; ++i)
    cycle();
  simassert(_uart.rx_state() == UART::rx_start, "didn't get START");

  // Wait for the rest of the START.
  for (int i = 0; i < d - 1; ++i)
    cycle();

  simassert(_uart.rx_state() == UART::rx_bit, "didn't transition from START");

  // Wait for all eight bits and STOP.
  for (int i = 0; i < d * 11; ++i)
    cycle();

  simassert(_uart.rx_state() == UART::rx_idle, "rx still busy?");

  auto maybe_output = _uart.rx_read();
  simassert(maybe_output.has_value(), "rx empty");
  uint8_t output = *maybe_output;

  // Sample the middle of each bit.
  std::cout << "output: ";
  for (int i = 7; i >= 0; --i)
    std::cout << ((output >> i) & 1 ? '1' : '0');
  std::cout << std::endl;

  simassert(output == input, "output differed from input");

  // Ensure nothing else happens.
  for (int i = 0; i < d * 2; ++i)
    cycle();

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