package ee.hrzn.kivikakk.sb

import chisel3._
import ee.hrzn.kivikakk.sb.Platform

class GenericTop[Top <: HasIO[_ <: Data]](genTop: => Top)(implicit
    platform: Platform,
) extends Module {
  override def desiredName = "top"

  private val top = Module(genTop)
  private val io  = IO(top.createIo())
  io :<>= top.io.as[Data]
}

object GenericTop {
  def apply[Top <: HasIO[_ <: Data]](genTop: => Top)(implicit
      platform: Platform,
  ) = new GenericTop(genTop)
}
