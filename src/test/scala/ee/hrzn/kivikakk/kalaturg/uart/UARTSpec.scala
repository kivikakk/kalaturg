package ee.hrzn.kivikakk.kalaturg.uart

import chisel3._
import chiseltest._
import chiseltest.simulator.WriteVcdAnnotation
import ee.hrzn.kivikakk.sb.ClockSpeed
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class UARTSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior.of("UART")

  // These tests are *really* ugly, but they work for now. Need more clarity.

  implicit private val clockSpeed: ClockSpeed = ClockSpeed(hz = 3)

  it should "receive a byte" in {
    test(new UART(baud = 1)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      // Assert START and hold for one bit.
      c.platIo.rx.poke(false.B)

      c.rxIo.valid.expect(false.B)

      c.clock.step(3)

      // Generate a byte and play it out. Ensure we remain not `rdy`.
      val input = (new scala.util.Random).nextInt(256)
      for {
        bitIx <- 7 to 0 by -1
        i     <- 0 until 3
      } {
        c.platIo.rx.poke(((input & (1 << bitIx)) != 0).B)
        c.rxIo.valid.expect(false.B)
        c.clock.step()
      }

      // Assert STOP and hold for one bit; wait for sync and processing delay (?).
      c.platIo.rx.poke(true.B)

      for { i <- 0 until 7 } {
        c.rxIo.valid.expect(false.B)
        c.clock.step()
      }

      // Check received OK.
      c.rxIo.ready.poke(true.B)

      c.rxIo.valid.expect(true.B)
      c.rxIo.bits.byte.expect(input)

      // Ensure we can move to the next byte.
      c.clock.step()
      c.rxIo.ready.poke(false.B)
      c.rxIo.valid.expect(false.B)
    }
  }

  it should "transmit a byte" in {
    test(new UART(baud = 1)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      // Generate a byte and request it to be sent.
      val input = (new scala.util.Random).nextInt(256)
      c.txIo.bits.poke(input.U)
      c.txIo.valid.poke(true.B)

      c.platIo.tx.expect(true.B)

      c.clock.step()
      c.txIo.valid.poke(false.B)

      c.platIo.tx.expect(true.B)

      // Watch START.
      for { i <- 0 until 3 } {
        c.clock.step()
        c.platIo.tx.expect(false.B)
      }

      // Check for each bit in turn.
      for {
        bitIx <- 7 to 0 by -1
        i     <- 0 until 3
      } {
        c.clock.step()
        c.platIo.tx.expect(((input & (1 << bitIx)) != 0).B)
      }

      // Watch STOP.
      for { i <- 0 until 3 } {
        c.clock.step()
        c.platIo.tx.expect(true.B)
      }
    }
  }
}
