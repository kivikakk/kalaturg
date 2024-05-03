package ee.hrzn.kivikakk.kalaturg

import chisel3._

class UART(val baud: Int = 9600, val clockHz: Int) extends Module {
  val io = IO(new Bundle {
    val wrData = Input(UInt(8.W))
    val wrEn = Input(Bool())

    val rdRdy = Output(Bool())
    val rdEn = Input(Bool())
    val rdData = Output(UInt(8.W))
  })
  var platIo = IO(new PlatUART)

  io.rdRdy := false.B
  io.rdData := 0.U

  private val outReg = RegInit(true.B)
  platIo.tx := outReg

  private val divisor = clockHz / baud
}
