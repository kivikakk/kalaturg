#ifndef UART_H
#define UART_H

#include <queue>

#include <cxxrtl/cxxrtl.h>

class UART {
public:
  UART(unsigned int baud, cxxrtl::wire<1> &tx_wire, cxxrtl::value<1> &rx_wire);

  enum rx_state_t {
    rx_idle,
    rx_expecting,
    rx_start,
    rx_bit,
    rx_stop,
  };

  void reset();
  void cycle();
  unsigned int divisor() const;

  void tx_queue(uint8_t byte);

  void rx_expect();
  enum rx_state_t rx_state() const;
  bool rx_read(uint8_t *out);

private:
  const unsigned int _divisor;

  cxxrtl::wire<1> &_tx_wire;
  enum tx_state_t {
    tx_idle,
    tx_start,
    tx_bit,
    tx_stop,
  } _tx_state;
  unsigned int _tx_timer;
  unsigned char _tx_counter;
  uint8_t _tx_sr;
  std::queue<uint8_t> _tx_queue;

  cxxrtl::value<1> &_rx_wire;
  enum rx_state_t _rx_state;
  unsigned int _rx_timer;
  unsigned char _rx_counter;
  uint8_t _rx_sr;
  uint8_t _rx_buffer;
  bool _rx_buffer_full;
};

#endif
