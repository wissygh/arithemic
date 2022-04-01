package division

import chisel3._

trait Divider[T] extends Module {
  val width:     Int //一位符号位，其他是数值位
  val dividend:  T
  val divisor:   T
  val quotient:  T
  val remainder: T
}
class UnsignedDivider extends Divider[UInt] {
  val dividend:  UInt = IO(Input(UInt(width.W)))
  val divisor:   UInt = IO(Input(UInt(width.W)))
  val quotient:  UInt = IO(Input(UInt(width.W)))
  val remainder: UInt = IO(Input(UInt(width.W)))
}

class SignedDivider extends Divider[SInt] {
  val dividend:  SInt = IO(Input(SInt(width.W)))
  val divisor:   SInt = IO(Input(SInt(width.W)))
  val quotient:  SInt = IO(Input(SInt(width.W)))
  val remainder: SInt = IO(Input(SInt(width.W)))
}
