#include <fstream>
#include <iostream>

#include <Top.h>
#include <cxxrtl/cxxrtl_vcd.h>

#include "CXXRTLTestbench.h"
#include "main.h"
#include "simassert.h"

int main(int argc, char **argv) {
  cxxrtl_design::p_top top;
  debug_items di;
  top.debug_info(&di, nullptr, "top ");

  bool do_vcd = argc >= 2 && std::string(argv[1]) == "--vcd";
  cxxrtl::vcd_writer vcd;
  uint64_t vcd_time = 0;
  if (do_vcd)
    vcd.add(di);

  auto &bench = CXXRTLTestbench::inst();

  int ret = 0;
  try {
    while (!bench.finished()) {
      top.CLOCK_WIRE.set(true);
      top.step();
      vcd.sample(vcd_time++);

      top.CLOCK_WIRE.set(false);
      top.step();
      vcd.sample(vcd_time++);
    }
  } catch (assertion_error &e) {
    std::cerr << "got assertion on cycle " << (vcd_time >> 1) << std::endl
              << e.what() << std::endl;
    ret = -1;
  }

  std::cout << "finished on cycle " << (vcd_time >> 1) << std::endl;

  if (do_vcd) {
    std::ofstream of("cxxsim.vcd");
    of << vcd.buffer;
  }

  return ret;
}
