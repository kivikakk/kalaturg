#ifndef CXXRTL_TESTBENCH_H
#define CXXRTL_TESTBENCH_H

#include <Top.h>

struct CXXRTLTestbench : cxxrtl_design::bb_p_CXXRTLTestbench {
  void reset() override;
  bool eval(performer *performer) override;
};

#endif
