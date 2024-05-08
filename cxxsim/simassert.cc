#include "simassert.h"

assertion_error::assertion_error(const std::string &msg)
    : std::runtime_error(msg) {}

void simassert(bool condition, const std::string &msg) {
  if (!condition)
    throw assertion_error(msg);
}
