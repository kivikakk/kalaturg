package ee.kivikakk.kalaturg

import chisel3._

class CXXRTLTestbench extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val rx    = Input(Bool())
    val tx    = Output(Bool())
  })
}
