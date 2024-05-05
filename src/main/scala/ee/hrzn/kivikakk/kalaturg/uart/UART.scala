package ee.hrzn.kivikakk.kalaturg.uart

import chisel3._
import chisel3.util._

class UART(val baud: Int = 9600, val clockHz: Int) extends Module {
  val divisor = clockHz / baud

  val txIo = IO(Flipped(Decoupled(UInt(8.W))))
  val rxIo = IO(Decoupled(new RXOut))
  val platIo = IO(new IO)

  val rx = Module(new RX(divisor))
  rxIo <> Queue(rx.io, 32, useSyncReadMem = true)
  platIo.rx <> rx.platIo

  val tx = Module(new TX(divisor))
  tx.io <> Queue(txIo, 32, useSyncReadMem = true)
  platIo.tx <> tx.platIo
}