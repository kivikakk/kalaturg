package ee.hrzn.kivikakk.kalaturg.uart

import chisel3._
import chisel3.util._
import ee.hrzn.kivikakk.sb.ClockSpeed

class UART(val baud: Int = 9600)(implicit clockSpeed: ClockSpeed)
    extends Module {
  val divisor = clockSpeed.hz / baud

  val txIo   = IO(Flipped(Decoupled(UInt(8.W))))
  val rxIo   = IO(Decoupled(new RXOut))
  val platIo = IO(new IO)

  val rx = Module(new RX(divisor))
  rxIo <> Queue(rx.io, 32, useSyncReadMem = true)
  platIo.rx <> rx.platIo

  val tx = Module(new TX(divisor))
  tx.io <> Queue(txIo, 32, useSyncReadMem = true)
  platIo.tx <> tx.platIo
}
