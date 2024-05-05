package ee.hrzn.kivikakk.kalaturg.uart

import chisel3._

class IO extends Bundle {
  val rx = Input(Bool())
  val tx = Output(Bool())
}
