#include <cstdlib>
#include <memory>
#include <random>

#include "CXXRTLTestbench.h"
#include "simassert.h"

#define INPUT_COUNT 1 // XXX: 1->6

CXXRTLTestbench::CXXRTLTestbench()
    : _finished(false), _uart(9600, p_tx, p_rx), _inputs(), _state(sSetup),
      _timer(2) {
  simassert(_inst == nullptr, "CXXRTLTestbench already exists");
  _inst = this;

  std::random_device rd;
  std::mt19937 mt(rd());
  std::uniform_int_distribution<uint8_t> dist(0, 255);
  for (int i = 0; i < INPUT_COUNT; ++i)
    _inputs.push(dist(mt));
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

  if (this->posedge_p_clock()) {
    std::cout << "." << std::flush;
    if (_state != sSetup)
      _uart.cycle();

    switch (_state) {
    case sSetup:
      if (--_timer == 0) {
        _state = sInitialStable;
        _timer = _uart.divisor() * 2;
      }
      break;
    case sInitialStable:
      if (--_timer == 0) {
        _finished = true;
      }
      break;
    }
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