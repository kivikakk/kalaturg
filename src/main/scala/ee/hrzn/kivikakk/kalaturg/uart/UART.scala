package ee.hrzn.kivikakk.kalaturg.uart

import chisel3._
import chisel3.util._

class UART(val baud: Int = 9600, val clockHz: Int) extends Module {
  val divisor = clockHz / baud

  val rx = Module(new RX(divisor))
  val tx = Module(new TX(divisor))

  val rxIo = IO(Decoupled(UInt(8.W)))
  val txIo = IO(Flipped(Decoupled(UInt(8.W))))
  val platIo = IO(new PlatIO)

  rxIo <> rx.io
  txIo <> tx.io

  platIo.rx <> rx.platIo
  platIo.tx <> tx.platIo
}