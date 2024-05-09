#include <cstdlib>
#include <memory>
#include <random>

#include "CXXRTLTestbench.h"
#include "simassert.h"
#include "utility.h"

#define INPUT_COUNT 6

CXXRTLTestbench::CXXRTLTestbench() : _uart(9600, p_tx, p_rx) {
  simassert(_inst == nullptr, "CXXRTLTestbench already exists");
  _inst = this;

  reset();
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

void CXXRTLTestbench::reset() {
  p_tx = wire<1>{1u};

  _finished = false;
  _uart.reset();

  _inputs.clear();
  std::random_device rd;
  std::mt19937 mt(rd());
  std::uniform_int_distribution<uint8_t> dist(0, 255);
  for (int i = 0; i < INPUT_COUNT; ++i)
    _inputs.push_front(dist(mt));

  _state = sSetup;
  _timer = 2;
}

bool CXXRTLTestbench::eval(performer *performer) {
  bool converged = true;
  uint8_t output;

  if (this->posedge_p_clock()) {
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
        for (auto input : _inputs) {
          std::cout << "input: " << eight_bit_byte{input} << std::endl;
          _uart.tx_queue(input);
        }
        _state = sEcho;
      }
      break;
    case sEcho:
      if (_uart.rx_read(&output)) {
        simassert(!_inputs.empty(),
                  "CXXRTLTestbench got UART read but inputs empty");
        std::cout << "output: " << eight_bit_byte{output} << std::endl;
        simassert(output == _inputs.front(),
                  "CXXRTLTestbench got UART read but mismatch");
        _inputs.pop_front();
      }
      if (_uart.rx_state() == UART::rx_idle) {
        if (_inputs.empty())
          _finished = true;
        else {
          std::cout << "expect()" << std::endl;
          _uart.rx_expect();
        }
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