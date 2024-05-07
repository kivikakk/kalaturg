package ee.hrzn.kivikakk.kalaturg.uart

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class TXSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior.of("TX")

  it should "transmit a byte" in {
    test(new TX(divisor = 3)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      // Note that poked inputs take effect *immediately* on combinatorial
      // circuits. We want to poke as the first thing we do in any simulated
      // cycle, as if responding to the last cycle.  We do this before any
      // expects -- otherwise we might not observe comb changes correctly.
      c.io.bits.poke("b10101100".U)
      c.io.valid.poke(true.B)

      c.platIo.expect(true.B)
      c.io.ready.expect(true.B)

      c.clock.step()

      c.io.valid.poke(false.B)

      for {
        bit <- Seq(0, 1, 0, 1, 0, 1, 1, 0, 0, 1)
        i   <- 0 until 3
      } {
        c.platIo.expect((bit == 1).B)
        c.clock.step()
      }
    }
  }
}
