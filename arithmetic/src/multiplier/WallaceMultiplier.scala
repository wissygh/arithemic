package multiplier

import addition.prefixadder.PrefixSum
import addition.prefixadder.common.BrentKungSum
import chisel3._
import chisel3.util._
<<<<<<< HEAD
import chisel3.experimental.FixedPoint
import utils.extend

class WallaceMultiplierImpl(
  val aWidth: Int,
  val bWidth: Int,
=======
import utils.extend

class WallaceMultiplierImpl(
  val width:  Int,
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
  val signed: Boolean
)(radixLog2:  Int,
  sumUpAdder: PrefixSum,
  // TODO: add additional stage for final adder?
  pipeAt: Seq[Int]
  // TODO: making addOneColumn to be configurable to add more CSA to make this circuit more configurable?
) extends Module {

<<<<<<< HEAD
  val a: Bits = IO(Input(if (signed) SInt(aWidth.W) else UInt(aWidth.W)))
  val b: Bits = IO(Input(if (signed) SInt(bWidth.W) else UInt(bWidth.W)))
  val z: Bits = IO(Output(if (signed) SInt((aWidth + bWidth).W) else UInt((aWidth + bWidth).W)))
=======
  val a: Bits = IO(Input(if (signed) SInt(width.W) else UInt(width.W)))
  val b: Bits = IO(Input(if (signed) SInt(width.W) else UInt(width.W)))
  val z: Bits = IO(Output(if (signed) SInt((2 * width).W) else UInt((2 * width).W)))
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460

  val stage:  Int = pipeAt.size
  val stages: Seq[Int] = pipeAt.sorted

  // TODO: use chisel type here?
  def addOneColumn(col: Seq[Bool]): (Seq[Bool], Seq[Bool], Seq[Bool]) =
    col.size match {
      case 1 => // do nothing
        (col, Seq.empty[Bool], Seq.empty[Bool])
      case 2 =>
        val c22 = addition.csa.c22(VecInit(col)).map(_.asBool).reverse
        (Seq(c22(0)), Seq.empty[Bool], Seq(c22(1)))
      case 3 =>
        val c32 = addition.csa.c32(VecInit(col)).map(_.asBool).reverse
        (Seq(c32(0)), Seq.empty[Bool], Seq(c32(1)))
      case 4 =>
        val c53 = addition.csa.c53(VecInit(col :+ false.B)).map(_.asBool).reverse
        (Seq(c53(0)), Seq(c53(1)), Seq(c53(2)))
      case 5 =>
        val c53 = addition.csa.c53(VecInit(col)).map(_.asBool).reverse
        (Seq(c53(0)), Seq(c53(1)), Seq(c53(2)))
      case _ =>
        val (s1, c11, c12) = addOneColumn(col.take(5))
        val (s2, c21, c22) = addOneColumn(col.drop(5))
        (s1 ++ s2, c11 ++ c21, c12 ++ c22)
    }

  def addAll(cols: Array[_ <: Seq[Bool]], depth: Int): (UInt, UInt) = {
    if (cols.map(_.size).max <= 2) {
      val sum = Cat(cols.map(_.head).reverse)
      val carry = Cat(cols.map(col => if (col.length > 1) col(1) else 0.B).reverse)
      (sum, carry)
    } else {
<<<<<<< HEAD
      val columnsNext = Array.fill(aWidth + bWidth)(Seq[Bool]())
=======
      val columnsNext = Array.fill(2 * width)(Seq[Bool]())
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
      var cout1, cout2 = Seq[Bool]()
      for (i <- cols.indices) {
        val (s, c1, c2) = addOneColumn(cols(i) ++ cout1)
        columnsNext(i) = s ++ cout2
        cout1 = c1
        cout2 = c2
      }

      val needReg = stages.contains(depth)
      val toNextLayer =
        if (needReg)
          // TODO: use 'RegEnable' instead
          columnsNext.map(_.map(x => RegNext(x)))
        else
          columnsNext

      addAll(toNextLayer, depth + 1)
    }
  }

  // produce Seq(b, 2 * b, ..., 2^digits * b), output width = width + radixLog2 - 1
<<<<<<< HEAD
  val bMultipleWidth = (bWidth + radixLog2 - 1).W
=======
  val bMultipleWidth = (width + radixLog2 - 1).W
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
  def prepareBMultiples(digits: Int): Seq[SInt] = {
    if (digits == 0) {
      Seq(extend(b, bMultipleWidth.get, signed).asSInt)
    } else {
      val lowerMultiples = prepareBMultiples(digits - 1)
      val bPower2 = extend(b << (digits - 1), bMultipleWidth.get, signed)
      val higherMultiples = lowerMultiples.dropRight(1).map { m =>
        addition.prefixadder.apply(sumUpAdder)(bPower2.asUInt, m.asUInt)(bMultipleWidth.get - 1, 0)
      } :+ (bPower2 << 1)(bMultipleWidth.get - 1, 0)
      lowerMultiples ++ higherMultiples.map(_.asSInt)
    }
  }

  val bMultiples = prepareBMultiples(radixLog2 - 1)
  val encodedWidth = (radixLog2 + 1).W
  val partialProductLookupTable: Seq[(UInt, SInt)] = Range(-(1 << (radixLog2 - 1)), (1 << (radixLog2 - 1)) + 1).map {
    case 0 =>
      0.U(encodedWidth) -> 0.S(bMultipleWidth)
    case i if i > 0 =>
      i.U(encodedWidth) -> bMultiples(i - 1)
    case i if i < 0 =>
      i.S(encodedWidth).asUInt -> (~bMultiples(-i - 1)).asSInt
  }

  def makePartialProducts(i: Int, recoded: SInt): Seq[(Int, Bool)] = { // Seq[(weight, value)]
    val bb: UInt = MuxLookup(recoded.asUInt, 0.S(bMultipleWidth), partialProductLookupTable).asUInt
    val shouldPlus1 = recoded.head(1).asBool
    val s = if (signed) bb.head(1) else shouldPlus1
    val pp = i match {
      case 0 =>
        Cat(~s, Fill(radixLog2, s), bb)
<<<<<<< HEAD
      case i if i >= aWidth - radixLog2 =>
        Cat(~s, bb)
      case _ =>
        val fillWidth = math.min(aWidth - radixLog2, radixLog2 - 1)
=======
      case i if i >= width - radixLog2 =>
        Cat(~s, bb)
      case _ =>
        val fillWidth = math.min(width - radixLog2, radixLog2 - 1)
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
        Cat(Fill(fillWidth, 1.B), ~s, bb)
    }
    Seq.tabulate(pp.getWidth) { j => (i + j, pp(j)) } :+ (i, shouldPlus1)
  }

  val columnsMap = Booth
<<<<<<< HEAD
    .recode(aWidth)(radixLog2, signed = signed)(a.asUInt)
=======
    .recode(width)(radixLog2, signed = signed)(a.asUInt)
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
    .zipWithIndex
    .flatMap { case (x, i) => makePartialProducts(radixLog2 * i, x) }
    .groupBy { _._1 }

<<<<<<< HEAD
  val columns = Array.tabulate(aWidth + bWidth) { i => columnsMap(i).map(_._2) }

  val (sum, carry) = addAll(cols = columns, depth = 0)

  val result = addition.prefixadder.apply(sumUpAdder)(sum, carry)(aWidth + bWidth - 1, 0)
=======
  val columns = Array.tabulate(2 * width) { i => columnsMap(i).map(_._2) }

  val (sum, carry) = addAll(cols = columns, depth = 0)

  val result = addition.prefixadder.apply(sumUpAdder)(sum, carry)(2 * width - 1, 0)
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
  z := (if (signed) result.asSInt else result.asUInt)
}

class SignedWallaceMultiplier(
<<<<<<< HEAD
  val aWidth: Int,
  val bWidth: Int
=======
  val width:  Int
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
)(radixLog2:  Int = 2,
  sumUpAdder: PrefixSum = BrentKungSum,
  pipeAt:     Seq[Int] = Nil)
    extends SignedMultiplier {
<<<<<<< HEAD
  // Choose port whose width is smaller between a and b to "Booth"
  if (aWidth <= bWidth) {
    val impl = Module(new WallaceMultiplierImpl(aWidth, bWidth, true)(radixLog2, sumUpAdder, pipeAt))
    impl.a := a
    impl.b := b
    z := impl.z
  } else { //swap(a,b)
    val impl = Module(new WallaceMultiplierImpl(bWidth, aWidth, true)(radixLog2, sumUpAdder, pipeAt))
    impl.a := b
    impl.b := a
    z := impl.z
  }
}

class UnsignedWallaceMultiplier(
  val aWidth: Int,
  val bWidth: Int
=======
  val impl = Module(new WallaceMultiplierImpl(width, true)(radixLog2, sumUpAdder, pipeAt))
  impl.a := a
  impl.b := b
  z := impl.z
}

class UnsignedWallaceMultiplier(
  val width:  Int
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
)(radixLog2:  Int = 2,
  sumUpAdder: PrefixSum = BrentKungSum,
  pipeAt:     Seq[Int] = Nil)
    extends UnsignedMultiplier {
<<<<<<< HEAD
  // Choose port whose width is smaller between a and b to "Booth"
  if (aWidth <= bWidth) {
    val impl = Module(new WallaceMultiplierImpl(aWidth, bWidth, false)(radixLog2, sumUpAdder, pipeAt))
    impl.a := a
    impl.b := b
    z := impl.z
  } else { // swap(a,b)
    val impl = Module(new WallaceMultiplierImpl(bWidth, aWidth, false)(radixLog2, sumUpAdder, pipeAt))
    impl.a := b
    impl.b := a
    z := impl.z
  }
=======
  val impl = Module(new WallaceMultiplierImpl(width, false)(radixLog2, sumUpAdder, pipeAt))
  impl.a := a
  impl.b := b
  z := impl.z
>>>>>>> f3cb464a2eed5a8c2a886c07fa25e0e8314cd460
}
