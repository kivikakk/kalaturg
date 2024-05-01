from amaranth import Elaboratable, Module, Signal

from .uart import UART

__all__ = ["Top"]


class TestUart:
    class Io:
        def __init__(self, attr, name):
            setattr(self, attr, Signal(name=name))

    def __init__(self):
        self.rx = TestUart.Io('i', 'rx')
        self.tx = TestUart.Io('o', 'tx')


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

        # echo
        with m.FSM():
            with m.State('init'):
                with m.If(uart.rd_rdy):
                    m.d.sync += [
                        uart.rd_en.eq(1),
                        uart.wr_data.eq(uart.rd_data),
                        uart.wr_en.eq(1),
                    ]
                    m.next = 'dessert'

            with m.State('dessert'):
                m.d.sync += [
                    uart.rd_en.eq(0),
                    uart.wr_en.eq(0),
                ]
                m.next = 'init'

        return m
