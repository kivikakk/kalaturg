import rainhdx
from amaranth_boards.icebreaker import ICEBreakerPlatform

from . import rtl

__all__ = ["Kalaturg", "icebreaker"]


class Kalaturg(rainhdx.Project):
    name = "kalaturg"
    top = rtl.Top
    cxxsim_top = rtl.Top


class icebreaker(ICEBreakerPlatform, rainhdx.Platform):
    pass

class plats:
    class test(rainhdx.Platform):
        simulation = True

        @property
        def default_clk_frequency(self):
            return 1e6

    class cxxsim(rainhdx.Platform):
        simulation = True

        @property
        def default_clk_frequency(self):
            return 3e6
