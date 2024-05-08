package ee.kivikakk.kalaturg.uart

import chisel3._

class PinsIO extends Bundle {
  val rx = Input(Bool())
  val tx = Output(Bool())
}
