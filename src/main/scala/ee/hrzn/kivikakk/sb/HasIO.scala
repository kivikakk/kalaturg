package ee.hrzn.kivikakk.sb

import chisel3._

trait HasIO[ContainedIO <: Data] extends RawModule {
  def createIo(): ContainedIO

  val io = IO(createIo())
}
