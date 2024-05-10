package ee.kivikakk.kalaturg

import chisel3._
import chisel3.util._
import ee.hrzn.chryse.platform.Platform

class Blinker(implicit platform: Platform) extends Module {
  val io = IO(new Bundle {
    val ledr = Output(Bool())
    val ledg = Output(Bool())
  })

  private val ledReg = RegInit(true.B)
  io.ledr := ledReg
  val timerReg = RegInit(2_999_999.U(unsignedBitLength(5_999_999).W))
  when(timerReg === 0.U) {
    ledReg   := ~ledReg
    timerReg := 5_999_999.U
  }.otherwise {
    timerReg := timerReg - 1.U
  }

  io.ledg := false.B
}
