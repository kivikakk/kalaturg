package ee.hrzn.kivikakk.kalaturg

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class TopSpec extends AnyFreeSpec with Matchers {
  "r should equal x^y combinationally" in {
    simulate(new Top()) { dut =>
      for { x <- 0 to 1; y <- 0 to 1 } {
        dut.io.x.poke(x)
        dut.io.y.poke(y)
        assert(dut.io.r.peekValue().asBigInt == (x ^ y))
      }
    }
  }

  "s should equal x^y synchronously" in {
    simulate(new Top()) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      dut.clock.step()

      var last = dut.io.s.peekValue().asBigInt
      assert(last == 0)
      for { x <- 0 to 1; y <- 0 to 1 } {
        dut.io.x.poke(x)
        dut.io.y.poke(y)
        assert(dut.io.s.peekValue().asBigInt == last)
        dut.clock.step()
        assert(dut.io.s.peekValue().asBigInt == (x ^ y))
        last = x ^ y
      }
    }
  }
}
