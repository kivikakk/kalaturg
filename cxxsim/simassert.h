#ifndef SIMASSERT_H
#define SIMASSERT_H

#include <stdexcept>

class assertion_error : public std::runtime_error
{
public:
  assertion_error(const std::string &msg);
};

void simassert(bool condition, const std::string &msg);

#endif
