#ifndef UART_H
#define UART_H

#include <cxxrtl/cxxrtl.h>

class UART
{
public:
  UART(unsigned int baud, cxxrtl::value<1> &tx_wire, cxxrtl::value<1> &rx_wire);

  void cycle();
  void transmit(uint8_t byte);

private:
  unsigned int _divisor;

  cxxrtl::value<1> &_tx_wire;
  enum tx_state_t {
    idle,
    start,
    bit,
    stop,
  } _tx_state;
  unsigned int _tx_timer;
  unsigned short _tx_counter;
  uint8_t _tx_sr;

  cxxrtl::value<1> &_rx_wire;
};

#endif
