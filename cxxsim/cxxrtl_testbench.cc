#include "cxxrtl_testbench.h"
#include <memory>

void CXXRTLTestbench::reset() { p_tx = wire<1>{1u}; }

bool CXXRTLTestbench::eval(performer *performer) {
  bool converged = true;

  if (this->posedge_p_clock()) {
    // ...
  }

  return converged;
}

std::unique_ptr<cxxrtl_design::bb_p_CXXRTLTestbench>
cxxrtl_design::bb_p_CXXRTLTestbench::create(std::string name,
                                            metadata_map parameters,
                                            metadata_map attributes) {
  return std::make_unique<CXXRTLTestbench>();
}