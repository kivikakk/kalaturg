#include <cstdlib>
#include <ctime>
#include <fstream>
#include <iostream>

#include <cxxrtl/cxxrtl_vcd.h>
#include <kalaturg.h>

#include "bench.h"

int main(int argc, char **argv)
{
  int ret = 0;
  srand(time(0));

  cxxrtl_design::p_top top;
  debug_items di;
  top.debug_info(&di, nullptr, "top ");

  bool do_vcd = argc >= 2 && std::string(argv[1]) == "--vcd";
  cxxrtl::vcd_writer vcd;
  if (do_vcd)
    vcd.add(di);

  ret = Bench(top, vcd).run();

  if (do_vcd) {
    std::ofstream of("cxxsim.vcd");
    of << vcd.buffer;
  }

  return ret;
}
