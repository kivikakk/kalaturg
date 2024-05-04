package ee.hrzn.kivikakk.kalaturg.uart

import chisel3._
import chisel3.util._

class TX(val divisor: Int) extends Module {
  val io = IO(Flipped(Decoupled(UInt(8.W))))
  val platIo = IO(Output(Bool()))

  object State extends ChiselEnum {
    val sIdle, sSTART, sWaitNext, sSTOP = Value
  }
  private val state = RegInit(State.sIdle)

  private val timerReg = Reg(UInt(unsignedBitLength(divisor - 1).W))
  private val counterReg = Reg(UInt(unsignedBitLength(7).W))
  private val shiftReg = Reg(UInt(8.W))

  platIo := true.B
  io.ready := false.B

  switch(state) {
    is(State.sIdle) {
      when(io.valid) {
        timerReg := 0.U
        shiftReg := io.bits
        io.ready := true.B
        state := State.sSTART
      }
    }
    is(State.sSTART) {
      platIo := false.B
      timerReg := timerReg + 1.U
      when(timerReg === (divisor - 1).U) {
        timerReg := 0.U
        counterReg := 0.U
        state := State.sWaitNext
      }
    }
    is(State.sWaitNext) {
      platIo := shiftReg(7)
      timerReg := timerReg + 1.U
      when(timerReg === (divisor - 1).U) {
        timerReg := 0.U
        when(counterReg === 7.U) {
          state := State.sSTOP
        }.otherwise {
          counterReg := counterReg + 1.U
          shiftReg := shiftReg(6, 0) ## 0.U(1.W)
        }
      }
    }
    is(State.sSTOP) {
      timerReg := timerReg + 1.U
      when(timerReg === (divisor - 1).U) {
        state := State.sIdle
      }
    }
  }
}
