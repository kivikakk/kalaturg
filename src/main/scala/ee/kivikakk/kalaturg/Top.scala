package ee.kivikakk.kalaturg

import _root_.circt.stage.ChiselStage
import chisel3._
import ee.hrzn.chryse.HasIO
import ee.hrzn.chryse.platform.ElaboratablePlatform
import ee.hrzn.chryse.platform.Platform
import ee.hrzn.chryse.platform.cxxrtl.CXXRTLPlatform
import ee.hrzn.chryse.platform.ice40.ICE40Platform
import ee.kivikakk.kalaturg.uart.UART

import java.io.PrintWriter

// Notes:
// - Buttons and LEDs are active-low.
// - Gotta supply our own POR!
// - `+` and `-` are truncating by default (to the larger of the inputs),
//   equivalent to `+%` and `-%`. Use `+&` or `-%` to widen.
// - `Reg` is completely disconnected from reset.
// - Look into `DontCare`.

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

object Top extends App {
  def apply(
      baud: Int = 9_600,
  )(implicit platform: ElaboratablePlatform) =
    platform(new Top(baud = baud))

  private val firtoolOpts = Array(
    "--lowering-options=disallowLocalVariables",
    "-disable-all-randomization",
    "-strip-debug-info",
  )
  for { platform <- Seq(CXXRTLPlatform(clockHz = 3_000_000), ICE40Platform) } {
    val verilog = ChiselStage.emitSystemVerilog(
      Top()(platform = platform),
      firtoolOpts = firtoolOpts,
    )
    new PrintWriter(s"Top-${platform.id}.sv", "utf-8") {
      try
        write(verilog)
      finally close()
    }
  }
}
