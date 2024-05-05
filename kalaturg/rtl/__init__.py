from amaranth import Elaboratable, Module, Signal

from .uart import UART

__all__ = ["Top"]


class TestUart:
    class Io:
        def __init__(self, attr, name):
            setattr(self, attr, Signal(name=name))

    def __init__(self):
        self.rx = TestUart.Io('i', 'io_rx')
        self.tx = TestUart.Io('o', 'io_tx')


class Top(Elaboratable):
    def __init__(self, *, baud=9600):
        super().__init__()
        self.test_uart = TestUart()
        self.baud = baud

    def ports(self, platform):
        return [self.test_uart.rx.i, self.test_uart.tx.o]

    def elaborate(self, platform):
        from .. import icebreaker
        m = Module()

        match platform:
            case icebreaker():
                plat_uart = platform.request("uart")

            case _:
                plat_uart = self.test_uart

        m.submodules.uart = uart = UART(plat_uart, baud=self.baud)

        m.d.comb += [
            uart.wr_data.eq(uart.rd_data),
            uart.wr_en.eq(uart.rd_rdy),
            uart.rd_en.eq(uart.rd_rdy),
        ]

        return m
