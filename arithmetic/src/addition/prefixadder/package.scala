package addition

import addition.prefixadder.common.{BrentKungSum, KoggeStoneSum, RippleCarrySum}
import chisel3._
<<<<<<< HEAD
import chisel3.util.Cat
import utils.{extend, sIntToBitPat}

package object prefixadder {

  def apply(
    prefixSum: PrefixSum,
    width:     Option[Int] = None,
    hasSign:   Boolean = false
  )(a:         UInt,
    b:         UInt,
    cin:       Bool = false.B
  ) = {
    val w = width.getOrElse(Seq(a, b).flatMap(_.widthOption).max)
    val m = Module(new PrefixAdder(w, prefixSum))
    // This need synthesis tool to do constant propagation
    m.a := extend(a, w, hasSign)
    m.b := extend(b, w, hasSign)
    m.cin := cin
    Cat(m.cout, m.z)
  }

  def brentKun(a: UInt, b: UInt, cin: Bool = false.B, width: Option[Int] = None, hasSign: Boolean = false) =
    apply(BrentKungSum, width, hasSign)(a, b, cin)

  def koggeStone(a: UInt, b: UInt, cin: Bool = false.B, width: Option[Int] = None, hasSign: Boolean = false) =
    apply(KoggeStoneSum, width, hasSign)(a, b, cin)

  def rippleCarry(a: UInt, b: UInt, cin: Bool = false.B, width: Option[Int] = None, hasSign: Boolean = false) =
    apply(RippleCarrySum, width, hasSign)(a, b, cin)
=======

package object prefixadder {
  def apply(prefixSum: PrefixSum, width: Option[Int] = None)(a: UInt, b: UInt) = {
    val m = Module(new PrefixAdder(width.getOrElse(Seq(a, b).flatMap(_.widthOption).max), prefixSum))
    m.a := a
    m.b := b
    m.z
  }

  def brentKun(a: UInt, b: UInt, width: Option[Int] = None) = apply(BrentKungSum, width)(a, b)

  def koggeStone(a: UInt, b: UInt, width: Option[Int] = None) = apply(KoggeStoneSum, width)(a, b)

  def rippleCarry(a: UInt, b: UInt, width: Option[Int] = None) = apply(RippleCarrySum, width)(a, b)
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
}
