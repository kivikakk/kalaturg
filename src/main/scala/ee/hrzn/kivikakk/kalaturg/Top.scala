package ee.hrzn.kivikakk.kalaturg

import chisel3._
import _root_.circt.stage.ChiselStage

class Top(val baud: Int = 9600, val clockHz: Int) extends Module {
  override def desiredName = "top"

  val io = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
  })

  io.tx := io.rx

  // private val uart = new UART(baud=baud, clockHz=clockHz)
}

object Top extends App {
  ChiselStage.emitSystemVerilogFile(
    new Top(
      clockHz = 3_000_000, // main.cc assumes this.
    ),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}