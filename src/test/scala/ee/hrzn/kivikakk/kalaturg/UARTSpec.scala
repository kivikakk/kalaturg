package ee.hrzn.kivikakk.kalaturg

import chisel3._
import chiseltest._
import chiseltest.simulator.WriteVcdAnnotation
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class UARTSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior.of("UART")

  it should "receive a byte" in {
    test(new UART(baud = 1, clockHz = 5)).withAnnotations(Seq(WriteVcdAnnotation))(c => {
      c.platIo.rx.poke(true.B)
      c.rxIo.rdy.expect(false.B)

      // Assert START and hold for one bit.
      c.platIo.rx.poke(false.B)
      c.clock.step(5)

      // Generate a byte and play it out. Ensure we remain not `rdy`.
      val input = (new scala.util.Random).nextInt(256)
      for (bitIx <- 7 to 0 by -1) {
        c.rxIo.rdy.expect(false.B)
        c.platIo.rx.poke(((input & (1 << bitIx)) != 0).B)
        c.clock.step(5)
      }

      // Assert STOP and hold for one bit.
      c.rxIo.rdy.expect(false.B)
      c.platIo.rx.poke(true.B)
      c.clock.step(5)

      // Check received OK.
      c.rxIo.rdy.expect(true.B)
      c.rxIo.data.expect(input)

      // Ensure we can move to the next byte. (TODO: actually have a FIFO.)
      c.rxIo.en.poke(true.B)
      c.clock.step()
      c.rxIo.rdy.expect(false.B)
    })
  }

  it should "transmit a byte" in {
    test(new UART(baud = 1, clockHz = 5)).withAnnotations(Seq(WriteVcdAnnotation))(c => {
      c.platIo.tx.expect(true.B)

      // Generate a byte and request it to be sent.
      val input = (new scala.util.Random).nextInt(256)
      c.txIo.data.poke(input.U)
      c.txIo.en.poke(true.B)

      // Watch START.
      for { i <- 0 until 5 } {
        c.clock.step()
        c.platIo.tx.expect(false.B)
      }

      // Check for each bit in turn.
      for { bitIx <- 7 to 0 by -1; i <- 0 until 5 } {
        c.clock.step()
        c.platIo.tx.expect(((input & (1 << bitIx)) != 0).B)
      }

      // Watch STOP.
      for { i <- 0 until 5 } {
        c.clock.step()
        c.platIo.tx.expect(true.B)
      }
    })
  }
}