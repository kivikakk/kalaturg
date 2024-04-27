from amaranth import Module
from amaranth.lib.fifo import SyncFIFOBuffered
from amaranth.lib.io import Pin
from amaranth.lib.wiring import Component, In, Out
from amaranth_stdio.serial import AsyncSerial

__all__ = ["UART"]


class UART(Component):
    wr_data: In(8)
    wr_en: In(1)

    rd_rdy: Out(1)
    rd_en: In(1)
    rd_data: Out(8)

    _plat_uart: Pin
    _baud: int
    _tx_fifo: SyncFIFOBuffered
    _rx_fifo: SyncFIFOBuffered

    def __init__(self, plat_uart, baud=9600):
        self._plat_uart = plat_uart
        self._baud = baud
        super().__init__()

    def elaborate(self, platform):
        m = Module()

        # if platform.simulation:
        #     # Blackboxed in tests.
        #     return m

        freq = platform.default_clk_frequency

        m.submodules.serial = serial = AsyncSerial(
            divisor=int(freq // self._baud),
            pins=self._plat_uart,
        )

        # tx
        m.submodules.tx_fifo = self._tx_fifo = SyncFIFOBuffered(width=8, depth=32)
        m.d.comb += [
            self._tx_fifo.w_data.eq(self.wr_data),
            self._tx_fifo.w_en.eq(self.wr_en),
        ]
        with m.FSM() as fsm:
            with m.State("idle"):
                with m.If(serial.tx.rdy & self._tx_fifo.r_rdy):
                    m.d.sync += serial.tx.data.eq(self._tx_fifo.r_data)
                    m.next = "wait"

            with m.State("wait"):
                m.next = "idle"

            m.d.comb += [
                serial.tx.ack.eq(fsm.ongoing("wait")),
                self._tx_fifo.r_en.eq(fsm.ongoing("wait")),
            ]

        # rx
        m.submodules.rx_fifo = self._rx_fifo = SyncFIFOBuffered(width=8, depth=32)
        m.d.comb += [
            self.rd_rdy.eq(self._rx_fifo.r_rdy),
            self.rd_data.eq(self._rx_fifo.r_data),
            self._rx_fifo.r_en.eq(self.rd_en),
        ]
        with m.FSM() as fsm:
            with m.State("idle"):
                m.d.sync += self._rx_fifo.w_en.eq(0)
                with m.If(serial.rx.rdy):
                    m.next = "read"

            with m.State("read"):
                m.d.sync += [
                    self._rx_fifo.w_data.eq(serial.rx.data),
                    self._rx_fifo.w_en.eq(1),
                ]
                m.next = "idle"

            m.d.comb += serial.rx.ack.eq(fsm.ongoing("idle"))

        return m
