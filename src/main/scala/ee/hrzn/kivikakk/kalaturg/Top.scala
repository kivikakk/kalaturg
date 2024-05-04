package ee.hrzn.kivikakk.kalaturg

import chisel3._
import _root_.circt.stage.ChiselStage
import ee.hrzn.kivikakk.kalaturg.uart.UART
import ee.hrzn.kivikakk.kalaturg.uart.PlatIO

class Top(val baud: Int = 9600, val clockHz: Int) extends Module {
  override def desiredName = "top"

  val io = IO(new PlatIO)

  private val uart = Module(new UART(baud=baud, clockHz=clockHz))
  io <> uart.platIo

  uart.txIo.data := 0.U
  uart.txIo.en := false.B
  uart.rxIo.en := false.B

  when(uart.rxIo.rdy) {
    uart.txIo.data := uart.rxIo.data
    uart.txIo.en := true.B
    uart.rxIo.en := true.B
  }
}

object Top extends App {
  ChiselStage.emitSystemVerilogFile(
    new Top(
      clockHz = 3_000_000, // main.cc assumes this.
    ),
    firtoolOpts = Array(
      "--lowering-options=disallowLocalVariables",
      "-disable-all-randomization",
      "-strip-debug-info",
    )
  )
}