package ee.hrzn.kivikakk.kalaturg.fifo

import chisel3._

class FIFO(val width: Int, val depth: Int) extends Module {
  // Not bothering with transparency, async, unbuffered, etc.
  // TODO: look into DecoupledIO/ValidIO, Flipped,
  val writeIo = IO(new Bundle {
    val data = Input(UInt(width.W))
    val en = Input(Bool())
    val rdy = Output(Bool())
  })
  val readIo = IO(new Bundle {
    val en = Input(Bool())
    val rdy = Output(Bool())
    val data = Output(UInt(width.W))
  })
}
