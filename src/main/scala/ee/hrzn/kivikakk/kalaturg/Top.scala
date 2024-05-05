package ee.hrzn.kivikakk.kalaturg

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

// Notes:
// - Buttons and LEDs are inverted.
// - Gotta supply our own POR!

class Top(private val baud: Int = 9600, private val clockHz: Int) extends RawModule {
  override def desiredName = "top"

  private val clk = IO(Input(Clock()))

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

  private val inner = withClockAndReset(clk, reset | ~io_ubtn)(Module(new TopInner(baud, clockHz)))
  private val io = IO(new TopIO)
  io <> inner.io
}

class TopIO extends Bundle {
  val plat = new uart.IO
  val ledr = Output(new Bool())
  val ledg = Output(new Bool())
}

class TopInner(val baud: Int = 9600, val clockHz: Int) extends Module {
  val io = IO(new TopIO)

  private val ledReg = RegInit(true.B)
  io.ledr := ledReg
  val timerReg = RegInit(2999_999.U(unsignedBitLength(5_999_999).W))
  when(timerReg === 0.U) {
    ledReg := ~ledReg
    timerReg := 5_999_999.U
  }.otherwise {
    timerReg := timerReg - 1.U
  }

  io.ledg := false.B

  private val uartM = Module(new uart.UART(baud = baud, clockHz = clockHz))
  io.plat <> uartM.platIo

  uartM.txIo.bits := uartM.rxIo.bits.byte
  uartM.txIo.valid := uartM.txIo.ready && uartM.rxIo.valid && !uartM.rxIo.bits.err
  uartM.rxIo.ready := uartM.txIo.ready
}

object Top extends App {
  private val firtoolOpts = Array(
    "--lowering-options=disallowLocalVariables",
    "-disable-all-randomization",
    "-strip-debug-info",
  )
  println(ChiselStage.emitFIRRTLDialect(new Top(clockHz = 12_000_000), firtoolOpts = firtoolOpts))
  ChiselStage.emitSystemVerilogFile(new Top(clockHz = 12_000_000), firtoolOpts = firtoolOpts)
}