package ee.hrzn.kivikakk.kalaturg

import _root_.circt.stage.ChiselStage
import chisel3._
import chisel3.experimental.ExtModule
import chisel3.experimental.noPrefix
import chisel3.util._
import ee.hrzn.chryse.HasIO
import ee.hrzn.chryse.platform.ElaboratablePlatform
import ee.hrzn.chryse.platform.Platform
import ee.hrzn.chryse.platform.cxxrtl.CXXRTLPlatform
import ee.hrzn.chryse.platform.ice40.ICE40Platform
import ee.hrzn.chryse.platform.ice40.ICE40Top
import ee.hrzn.kivikakk.kalaturg.uart.UART

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

class Top(val baud: Int = 9600)(implicit platform: Platform)
    extends Module
    with HasIO[TopIO] {
  def createIo() = new TopIO

  private val uart = Module(new UART(baud = baud))
  io.pins :<>= uart.pinsIo

  uart.txIo.bits  := uart.rxIo.bits.byte
  uart.txIo.valid := uart.txIo.ready && uart.rxIo.valid && !uart.rxIo.bits.err
  uart.rxIo.ready := uart.txIo.ready

  platform match {
    case CXXRTLPlatform =>
      val bb = Module(new CXXRTLTestbench)
      bb.io.clock := clock
      bb.io.rx    := io.pins.rx
      io.pins.tx  := bb.io.tx
    case _ =>
  }

  private val pwm = Module(new PWM)
  io.pwm :<>= pwm.io

  private val blinker = Module(new Blinker)
  io.ledr := blinker.io.ledr
  io.ledg := blinker.io.ledg
}

class CXXRTLTestbench extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val rx    = Input(Bool())
    val tx    = Output(Bool())
  })
}

class Blinker(implicit platform: Platform) extends Module {
  val io = IO(new Bundle {
    val ledr = Output(Bool())
    val ledg = Output(Bool())
  })

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
}

object Top extends App {
  def apply(
      baud: Int = 230_400,
  )(implicit platform: ElaboratablePlatform) =
    platform(new Top(baud = baud))

  private val firtoolOpts = Array(
    "--lowering-options=disallowLocalVariables",
    "-disable-all-randomization",
    "-strip-debug-info",
  )
  for { platform <- Seq(CXXRTLPlatform, ICE40Platform) } {
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
