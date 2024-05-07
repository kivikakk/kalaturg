package ee.hrzn.kivikakk.sb

import chisel3._

class CXXRTLTop[Top <: HasIO[_ <: Data]](genTop: => Top)(implicit
    clockSpeed: ClockSpeed,
) extends Module {
  override def desiredName = "top"

  private val top = Module(genTop)
  private val io  = IO(top.createIo())
  io :<>= top.io.as[Data]
}

object CXXRTLTop {
  def apply[Top <: HasIO[_ <: Data]](genTop: => Top)(implicit
      clockSpeed: ClockSpeed,
  ) = new CXXRTLTop(genTop)
}
