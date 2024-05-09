#include <cstdlib>
#include <memory>
#include <random>

#include "CXXRTLTestbench.h"
#include "simassert.h"

CXXRTLTestbench::CXXRTLTestbench()
    : _finished(false), _uart(9600, p_tx, p_rx), _rd(), _mt(_rd()) {
  simassert(_inst == nullptr, "CXXRTLTestbench already exists");
  _inst = this;
}

CXXRTLTestbench::~CXXRTLTestbench() {
  simassert(_inst == this, "non-match in ~CXXRTLTestbench");
  _inst = nullptr;
}

CXXRTLTestbench &CXXRTLTestbench::inst() {
  simassert(_inst != nullptr, "CXXRTLTestbench doesn't exist in inst");
  return *_inst;
}

CXXRTLTestbench *CXXRTLTestbench::_inst = nullptr;

void CXXRTLTestbench::reset() { p_tx = wire<1>{1u}; }

bool CXXRTLTestbench::eval(performer *performer) {
  bool converged = true;
  std::uniform_int_distribution<uint8_t> dist(0, 255);

  if (this->posedge_p_clock()) {
    // ...
  }

  return converged;
}

bool CXXRTLTestbench::finished() const { return _finished; }

std::unique_ptr<cxxrtl_design::bb_p_CXXRTLTestbench>
cxxrtl_design::bb_p_CXXRTLTestbench::create(std::string name,
                                            metadata_map parameters,
                                            metadata_map attributes) {
  return std::make_unique<CXXRTLTestbench>();
}