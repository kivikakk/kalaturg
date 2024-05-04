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
      simassert((bool)_rx_wire, "rx went low while (expected) idle");
      break;
    case rx_expecting:
      if (!_rx_wire) {
        _rx_state = rx_start;
        _rx_timer = _divisor - 1;
      }
      break;
    case rx_start:
      simassert(!_rx_wire, "rx went high while (expected) START");
      if (--_rx_timer == 0) {
        _rx_state = rx_bit;
        _rx_timer = _divisor;
        _rx_sr = 0;
      }
      break;
    case rx_bit:
      --_rx_timer;
      if (_rx_timer == _divisor / 2) {
        ++_rx_counter;
        _rx_sr = (_rx_sr << 1) | (uint8_t)(bool)_rx_wire;
      } else if (_rx_timer == 0) {
        if (_rx_counter == 8)
          _rx_state = rx_stop;
        _rx_timer = _divisor;
      }
      break;
    case rx_stop:
      simassert((bool)_rx_wire, "rx went low while (expected) STOP");
      if (--_rx_timer == 0) {
        simassert(!_rx_buffer.has_value(), "rx buffer already full");
        _rx_buffer = _rx_sr;
        _rx_state = rx_idle;
      }
      break;
  }
}

unsigned int UART::divisor() const
{
  return _divisor;
}

void UART::tx_send(uint8_t byte)
{
  simassert(_tx_state == tx_idle, "transmit when tx not idle");

  _tx_wire.set(false);
  _tx_state = tx_start;
  _tx_timer = _divisor;
  _tx_sr = byte;
}

void UART::rx_expect()
{
  simassert(_rx_state == rx_idle, "expect when rx not idle");

  _rx_state = rx_expecting;
}

enum UART::rx_state_t UART::rx_state() const
{
  return _rx_state;
}

std::optional<uint8_t> UART::rx_read()
{
  std::optional<uint8_t> rv;
  rv.swap(_rx_buffer);
  return rv;
}
