package ee.hrzn.kivikakk.kalaturg

import _root_.circt.stage.ChiselStage
import chisel3._
import chisel3.experimental.ExtModule
import chisel3.experimental.noPrefix
import chisel3.util._
import ee.hrzn.kivikakk.kalaturg.uart.UART
import ee.hrzn.kivikakk.sb.ClockSpeed
import ee.hrzn.kivikakk.sb.HasIO
import ee.hrzn.kivikakk.sb.ICE40Top

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

  val pwm = noPrefix(new PWMIO)
}

class Top(val baud: Int = 9600)(implicit clockSpeed: ClockSpeed)
    extends Module
    with HasIO[TopIO] {
  def createIo() = new TopIO

  private val ledReg = RegInit(true.B)
  io.ledr := ledReg
  val timerReg = RegInit(2_999_999.U(unsignedBitLength(5_999_999).W))
  when(timerReg === 0.U) {
    ledReg   := ~ledReg
    timerReg := 5_999_999.U
  }.otherwise {
    timerReg := timerReg - 1.U
  }

  io.ledg := false.B

  private val uart = Module(new UART(baud = baud))
  io.plat <> uart.platIo

  uart.txIo.bits  := uart.rxIo.bits.byte
  uart.txIo.valid := uart.txIo.ready && uart.rxIo.valid && !uart.rxIo.bits.err
  uart.rxIo.ready := uart.txIo.ready

  private val pwm = Module(new PWM)
  pwm.io <> io.pwm
}

object Top extends App {
  def apply(baud: Int = 115_200)(implicit clockSpeed: ClockSpeed): RawModule =
    ICE40Top(new Top(baud = baud))

  private val firtoolOpts = Array(
    "--lowering-options=disallowLocalVariables",
    "-disable-all-randomization",
    "-strip-debug-info",
  )
  implicit private val clockSpeed: ClockSpeed = ClockSpeed(12_000_000)
  ChiselStage.emitSystemVerilogFile(Top(), firtoolOpts = firtoolOpts)
}
