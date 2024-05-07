package ee.hrzn.kivikakk.sb

import chisel3._

trait Platform {
  val id: String
  val clockHz: Int
}

trait ElaboratablePlatform extends Platform {
  def apply[Top <: HasIO[_ <: Data]](top: => Top)(implicit
      platform: Platform,
  ): RawModule
}

case object ICE40Platform extends ElaboratablePlatform {
  val id      = "ice40"
  val clockHz = 12_000_000

  override def apply[Top <: HasIO[_ <: Data]](top: => Top)(implicit
      platform: Platform,
  ) = ICE40Top(top)
}

case object CXXRTLPlatform extends ElaboratablePlatform {
  val id      = "cxxrtl"
  val clockHz = 3_000_000

  override def apply[Top <: HasIO[_ <: Data]](top: => Top)(implicit
      platform: Platform,
  ) = GenericTop(top)
}
