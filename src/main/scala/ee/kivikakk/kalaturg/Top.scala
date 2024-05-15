package ee.kivikakk.kalaturg

import _root_.circt.stage.ChiselStage
import chisel3._
import ee.hrzn.chryse.ChryseApp
import ee.hrzn.chryse.HasIO
import ee.hrzn.chryse.platform.BoardPlatform
import ee.hrzn.chryse.platform.Platform
import ee.hrzn.chryse.platform.cxxrtl.CXXRTLOptions
import ee.hrzn.chryse.platform.cxxrtl.CXXRTLPlatform
import ee.hrzn.chryse.platform.ice40.ICE40Platform
import ee.kivikakk.kalaturg.uart.UART

// Notes:
// - Buttons and LEDs are active-low.
// - Gotta supply our own POR!
// - `+` and `-` are truncating by default (to the larger of the inputs),
//   equivalent to `+%` and `-%`. Use `+&` or `-%` to widen.
// - `Reg` is completely disconnected from reset.

class TopIO extends Bundle {
  val pins = new uart.PinsIO
  val ledr = Output(Bool())
  val ledg = Output(Bool())

  val pwm = new PWMIO
}

class Top(val baud: Int)(implicit platform: Platform)
    extends Module
    with HasIO[TopIO] {
  def createIo() = new TopIO

  private val uart = Module(new UART(baud = baud))

  uart.txIo.bits  := uart.rxIo.bits.byte
  uart.txIo.valid := uart.txIo.ready && uart.rxIo.valid && !uart.rxIo.bits.err
  uart.rxIo.ready := uart.txIo.ready

  platform match {
    case CXXRTLPlatform(_) =>
      val bb = Module(new CXXRTLTestbench)
      bb.io.clock    := clock
      uart.pinsIo.rx := bb.io.tx
      bb.io.rx       := uart.pinsIo.tx

      io.pins.tx := DontCare
      io.pwm     := DontCare
      io.ledr    := DontCare
      io.ledg    := DontCare
    case _ =>
      io.pins :<>= uart.pinsIo

      val pwm = Module(new PWM)
      io.pwm :<>= pwm.io

      val blinker = Module(new Blinker)
      io.ledr := blinker.io.ledr
      io.ledg := blinker.io.ledg
  }
}

object Top extends ChryseApp {
  override val name            = "kalaturg"
  override val targetPlatforms = Seq(ICE40Platform(ubtnReset = true))
  override val cxxrtlOptions = Some(
    CXXRTLOptions(
      clockHz = 3_000_000,
      blackboxes = Seq(
        classOf[CXXRTLTestbench],
      ),
    ),
  )

  override def genTop(implicit platform: Platform) = new Top(9600)
}
