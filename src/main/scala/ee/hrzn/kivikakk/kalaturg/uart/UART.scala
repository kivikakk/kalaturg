package ee.hrzn.kivikakk.kalaturg.uart

import chisel3._

class UART(val baud: Int = 9600, val clockHz: Int) extends Module {
  // TODO: we'll do this FIFO-less for now; then we'll stress it
  // and force the matter.

  val divisor = clockHz / baud

  val rx = Module(new RX(divisor))
  val tx = Module(new TX(divisor))

  val rxIo = IO(new rx.RXIO)
  val txIo = IO(new tx.TXIO)
  val platIo = IO(new PlatIO)

  rxIo <> rx.io
  txIo <> tx.io

  platIo.rx <> rx.platIo
  platIo.tx <> tx.platIo
}