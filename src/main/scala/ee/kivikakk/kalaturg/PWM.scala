package ee.kivikakk.kalaturg

import chisel3._
import chisel3.util._
import ee.hrzn.chryse.platform.Platform

class PWMIO extends Bundle {
  val pmod1a1 = Output(Bool())
  val pmod1a2 = Output(Bool())
  val pmod1a3 = Output(Bool())
}

class PWM(implicit platform: Platform) extends Module {
  val io = IO(new PWMIO)

  // Produces a square wave over period `period` cycles with duty `din/period`.
  private def pwm(period: Int, din: UInt) = {
    val cntReg = RegInit(0.U(unsignedBitLength(period - 1).W))
    cntReg := Mux(cntReg === (period - 1).U, 0.U, cntReg + 1.U)
    din > cntReg
  }

  // Assumes 1024Hz period to simplify things.
  private def pwmValue(value: UInt, potency: Double = 1.0) = {
    val period  = platform.clockHz / 1024
    val element = ((period / 255).toDouble * potency).toInt
    pwm(period, value * element.U)
  }

  private val rgbVecReg = RegInit(VecInit(255.U(8.W), 0.U, 0.U))

  private val potency = 0.5
  io.pmod1a1 := pwmValue(rgbVecReg(0), potency)
  io.pmod1a2 := pwmValue(rgbVecReg(1), potency)
  io.pmod1a3 := pwmValue(rgbVecReg(2), potency)

  private val rgbCount      = 12_000_000 / ((256 * 6) / 6)
  private val rgbCounterReg = RegInit(0.U(unsignedBitLength(rgbCount - 1).W))
  rgbCounterReg := Mux(
    rgbCounterReg === (rgbCount - 1).U,
    0.U,
    rgbCounterReg + 1.U,
  )

  private val elementIxReg    = RegInit(1.U(unsignedBitLength(2).W))
  private val incrementingReg = RegInit(true.B)

  when(rgbCounterReg === (rgbCount - 1).U) {
    when(incrementingReg) {
      when(rgbVecReg(elementIxReg) =/= 255.U) {
        rgbVecReg(elementIxReg) := rgbVecReg(elementIxReg) + 1.U
      }.otherwise {
        elementIxReg    := Mux(elementIxReg === 0.U, 2.U, elementIxReg - 1.U)
        incrementingReg := !incrementingReg
      }
    }.otherwise {
      when(rgbVecReg(elementIxReg) =/= 0.U) {
        rgbVecReg(elementIxReg) := rgbVecReg(elementIxReg) - 1.U
      }.otherwise {
        elementIxReg    := Mux(elementIxReg === 0.U, 2.U, elementIxReg - 1.U)
        incrementingReg := !incrementingReg
      }
    }
  }
}
