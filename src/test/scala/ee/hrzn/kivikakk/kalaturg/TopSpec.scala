package ee.hrzn.kivikakk.kalaturg

import chisel3._
import chisel3.experimental.BundleLiterals._
import chiseltest._
import chiseltest.simulator.WriteVcdAnnotation
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class TopSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior.of("Top")

  it should "s should equal x^y synchronously" in {
    test(new Top(clockHz = 1000)).withAnnotations(Seq(WriteVcdAnnotation)) {
      c => {
        c.reset.poke(true.B)
        c.clock.step()
        c.reset.poke(false.B)
        c.clock.step()

        for {x <- 0 to 10} {
          c.clock.step()
        }
        c.io.rx.poke(true);
        for {x <- 0 to 10} {
          c.clock.step()
        }
        c.io.rx.poke(false);
        for {x <- 0 to 10} {
          c.clock.step()
        }
      }
    }
  }
}