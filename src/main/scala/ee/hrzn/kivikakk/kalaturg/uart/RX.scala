package ee.hrzn.kivikakk.kalaturg.uart

import chisel3._
import chisel3.util._

class RX(private val divisor: Int) extends Module {
  val io = IO(Decoupled(UInt(8.W)))
  val platIo = IO(Input(Bool()))

  private val validReg = RegInit(false.B)
  io.valid <> validReg
  private val bitsReg = RegInit(0.U(8.W))
  io.bits <> bitsReg

  object State extends ChiselEnum {
    val sWaitSTART, sWaitFirstHalf, sWaitNextSample, sWaitLastHalf, sAssertSTOP = Value
  }
  private val state = RegInit(State.sWaitSTART)

  private val timerReg = Reg(UInt(unsignedBitLength(divisor - 1).W))
  private val counterReg = Reg(UInt(unsignedBitLength(7).W))
  private val shiftReg = Reg(UInt(8.W))

  when (io.ready) {
    validReg := false.B
  }

  switch(state) {
    is(State.sWaitSTART) {
      timerReg := 0.U
      when(!platIo) {
        when(timerReg === (divisor - 1).U) {
          state := State.sWaitFirstHalf
        }.otherwise {
          timerReg := timerReg + 1.U
        }
      }
    }
    is(State.sWaitFirstHalf) {
      timerReg := timerReg + 1.U
      when(timerReg === ((divisor / 2) - 1).U) {
        timerReg := 0.U
        counterReg := 1.U
        shiftReg := shiftReg(6, 0) ## platIo
        state := State.sWaitNextSample
      }
    }
    is(State.sWaitNextSample) {
      timerReg := timerReg + 1.U
      when(timerReg === (divisor - 1).U) {
        timerReg := 0.U
        shiftReg := shiftReg(6, 0) ## platIo
        when(counterReg === 7.U) {
          state := State.sWaitLastHalf
        }.otherwise {
          counterReg := counterReg + 1.U
        }
      }
    }
    is(State.sWaitLastHalf) {
      timerReg := timerReg + 1.U
      when(timerReg === ((divisor - (divisor / 2)) - 1).U) {
        timerReg := 0.U
        state := State.sAssertSTOP
      }
    }
    is(State.sAssertSTOP) {
      // TODO: we don't actually assert! We have no error reporting.
      timerReg := timerReg + 1.U
      when(timerReg === (divisor - 1).U) {
        timerReg := 0.U
        state := State.sWaitSTART

        when(io.ready) {
          validReg := true.B
          bitsReg := shiftReg
        }
      }
    }
  }
}
