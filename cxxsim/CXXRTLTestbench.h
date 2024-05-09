#ifndef CXXRTL_TESTBENCH_H
#define CXXRTL_TESTBENCH_H

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

  std::random_device _rd;
  std::mt19937 _mt;
};

#endif
