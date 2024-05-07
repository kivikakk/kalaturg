package ee.hrzn.kivikakk.sb

import chisel3._

sealed trait Platform {
  def apply[Top <: HasIO[_ <: Data]](top: => Top)(implicit
      clockSpeed: ClockSpeed,
  ): RawModule
}

case object ICE40Platform extends Platform {
  override def apply[Top <: HasIO[_ <: Data]](top: => Top)(implicit
      clockSpeed: ClockSpeed,
  ) = ICE40Top(top)
}

case object CXXRTLPlatform extends Platform {
  override def apply[Top <: HasIO[_ <: Data]](top: => Top)(implicit
      clockSpeed: ClockSpeed,
  ) = top
}
