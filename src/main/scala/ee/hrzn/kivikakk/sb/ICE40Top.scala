package ee.hrzn.kivikakk.sb

import chisel3._
import chisel3.util._
import chisel3.experimental.BaseModule

class ICE40Top[Top <: HasIO[_ <: Data]](wrapped: => Top)(implicit
    clockSpeed: ClockSpeed,
) extends RawModule {
  override def desiredName = "top"

  private val clki = IO(Input(Clock()))

  private val clk_gb = Module(new SB_GB)
  clk_gb.USER_SIGNAL_TO_GLOBAL_BUFFER := clki
  private val clk = clk_gb.GLOBAL_BUFFER_OUTPUT

  private val timerLimit = (15e-6 * clockSpeed.hz).toInt
  private val resetTimerReg =
    withClock(clk)(Reg(UInt(unsignedBitLength(timerLimit).W)))
  private val reset = Wire(Bool())

  when(resetTimerReg === timerLimit.U) {
    reset := false.B
  }.otherwise {
    reset         := true.B
    resetTimerReg := resetTimerReg + 1.U
  }
  private val io_ubtn = IO(Input(Bool()))

  private val wrappedModule =
    withClockAndReset(clk, reset | ~io_ubtn)(Module(wrapped))
  private val io = IO(wrappedModule.createIo())
  io :<>= wrappedModule.io.as[Data]
}

object ICE40Top {
  def apply[Top <: HasIO[_ <: Data]](top: => Top)(implicit
      clockSpeed: ClockSpeed,
  ) = new ICE40Top(top)
}

case class ClockSpeed(hz: Int)
