package ee.kivikakk.kalaturg.uart

import chisel3._
import chisel3.util._
import ee.hrzn.chryse.platform.Platform

class UART(val baud: Int = 9600)(implicit platform: Platform) extends Module {
  val divisor = platform.clockHz / baud

  val txIo = IO(Flipped(Decoupled(UInt(8.W))))
  val rxIo = IO(Decoupled(new RXOut))
  val pinsIo = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
  })

  // Note that UART's meant to be LSB first (!), so we're backwards.
  // Echo tests of course don't reveal this.

  val rx = Module(new RX(divisor))
  rxIo :<>= Queue(rx.io, 32, useSyncReadMem = true)
  rx.pinIo := pinsIo.rx

  val tx = Module(new TX(divisor))
  tx.io :<>= Queue(txIo, 32, useSyncReadMem = true)
  pinsIo.tx := tx.pinIo
}
