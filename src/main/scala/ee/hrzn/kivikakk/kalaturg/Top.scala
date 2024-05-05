package ee.hrzn.kivikakk.kalaturg

import chisel3._
import _root_.circt.stage.ChiselStage
import ee.hrzn.kivikakk.kalaturg.uart.UART
import ee.hrzn.kivikakk.kalaturg.uart.PlatIO

class Top(val baud: Int = 9600, val clockHz: Int) extends Module {
  override def desiredName = "top"

  val io = IO(new PlatIO)

  val uart = Module(new UART(baud=baud, clockHz=clockHz))
  io <> uart.platIo

  uart.txIo.bits := 0.U
  uart.txIo.valid := false.B
  uart.rxIo.ready := uart.txIo.ready

  when(uart.txIo.ready && uart.rxIo.valid && !uart.rxIo.bits.err) {
    uart.txIo.bits := uart.rxIo.bits.byte
    uart.txIo.valid := true.B
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