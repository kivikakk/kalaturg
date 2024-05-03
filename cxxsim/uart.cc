#include <cassert>

#include "bench.h"
#include "uart.h"

UART::UART(unsigned int baud, cxxrtl::value<1> &tx_wire, cxxrtl::value<1> &rx_wire):
  _divisor(CLOCK_HZ / baud),
  _tx_wire(tx_wire),
  _tx_state(idle),
  _rx_wire(rx_wire)
{
  _tx_wire.set(true);
}

void UART::cycle()
{
  switch (_tx_state)
  {
    case idle:
      break;
    case start:
      if (_tx_timer > 0)
        --_tx_timer;
      else {
        _tx_wire.set((_tx_sr >> 7) == 1);
        _tx_state = bit;
        _tx_timer = _divisor;
        _tx_counter = 0;
      }
      break;
    case bit:
      if (_tx_timer > 0)
        --_tx_timer;
      else {
        if (_tx_counter == 7) {
          _tx_wire.set(true);
          _tx_state = stop;
          _tx_timer = _divisor;
        } else {
          _tx_sr <<= 1;
          _tx_wire.set((_tx_sr >> 7) == 1);
          _tx_timer = _divisor;
          ++_tx_counter;
        }
      }
      break;
    case stop:
      if (_tx_timer > 0)
        --_tx_timer;
      else
        _tx_state = idle;
      break;
  }
}

void UART::transmit(uint8_t byte)
{
  assert(_tx_state == idle);

  _tx_wire.set(false);
  _tx_state = start;
  _tx_timer = _divisor;
  _tx_sr = byte;
}
