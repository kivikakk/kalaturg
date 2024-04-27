package ee.hrzn.kivikakk.kalaturg

import chisel3._
import _root_.circt.stage.ChiselStage

class Top extends Module {
  val io = IO(new Bundle {
    val x = Input(Bool())
    val y = Input(Bool())
    val r = Output(Bool())
    val s = Output(Bool())
  })

  private val sReg = Reg(Bool())

  when(reset.asBool) {
    sReg := 0.U
  }.otherwise {
    sReg := io.r
  }

  io.r := io.x ^ io.y
  io.s := sReg
}

object Top extends App {
  ChiselStage.emitSystemVerilogFile(
    new Top,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}