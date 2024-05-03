#include "uart.h"

UART::UART(cxxrtl::value<1> &rx_wire, cxxrtl::value<1> &tx_wire):
  _rx_wire(rx_wire), _tx_wire(tx_wire)
{}
