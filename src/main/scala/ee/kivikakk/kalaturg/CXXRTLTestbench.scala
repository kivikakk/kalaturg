package ee.kivikakk.kalaturg

import chisel3._
import ee.hrzn.chryse.platform.cxxrtl.CXXRTLBlackBox

class CXXRTLTestbench extends CXXRTLBlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val rx    = Input(Bool())
    val tx    = Output(Bool())
  })
}
