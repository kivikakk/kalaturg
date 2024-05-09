#ifndef UTILITY_H
#define UTILITY_H

#include <iostream>

struct eight_bit_byte {
  uint8_t byte;
};

inline std::ostream &operator<<(std::ostream &os, const eight_bit_byte &ebb) {
  for (int j = 7; j >= 0; --j)
    os << ((ebb.byte >> j) & 1 ? '1' : '0');
  return os;
}

#endif
