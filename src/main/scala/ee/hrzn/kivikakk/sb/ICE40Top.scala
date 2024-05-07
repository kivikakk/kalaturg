package ee.hrzn.kivikakk.sb

import chisel3._
import chisel3.util._
import chisel3.experimental.BaseModule

trait HasIO[ContainedIO <: Data] extends BaseModule {
  def createIo(): ContainedIO

  val io = IO(createIo())
}

class ICE40Top[
  TopInner <: HasIO[_ <: Data],
](
  private val clockHz: Int,
  inner: => TopInner,
) extends RawModule {
  override def desiredName = "top"

  private val clki = IO(Input(Clock()))

  private val clk_gb = Module(new SB_GB)
  clk_gb.USER_SIGNAL_TO_GLOBAL_BUFFER := clki
  private val clk = clk_gb.GLOBAL_BUFFER_OUTPUT

  private val timerLimit = (15e-6 * clockHz).toInt
  private val resetTimerReg = withClock(clk)(Reg(UInt(unsignedBitLength(timerLimit).W)))
  private val reset = Wire(Bool())

  when(resetTimerReg === timerLimit.U) {
    reset := false.B
  }.otherwise {
    reset := true.B
    resetTimerReg := resetTimerReg + 1.U
  }

  private val io_ubtn = IO(Input(new Bool()))

  private val innerModule = withClockAndReset(clk, reset | ~io_ubtn)(Module(inner))
  private val io = IO(innerModule.createIo())
  io <> innerModule.io
}
