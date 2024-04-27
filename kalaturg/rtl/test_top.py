import random
import unittest
from amaranth import Fragment
from amaranth.sim import Simulator, Tick
from rainhdx import Platform

from . import Top

class TestTop(unittest.TestCase):
    def test_top(self):
        dut = Top()
        uart = dut.test_uart

        freq = 1e-6
        c = int(1/freq//9600)

        def bench():
            # Hold steady for a bit.
            yield uart.rx.i.eq(1)
            for _ in range(c * 5):
                yield Tick()
                assert (yield uart.tx.o)
            
            # START bit.
            yield uart.rx.i.eq(0)
            for _ in range(c):
                yield Tick()
                assert (yield uart.tx.o)

            inp = [random.choice([0, 1]) for _ in range(8)]
            print("input: ", inp)
            for e in inp:
                yield uart.rx.i.eq(e)
                for _ in range(c):
                    yield Tick()

            # Reassert and wait for their START.
            yield uart.rx.i.eq(1)
            for _ in range(c * 4):
                yield Tick()
                if not (yield uart.tx.o):
                    break
            else:
                assert False, "didn't get START"
            
            # Wait for the rest of the START.
            for _ in range(c - 1):
                yield Tick()

            # Sample the middle of each bit.
            output = []
            for _ in range(8):
                for _ in range(c // 2):
                    yield Tick()
                output.append(1 if (yield uart.tx.o) else 0)
                for _ in range(c - (c // 2)):
                    yield Tick()
            
            # Ensure we reassert promptly.
            for _ in range(c // 4):
                yield Tick()
                if (yield uart.tx.o):
                    break
            else:
                assert False, "didn't reassert"
            print("got: ", output)
            assert inp == output

        sim = Simulator(Fragment.get(dut, platform=Platform["test"]))
        sim.add_clock(freq)
        sim.add_testbench(bench)
        sim.run()
