#include <fstream>
#include <iostream>

#include <cxxrtl/cxxrtl_vcd.h>
#include <Top.h>

#include "simassert.h"
#include "bench.h"

int main(int argc, char **argv)
{
  cxxrtl_design::p_top top;
  debug_items di;
  top.debug_info(&di, nullptr, "top ");

  bool do_vcd = argc >= 2 && std::string(argv[1]) == "--vcd";
  cxxrtl::vcd_writer vcd;
  if (do_vcd)
    vcd.add(di);

  Bench bench(top, vcd);
  int ret = -1;
  try {
    ret = bench.run();
  } catch (assertion_error &e) {
    std::cerr
      << "got assertion on cycle " << bench.cycle_number() << std::endl
      << e.what() << std::endl;
    ret = -1;
  }

  if (do_vcd) {
    std::ofstream of("cxxsim.vcd");
    of << vcd.buffer;
  }

  return ret;
}
