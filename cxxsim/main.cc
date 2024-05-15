#include <csignal>
#include <fstream>
#include <iostream>

#include <Top.h>
#include <cxxrtl/cxxrtl_vcd.h>

#include "CXXRTLTestbench.h"
#include "simassert.h"

static bool caught_sigint = false;
static void sigint_handler(int signum) { caught_sigint = true; }

int main(int argc, char **argv) {
  signal(SIGINT, sigint_handler);

  cxxrtl_design::p_top top;
  debug_items di;
  top.debug_info(&di, nullptr, "top ");

  bool do_vcd = argc >= 2 && std::string(argv[1]) == "--vcd";
  cxxrtl::vcd_writer vcd;
  uint64_t vcd_time = 0;
  if (do_vcd)
    vcd.add(di);

  auto &bench = CXXRTLTestbench::inst();

  // SYNCHRONOUS RESET lol.
  top.p_reset.set(true);

  top.p_clock.set(true);
  top.step();
  vcd.sample(vcd_time++);

  top.p_clock.set(true);
  top.step();
  vcd.sample(vcd_time++);

  top.p_reset.set(false);

  int ret = 0;
  try {
    while (!bench.finished()) {
      top.p_clock.set(true);
      top.step();
      vcd.sample(vcd_time++);

      top.p_clock.set(false);
      top.step();
      vcd.sample(vcd_time++);

      if (caught_sigint) {
        std::cerr << "caught SIGINT on cycle " << (vcd_time >> 1) << std::endl;
        ret = -2;
        break;
      }
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
