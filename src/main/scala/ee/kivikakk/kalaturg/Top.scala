package ee.kivikakk.kalaturg

import _root_.circt.stage.ChiselStage
import chisel3._
import ee.hrzn.chryse.ChryseApp
import ee.hrzn.chryse.platform.Platform
import ee.hrzn.chryse.platform.cxxrtl.CXXRTLOptions
import ee.hrzn.chryse.platform.cxxrtl.CXXRTLPlatform
import ee.hrzn.chryse.platform.ice40.IceBreakerPlatform
import ee.kivikakk.kalaturg.uart.UART

// Notes:
// - Buttons and LEDs are active-low.
// - Gotta supply our own POR!
// - `+` and `-` are truncating by default (to the larger of the inputs),
//   equivalent to `+%` and `-%`. Use `+&` or `-%` to widen.
// - `Reg` is completely disconnected from reset.

class Top(val baud: Int)(implicit platform: Platform) extends Module {
  override def desiredName = "kalaturg"

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

    case plat: IceBreakerPlatform =>
      // TODO: can we expose resource.UART as a bundle instead?
      uart.pinsIo.rx         := plat.resources.uart.rx
      plat.resources.uart.tx := uart.pinsIo.tx

      val pwm = Module(new PWM)
      plat.resources.pmod1a(1).o := pwm.io.pmod1a1
      plat.resources.pmod1a(2).o := pwm.io.pmod1a2
      plat.resources.pmod1a(3).o := pwm.io.pmod1a3

      val blinker = Module(new Blinker)
      plat.resources.ledr := blinker.io.ledr
      plat.resources.ledg := blinker.io.ledg
    case _ =>
  }
}

object Top extends ChryseApp {
  override val name                                  = "kalaturg"
  override def genTop()(implicit platform: Platform) = new Top(9600)
  override val targetPlatforms                       = Seq(IceBreakerPlatform(ubtnReset = true))
  override val cxxrtlOptions = Some(
    CXXRTLOptions(
      clockHz = 3_000_000,
      blackboxes = Seq(
        classOf[CXXRTLTestbench],
      ),
    ),
  )
}
