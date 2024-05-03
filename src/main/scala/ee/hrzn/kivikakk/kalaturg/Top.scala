package ee.hrzn.kivikakk.kalaturg

import chisel3._
import _root_.circt.stage.ChiselStage

class Top(val baud: Int = 9600, val clockHz: Int) extends Module {
  override def desiredName = "top"

  val io = IO(new PlatUART)

  private val uart = Module(new UART(baud=baud, clockHz=clockHz))
  io <> uart.platIo

  uart.txIo.data := 0.U
  uart.txIo.en := false.B

  uart.rxIo.en := 0.U
}

object Top extends App {
  ChiselStage.emitSystemVerilogFile(
    new Top(
      clockHz = 3_000_000, // main.cc assumes this.
    ),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}