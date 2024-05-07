package ee.hrzn.kivikakk.kalaturg.uart

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util._

class RX(private val divisor: Int) extends Module {
  val io     = IO(Decoupled(new RXOut))
  val platIo = IO(Input(Bool()))

  private val syncedPlatIo = RegNext(RegNext(platIo, true.B), true.B)

  private val validReg = RegInit(false.B)
  io.valid := validReg
  private val bitsReg = RegInit(
    new RXOut().Lit(_.byte -> 0.U, _.err -> false.B),
  )
  io.bits := bitsReg

  object State extends ChiselEnum {
    val sIdle, sRx, sFinish = Value
  }
  private val state = RegInit(State.sIdle)

  private val timerReg   = RegInit(0.U(unsignedBitLength(divisor - 1).W))
  private val counterReg = RegInit(0.U(unsignedBitLength(9).W))
  private val shiftReg   = RegInit(0.U(10.W))

  // |_s_|_1_|_2_|_3_|_4_|_5_|_6_|_7_|_8_|_S_|_!
  // ^-- counterReg := 9, state := sRx
  //   ^-- timer hits 0 for the first time, counterReg 9->8
  //       ^-- timer hits 0, counterReg 8->7
  //                                       ^-- 1->0
  //                                           ^-- 0->-1. We finish here, a little late?
  // Is that better or worse than finishing right on time?

  // Reset valid when "consumed".
  when(io.ready) {
    validReg := false.B
  }

  switch(state) {
    is(State.sIdle) {
      when(!syncedPlatIo) {
        timerReg   := (divisor >> 1).U
        counterReg := 9.U
        state      := State.sRx
      }
    }
    is(State.sRx) {
      timerReg := timerReg - 1.U
      when(timerReg === 0.U) {
        timerReg   := (divisor - 1).U
        counterReg := counterReg - 1.U
        shiftReg   := shiftReg(8, 0) ## syncedPlatIo

        when(counterReg === 0.U) {
          state := State.sFinish
        }
      }
    }
    is(State.sFinish) {
      when(io.ready) {
        validReg     := true.B
        bitsReg.byte := shiftReg(8, 1)
        // START high or STOP low.
        bitsReg.err := shiftReg(9) | ~shiftReg(0)
      }
      state := State.sIdle
    }
  }
}
