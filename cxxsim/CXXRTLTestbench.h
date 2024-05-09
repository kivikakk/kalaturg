#ifndef CXXRTL_TESTBENCH_H
#define CXXRTL_TESTBENCH_H

#include <forward_list>
#include <random>

#include <Top.h>

#include "UART.h"

struct CXXRTLTestbench : cxxrtl_design::bb_p_CXXRTLTestbench {
  CXXRTLTestbench();
  virtual ~CXXRTLTestbench();

  static CXXRTLTestbench &inst();

  void reset() override;
  bool eval(performer *performer) override;

  bool finished() const;

private:
  static CXXRTLTestbench *_inst;

  bool _finished;

  UART _uart;
  std::forward_list<uint8_t> _inputs;
  enum state {
    sSetup,
    sInitialStable,
    sEcho,
  } _state;
  unsigned int _timer;
};

#endif
