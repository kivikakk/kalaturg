package ee.hrzn.kivikakk.kalaturg

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.util._
import _root_.circt.stage.ChiselStage
import ee.hrzn.kivikakk.sb.{ICE40Top, HasIO}

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

  val pmod1a1 = Output(Bool())
  val pmod1a2 = Output(Bool())
  val pmod1a3 = Output(Bool())
}

class Top(val baud: Int = 9600, val clockHz: Int) extends Module with HasIO[TopIO] {
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

  // Uses clockHz in scope. Assumes 1024Hz period to simplify things.
  private def pwmValue(value: UInt)(implicit potency: Double = 1.0) = {
    val period = clockHz / 1024
    val element = ((period / 255).toDouble * potency).toInt
    pwm(period, value * element.U)
  }

  private val rgbVecReg = RegInit(VecInit(255.U(8.W), 0.U, 0.U))
  implicit private val potency: Double = 0.5

  io.pmod1a1 := pwmValue(rgbVecReg(0))
  io.pmod1a2 := pwmValue(rgbVecReg(1))
  io.pmod1a3 := pwmValue(rgbVecReg(2))

  private val rgbCount = 12_000_000 / ((256 * 6) / 6)
  private val rgbCounterReg = RegInit(0.U(unsignedBitLength(rgbCount - 1).W))
  rgbCounterReg := Mux(rgbCounterReg === (rgbCount - 1).U, 0.U, rgbCounterReg + 1.U)

  private val elementIxReg = RegInit(1.U(unsignedBitLength(2).W))
  private val incrementingReg = RegInit(true.B)

  when(rgbCounterReg === (rgbCount - 1).U) {
    when(incrementingReg) {
      when(rgbVecReg(elementIxReg) =/= 255.U) {
        rgbVecReg(elementIxReg) := rgbVecReg(elementIxReg) + 1.U
      }.otherwise {
        elementIxReg := Mux(elementIxReg === 0.U, 2.U, elementIxReg - 1.U)
        incrementingReg := !incrementingReg
      }
    }.otherwise {
      when(rgbVecReg(elementIxReg) =/= 0.U) {
        rgbVecReg(elementIxReg) := rgbVecReg(elementIxReg) - 1.U
      }.otherwise {
        elementIxReg := Mux(elementIxReg === 0.U, 2.U, elementIxReg - 1.U)
        incrementingReg := !incrementingReg
      }
    }
  }
}

object Top extends App {
  def apply(clockHz: Int, baud: Int = 9600): RawModule =
    new ICE40Top(clockHz, new Top(baud = baud, clockHz = clockHz), new TopIO)

  private val firtoolOpts = Array(
    "--lowering-options=disallowLocalVariables",
    "-disable-all-randomization",
    "-strip-debug-info",
  )
  ChiselStage.emitSystemVerilogFile(Top(12_000_000), firtoolOpts = firtoolOpts)
}