package ee.hrzn.kivikakk.kalaturg

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage
import ee.hrzn.kivikakk.kalaturg.uart.UART
import ee.hrzn.kivikakk.kalaturg.uart.PlatIO

/** Notes:
 *
 * - Buttons are LEDs are inverted.
 */

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

  val io_ubtn = IO(Input(new Bool()))

  val io = IO(new PlatIO)
  val io_ledr = IO(Output(new Bool()))
  val io_ledg = IO(Output(new Bool()))

  val inner = withClockAndReset(clk, reset | ~io_ubtn)(Module(new TopInner(baud, clockHz)))
  io <> inner.io
  io_ledr <> inner.io_ledr
  io_ledg <> inner.io_ledg
}

class TopInner(val baud: Int = 9600, val clockHz: Int) extends Module {
  val io = IO(new PlatIO)
  val io_ledr = IO(Output(new Bool()))
  val io_ledg = IO(Output(new Bool()))

  val ledReg = RegInit(true.B)
  io_ledr := ledReg
  val timerReg = RegInit(2999_999.U(unsignedBitLength(5_999_999).W))
  when(timerReg === 0.U) {
    ledReg := ~ledReg
    timerReg := 5_999_999.U
  }.otherwise {
    timerReg := timerReg - 1.U
  }

  io_ledg := false.B

  io.tx := io.rx

  //  val uart = Module(new UART(baud=baud, clockHz=clockHz))
  //  io <> uart.platIo
  //
  //  uart.txIo.bits := uart.rxIo.bits.byte
  //  uart.txIo.valid := uart.txIo.ready && uart.rxIo.valid && !uart.rxIo.bits.err
  //  uart.rxIo.ready := uart.txIo.ready
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