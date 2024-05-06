package ee.hrzn.kivikakk.kalaturg

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.util._
import _root_.circt.stage.ChiselStage
import ee.hrzn.kivikakk.sb

// Notes:
// - Buttons and LEDs are active-low.
// - Gotta supply our own POR!
// - `+` and `-` are truncating by default (to the larger of the inputs),
//   equivalent to `+%` and `-%`. Use `+&` or `-%` to widen.
// - `Reg` is completely disconnected from reset.
// - Look into `DontCare`.

class Top(private val baud: Int = 9600, private val clockHz: Int) extends RawModule {
  override def desiredName = "top"

  private val clki = IO(Input(Clock()))

  private val clk_gb = Module(new sb.SB_GB)
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
}

class TopIO extends Bundle {
  val plat = new uart.IO
  val ledr = Output(Bool())
  val ledg = Output(Bool())

  val pmod1a1 = Output(Bool())
  val pmod1a2 = Output(Bool())
  val pmod1a3 = Output(Bool())
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

  // Produces a square wave over period `period` cycles with duty `din/period`.
  private def pwm(period: Int, din: UInt) = {
    val cntReg = RegInit(0.U(unsignedBitLength(period - 1).W))
    cntReg := Mux(cntReg === (period - 1).U, 0.U, cntReg + 1.U)
    din > cntReg
  }

  // Uses clockHz in scope. Assumes 1024hZ period to simplify things.
  private def pwmValue(value: UInt) = {
    val period = clockHz / 1024
    val element = period / 255
    pwm(period, value * element.U)
  }

  private val redReg = RegInit(255.U(8.W))
  private val greenReg = RegInit(0.U(8.W))
  private val blueReg = RegInit(0.U(8.W))

  io.pmod1a1 := pwmValue(redReg)
  io.pmod1a2 := pwmValue(greenReg)
  io.pmod1a3 := pwmValue(blueReg)

  object Stage extends ChiselEnum {
    val GreenUp, RedDown, BlueUp, GreenDown, RedUp, BlueDown = Value
  }
  private val rgbCount = 23_437
  private val rgbCounterReg = RegInit(0.U(unsignedBitLength(rgbCount - 1).W))
  rgbCounterReg := Mux(rgbCounterReg === (rgbCount - 1).U, 0.U, rgbCounterReg + 1.U)

  private val stageReg = RegInit(Stage.GreenUp)

  when(rgbCounterReg === (rgbCount - 1).U) {
    switch(stageReg) {
      is(Stage.GreenUp) {
        when(greenReg =/= 255.U) { greenReg := greenReg + 1.U }.otherwise { stageReg := Stage.RedDown }
      }
      is(Stage.RedDown) {
        when(redReg =/= 0.U) { redReg := redReg - 1.U }.otherwise { stageReg := Stage.BlueUp }
      }
      is(Stage.BlueUp) {
        when(blueReg =/= 255.U) { blueReg := blueReg + 1.U }.otherwise { stageReg := Stage.GreenDown }
      }
      is(Stage.GreenDown) {
        when(greenReg =/= 0.U) { greenReg := greenReg - 1.U }.otherwise { stageReg := Stage.RedUp }
      }
      is(Stage.RedUp) {
        when(redReg =/= 255.U) { redReg := redReg + 1.U }.otherwise { stageReg := Stage.BlueDown }
      }
      is(Stage.BlueDown) {
        when(blueReg =/= 0.U) { blueReg := blueReg - 1.U }.otherwise { stageReg := Stage.GreenUp }
      }
    }
  }
}

object Top extends App {
  private val firtoolOpts = Array(
    "--lowering-options=disallowLocalVariables",
    "-disable-all-randomization",
    "-strip-debug-info",
  )
  // println(ChiselStage.emitFIRRTLDialect(new Top(clockHz = 12_000_000), firtoolOpts = firtoolOpts))
  ChiselStage.emitSystemVerilogFile(new Top(clockHz = 12_000_000), firtoolOpts = firtoolOpts)
}