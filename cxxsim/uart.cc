#include "bench.h"
#include "uart.h"
#include "simassert.h"

UART::UART(unsigned int baud, cxxrtl::value<1> &tx_wire, cxxrtl::value<1> &rx_wire):
  _divisor(CLOCK_HZ / baud),
  _tx_wire(tx_wire),
  _tx_state(tx_idle),
  _rx_wire(rx_wire),
  _rx_state(rx_idle)
{
  _tx_wire.set(true);
}

void UART::cycle()
{
  switch (_tx_state)
  {
    case tx_idle:
      break;
    case tx_start:
      if (--_tx_timer == 0) {
        _tx_wire.set((_tx_sr >> 7) == 1);
        _tx_state = tx_bit;
        _tx_timer = _divisor;
        _tx_counter = 0;
      }
      break;
    case tx_bit:
      if (--_tx_timer == 0) {
        if (_tx_counter == 7) {
          _tx_wire.set(true);
          _tx_state = tx_stop;
          _tx_timer = _divisor;
        } else {
          _tx_sr <<= 1;
          _tx_wire.set((_tx_sr >> 7) == 1);
          _tx_timer = _divisor;
          ++_tx_counter;
        }
      }
      break;
    case tx_stop:
      if (--_tx_timer == 0)
        _tx_state = tx_idle;
      break;
  }

  switch (_rx_state)
  {
    case rx_idle:
      simassert((bool)_rx_wire, "rx went high while (expected) idle");
      break;
    case rx_expecting_start:
      break;
  }
}

void UART::transmit(uint8_t byte)
{
  simassert(_tx_state == tx_idle, "transmit when tx not idle");

  _tx_wire.set(false);
  _tx_state = tx_start;
  _tx_timer = _divisor;
  _tx_sr = byte;
}

void UART::expect(uint8_t byte)
{
  simassert(_rx_state == rx_idle, "expect when rx not idle");

  _rx_state = rx_expecting_start;
  _rx_expected = byte;
}
