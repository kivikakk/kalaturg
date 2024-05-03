# Steps to disentanglement

I'm no longer able to use Amaranth — I don't feel able to depend on something
I'm not allowed to contribute to — so I need a way to continue on with my FPGA
studies without it. I don't really view just trying to cobble together Verilog
as viable for me right now; I'm rather dependent on having a decent higher-level
thing going, and I already feel all the wind sucked out of my sails from having
to make any change whatsoever.

I've experimented with doing my own HDL using what I learned from working with
and on Amaranth (and Yosys, which I'll happily continue to depend on), but it's
way too much work. After surveying the scene, I've chosen [Chisel]. Scala is not
exactly my favourite, and this means really learning it properly, but y'know
what? That's how I felt about Python too, but I still did [some cursed stuff]
with it!

[Chisel]: https://www.chisel-lang.org/
[some cursed stuff]: https://github.com/amaranth-lang/amaranth/pull/830

I plan to bootstrap my way out of this hole by creating a small component in
Amaranth, workbench it using CXXRTL, then duplicating that component in Chisel,
using the same CXXRTL workbench to test it. This way I'm staying connected to
"doing useful/measurable stuff" in a way I know. I'm also furthering my own
[HDL experiments] while I go, letting Amaranth and Chisel combine in my head.

[HDL experiments]: https://github.com/kivikakk/eri

Done so far:

* Bring [`hdx`][hdx], `rainhdx`, and all their dependencies — including Amaranth
  — up to date.
  * New `abc` revision.
  * Amaranth depends on a newer `pdm-backend`, which I [needed to
    package][pdm-backend package] since it's not in nixpkgs.
  * Had to unbreak rainhdx's Nix, that last refactor was bad.
* Add [basic cxxsim support] to `rainhdx`. This was mostly pulled from [I²C, oh!
  Big stretch][i2c_obs], which I maintain is impeccably named.
  * There was also the option to pull the Zig–CXXRTL support from [sh1107], but
    the extra toolchain weight doesn't feel like it helps me move any faster
    here.
* A basic [UART echo], tested with Amaranth's simulator.
* A clone of the Python simulator [with CXXRTL].
* Learn to do a [very basic Chisel module with tests][Chisel Top] and Verilog
  output.
* Build the Chisel module with CXXRTL and integrate it into the simulator —
  it'll be very *wrong*, but the key is the integration.
* [Write a little unbuffered UART pair, test them, integrate. **Done.**][done]

[hdx]: https://hrzn.ee/kivikakk/hdx
[pdm-backend package]: https://hrzn.ee/kivikakk/hdx/commit/27c3609f5b90e97ed89ca11a7e5747d4b8d0d90b#diff-14a0b9fe455f18efa8eb5b66ab3f4818d6ef7c32
[basic cxxsim support]: https://hrzn.ee/kivikakk/hdx/commit/d52075e49ac05a7297b8ed8cd6cdd8a2808e72b0
[i2c_obs]: https://hrzn.ee/kivikakk/i2c_obs
[sh1107]: https://hrzn.ee/kivikakk/sh1107
[UART echo]: https://hrzn.ee/kivikakk/kalaturg/commit/cd7b97cfb697ac7def0d5d0689da9c03f403d3e0
[with CXXRTL]: https://hrzn.ee/kivikakk/kalaturg/commit/d4c853a680c494fe9acc36aa91b83a7cd2d4d026
[Chisel Top]: https://hrzn.ee/kivikakk/kalaturg/commit/35a791d597e0f31a2affda72a9de2c3f21161e36
[done]: https://hrzn.ee/kivikakk/kalaturg/commit/9d704aa2968ab3d287fe23ccfad2bdf26a88d5e3
