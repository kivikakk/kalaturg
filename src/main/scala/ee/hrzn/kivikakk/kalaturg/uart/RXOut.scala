package ee.hrzn.kivikakk.kalaturg.uart

import chisel3._

class RXOut extends Bundle {
  val byte = UInt(8.W)
  val err  = Bool()
}
