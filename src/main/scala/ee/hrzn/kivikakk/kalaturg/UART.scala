package ee.hrzn.kivikakk.kalaturg

import chisel3._
import chisel3.util._

class UART(val baud: Int = 9600, val clockHz: Int) extends Module {
  // TODO: we'll do this FIFO-less for now; then we'll stress it
  // and force the matter.

  val divisor = clockHz / baud

  class RXIO extends Bundle {
    val rdy = Output(Bool())
    val en = Input(Bool())
    val data = Output(UInt(8.W))
  }

  class RX extends Module {
    val io = IO(new RXIO)
    val platIo = IO(Input(Bool()))

    val rdyReg = RegInit(false.B)
    val dataReg = RegInit(0.U(8.W))
    io.rdy := rdyReg
    io.data := dataReg

    object State extends ChiselEnum {
      val sWaitSTART, sWaitFirstHalf, sWaitNextSample, sWaitLastHalf, sAssertSTOP = Value
    }

    val state = RegInit(State.sWaitSTART)

    val timerReg = RegInit(0.U(unsignedBitLength(divisor - 1).W))
    val counterReg = RegInit(0.U(unsignedBitLength(7).W))
    val shiftReg = Reg(UInt(8.W))

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
        // TODO: we don't actually assert!
        timerReg := timerReg + 1.U
        when(timerReg === (divisor - 1).U) {
          timerReg := 0.U
          state := State.sWaitSTART

          rdyReg := true.B
          dataReg := shiftReg
        }
      }
    }
  }

  class TXIO extends Bundle {
    val data = Input(UInt(8.W))
    val en = Input(Bool())
  }

  class TX extends Module {
    val io = IO(new TXIO)
  }

  val rxIo = IO(new RXIO)
  val txIo = IO(new TXIO)
  val platIo = IO(new PlatUART)

  val rx = Module(new RX)
  rxIo <> rx.io
  platIo.rx <> rx.platIo

  private val outReg = RegInit(true.B)
  platIo.tx := outReg
}