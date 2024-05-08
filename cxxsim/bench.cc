#include <random>

#include "bench.h"
#include "simassert.h"

Bench::Bench(cxxrtl_design::p_top &top, cxxrtl::vcd_writer &vcd):
  _top(top),
  _uart(9600, top.p_io__pins__rx, top.p_io__pins__tx),
  _vcd(vcd),
  _vcd_time(0)
{}

struct eight_bit_byte { uint8_t byte; };
std::ostream &operator <<(std::ostream &os, const eight_bit_byte &ebb)
{
  for (int j = 7; j >= 0; --j)
    os << ((ebb.byte >> j) & 1 ? '1' : '0');
  return os;
}

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

  std::queue<uint8_t> inputs;
  for (int i = 0; i < 6; ++i) {
    uint8_t input = dist(mt);
    std::cout << "input:  " << eight_bit_byte{input} << std::endl;
    _uart.tx_queue(input);
    inputs.push(input);
  }

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
  for (int i = 0; i < d * 9; ++i)
    cycle();

  simassert(_uart.rx_state() == UART::rx_idle, "rx still busy?");

  uint8_t output;
  simassert(_uart.rx_read(&output), "rx empty");

  std::cout << "output: " << eight_bit_byte{output} << std::endl;
  simassert(output == inputs.front(), "output differed from input");
  inputs.pop();

  // Run through the rest.
  while (!inputs.empty()) {
    _uart.rx_expect();
    for (int i = 0; i < d * 4 && _uart.rx_state() == UART::rx_expecting; ++i)
      cycle();
    simassert(_uart.rx_state() == UART::rx_start, "didn't get START");

    for (int i = 0; i < d * 14 && _uart.rx_state() != UART::rx_idle; ++i)
      cycle();
    simassert(_uart.rx_state() == UART::rx_idle, "didn't return to idle");

    simassert(_uart.rx_read(&output), "rx empty");

    std::cout << "output: " << eight_bit_byte{output} << std::endl;
    simassert(output == inputs.front(), "output differed from input");
    inputs.pop();
  }

  // Ensure nothing else happens.
  for (int i = 0; i < d * 4; ++i)
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
