package ee.hrzn.kivikakk.kalaturg

import chisel3.{Bool, Bundle, Input, Output}

class PlatUART extends Bundle {
  val rx = Input(Bool())
  val tx = Output(Bool())
}
