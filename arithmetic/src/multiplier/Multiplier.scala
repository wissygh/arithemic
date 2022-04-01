package multiplier

import chisel3._
<<<<<<< HEAD
import chisel3.experimental.FixedPoint

trait Multiplier[T] extends Module {
  val aWidth: Int
  val bWidth: Int
  require(aWidth > 0)
  require(bWidth > 0)
=======

trait Multiplier[T] extends Module {
  val width: Int
  require(width > 0)
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
  val a: T
  val b: T
  val z: T
}

trait SignedMultiplier extends Multiplier[SInt] {
<<<<<<< HEAD
  val a: SInt = IO(Input(SInt(aWidth.W)))
  val b: SInt = IO(Input(SInt(bWidth.W)))
  val z: SInt = IO(Output(SInt((aWidth + bWidth).W)))
=======
  val a: SInt = IO(Input(SInt(width.W)))
  val b: SInt = IO(Input(SInt(width.W)))
  val z: SInt = IO(Output(SInt((2 * width).W)))
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
  assert(a * b === z)
}

trait UnsignedMultiplier extends Multiplier[UInt] {
<<<<<<< HEAD
  val a: UInt = IO(Input(UInt(aWidth.W)))
  val b: UInt = IO(Input(UInt(bWidth.W)))
  val z: UInt = IO(Output(UInt((aWidth + bWidth).W)))
=======
  val a: UInt = IO(Input(UInt(width.W)))
  val b: UInt = IO(Input(UInt(width.W)))
  val z: UInt = IO(Output(UInt((2 * width).W)))
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
  assert(a * b === z)
}
