package ee.hrzn.kivikakk.kalaturg

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.util._
import _root_.circt.stage.ChiselStage

// Notes:
// - Buttons and LEDs are inverted.
// - Gotta supply our own POR!
// - `+` and `-` are truncating by default (to the larger of the inputs),
//   equivalent to `+%` and `-%`. Use `+&` or `-%` to widen.
// - `Reg` is completely disconnected from reset.
// - Look into `DontCare`.

class Top(private val baud: Int = 9600, private val clockHz: Int) extends RawModule {
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

  private val inner = withClockAndReset(clk, reset | ~io_ubtn)(Module(new TopInner(baud, clockHz)))
  private val io = IO(new TopIO)
  io <> inner.io

//  val io_ledrgb_r = IO(Output(new Bool()))
//  val io_ledrgb_g = IO(Output(new Bool()))
//  val io_ledrgb_b = IO(Output(new Bool()))
//  private val rgba_drv = Module(new SB_RGBA_DRV)
//  rgba_drv.CURREN := true.B
//  rgba_drv.RGBLEDEN := true.B
//  io_ledrgb_r := rgba_drv.RGB0
//  io_ledrgb_g := rgba_drv.RGB1
//  io_ledrgb_b := rgba_drv.RGB2

//  rgba_drv.RGB1PWM := false.B
//  rgba_drv.RGB0PWM := true.B
//  rgba_drv.RGB2PWM := true.B
}

class TopIO extends Bundle {
  val plat = new uart.IO
  val ledr = Output(Bool())
  val ledg = Output(Bool())

  val pmod1a1 = Output(Bool())
  val pmod1a2 = Output(Bool())
  val pmod1a3 = Output(Bool())

  val ledrgb_r = Output(Bool())
  val ledrgb_g = Output(Bool())
  val ledrgb_b = Output(Bool())
}

class SB_RGBA_DRV extends ExtModule(Map(
  "CURRENT_MODE" -> "0b0", // full current mode
  "RGB0_CURRENT" -> "0b000011",
  "RGB1_CURRENT" -> "0b000011",
  "RGB2_CURRENT" -> "0b000011",
)) {
  val CURREN = IO(Input(Bool()))
  var RGBLEDEN = IO(Input(Bool()))
  var RGB0PWM = IO(Input(Bool()))
  var RGB1PWM = IO(Input(Bool()))
  var RGB2PWM = IO(Input(Bool()))
  val RGB0 = IO(Output(Bool()))
  val RGB1 = IO(Output(Bool()))
  val RGB2 = IO(Output(Bool()))
}

class SB_GB extends ExtModule {
  val USER_SIGNAL_TO_GLOBAL_BUFFER = IO(Input(Clock()))
  val GLOBAL_BUFFER_OUTPUT = IO(Output(Clock()))
}

class TopInner(val baud: Int = 9600, val clockHz: Int) extends Module {
  val io = IO(new TopIO)

  private val ledReg = RegInit(true.B)
  io.ledr := ledReg
  val timerReg = RegInit(2_999_999.U(unsignedBitLength(5_999_999).W))
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

  // RGB LED stuff.
  private val rgba_drv = Module(new SB_RGBA_DRV)
  rgba_drv.CURREN := true.B
  rgba_drv.RGBLEDEN := true.B
  io.ledrgb_r := rgba_drv.RGB2
  io.ledrgb_g := rgba_drv.RGB1
  io.ledrgb_b := rgba_drv.RGB0

  private val rgbCounterReg = RegInit(0.U(26.W))
  rgbCounterReg := rgbCounterReg + 1.U
  rgba_drv.RGB1PWM := rgbCounterReg(24)
  io.pmod1a1 := rgbCounterReg(24)
  rgba_drv.RGB0PWM := rgbCounterReg(23)
  io.pmod1a2 := rgbCounterReg(23)
  rgba_drv.RGB2PWM := rgbCounterReg(25)
  io.pmod1a3 := rgbCounterReg(25)
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