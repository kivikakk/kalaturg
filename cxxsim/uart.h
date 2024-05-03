#ifndef UART_H
#define UART_H

#include <cxxrtl/cxxrtl.h>

class UART
{
public:
  UART(cxxrtl::value<1> &rx_wire, cxxrtl::value<1> &tx_wire);

private:
  cxxrtl::value<1> &_rx_wire, &_tx_wire;
};

#endif

