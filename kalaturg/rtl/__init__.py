from amaranth import Elaboratable, Module, Signal

from .uart import UART

__all__ = ["Top"]


class TestUart:
    class Io:
        def __init__(self, attr):
            setattr(self, attr, Signal())

    def __init__(self):
        self.rx = TestUart.Io('i')
        self.tx = TestUart.Io('o')


class Top(Elaboratable):
    def __init__(self):
        super().__init__()
        self.test_uart = TestUart()

    def elaborate(self, platform):
        from .. import icebreaker
        m = Module()

        match platform:
            case icebreaker():
                plat_uart = platform.request("uart")

            case _:
                plat_uart = self.test_uart

        m.submodules.uart = uart = UART(plat_uart)

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
