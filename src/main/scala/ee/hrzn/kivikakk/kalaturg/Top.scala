package ee.hrzn.kivikakk.kalaturg

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.util._
import _root_.circt.stage.ChiselStage
import ee.hrzn.kivikakk.sb.{ICE40Top, HasIO}
import ee.hrzn.kivikakk.sb.ClockSpeed

// Notes:
// - Buttons and LEDs are active-low.
// - Gotta supply our own POR!
// - `+` and `-` are truncating by default (to the larger of the inputs),
//   equivalent to `+%` and `-%`. Use `+&` or `-%` to widen.
// - `Reg` is completely disconnected from reset.
// - Look into `DontCare`.

class TopIO extends Bundle {
  val plat = new uart.IO
  val ledr = Output(Bool())
  val ledg = Output(Bool())

  val pwm = new PWMIO
}

class Top(val baud: Int = 9600)(implicit clockSpeed: ClockSpeed) extends Module with HasIO[TopIO] {
  def createIo() = new TopIO

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

  private val uartM = Module(new uart.UART(baud = baud))
  io.plat <> uartM.platIo

  uartM.txIo.bits := uartM.rxIo.bits.byte
  uartM.txIo.valid := uartM.txIo.ready && uartM.rxIo.valid && !uartM.rxIo.bits.err
  uartM.rxIo.ready := uartM.txIo.ready

  private val pwm = Module(new PWM)
  pwm.io <> io.pwm
}

object Top extends App {
  def apply(baud: Int = 9600)(implicit clockSpeed: ClockSpeed): RawModule =
    ICE40Top(new Top(baud = baud))

  private val firtoolOpts = Array(
    "--lowering-options=disallowLocalVariables",
    "-disable-all-randomization",
    "-strip-debug-info",
  )
  implicit private val clockSpeed: ClockSpeed = ClockSpeed(12_000_000)
  ChiselStage.emitSystemVerilogFile(Top(), firtoolOpts = firtoolOpts)
}