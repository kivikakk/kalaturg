package ee.hrzn.kivikakk.kalaturg

import chisel3._
import _root_.circt.stage.ChiselStage

class UART(val baud: Int = 9600, val clockHz: Int) extends Module {
  val io = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
  })

  private val outReg = RegInit(true.B)
  io.tx := outReg

  private val divisor = clockHz / baud
}