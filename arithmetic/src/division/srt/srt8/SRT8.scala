package division.srt.srt8

import division.srt._
import division.srt.SRTTable
import chisel3._
import chisel3.util._
import utils.leftShift

/** SRT8
  * 1/2 <= d < 1, 1/2 < rho <=1, 0 < q  < 2
  * radix = 8
  * a = 7, {-7, ... ,-2, -1, 0, 1, 2, ... 7},
  * dTruncateWidth = 4, rTruncateWidth = 4
  * y^（xxxx.xxxx）, d^（0.1xxx）
  * table from SRTTable
  * -129/16 < y^ < 127/16
  * floor((-r*rho - 2^-t)_t) <= y^ <= floor((r*rho - ulp)_t)
  */

class SRT8(
  dividendWidth:  Int,
  dividerWidth:   Int,
  n:              Int, // the longest width
  radixLog2:      Int = 3,
  a:              Int = 7,
  dTruncateWidth: Int = 4,
  rTruncateWidth: Int = 4)
    extends Module {

  val xLen: Int = dividendWidth + radixLog2 + 1
  val wLen: Int = xLen + radixLog2

  // IO
  val input = IO(Flipped(DecoupledIO(new SRTInput(dividendWidth, dividerWidth, n))))
  val output = IO(ValidIO(new SRTOutput(dividerWidth, dividendWidth)))

  val partialReminderCarryNext, partialReminderSumNext = Wire(UInt(wLen.W))
  val quotientNext, quotientMinusOneNext = Wire(UInt(n.W))
  val dividerNext = Wire(UInt(dividerWidth.W))
  val counterNext = Wire(UInt(log2Ceil(n).W))

  // Control
  // sign of select quotient, true -> negative, false -> positive
  // sign of Cycle, true -> (counter === 0.U)
  val isLastCycle, enable: Bool = Wire(Bool())

  // State
  // because we need a CSA to minimize the critical path
  val partialReminderCarry = RegEnable(partialReminderCarryNext, 0.U(wLen.W), enable)
  val partialReminderSum = RegEnable(partialReminderSumNext, 0.U(wLen.W), enable)
  val divider = RegEnable(dividerNext, 0.U(dividerWidth.W), enable)
  val quotient = RegEnable(quotientNext, 0.U(n.W), enable)
  val quotientMinusOne = RegEnable(quotientMinusOneNext, 0.U(n.W), enable)
  val counter = RegEnable(counterNext, 0.U(log2Ceil(n).W), enable)

  //  Datapath
  //  according two adders
  isLastCycle := !counter.orR
  output.valid := isLastCycle
  input.ready := isLastCycle
  enable := input.fire || !isLastCycle

  val remainderNoCorrect: UInt = partialReminderSum + partialReminderCarry
  val remainderCorrect: UInt =
    partialReminderSum + partialReminderCarry + (divider << radixLog2)
  val needCorrect: Bool = remainderNoCorrect(wLen - 4).asBool
  output.bits.reminder := Mux(needCorrect, remainderCorrect, remainderNoCorrect)(wLen - 5, radixLog2)
  output.bits.quotient := Mux(needCorrect, quotientMinusOne, quotient)

  val rWidth: Int = 1 + radixLog2 + rTruncateWidth
  val tables: Seq[Seq[Int]] = SRTTable(1 << radixLog2, a, dTruncateWidth, rTruncateWidth).tablesToQDS

  val ohWidth: Int = a match {
    case 7 => 10
    case 6 => 8
    case 5 => 8
    case 4 => 8
  }
  // qds
  val selectedQuotientOH: UInt =
    QDS(rWidth, ohWidth, dTruncateWidth - 1, tables, a)(
      leftShift(partialReminderSum, radixLog2).head(rWidth),
      leftShift(partialReminderCarry, radixLog2).head(rWidth),
      dividerNext.head(dTruncateWidth)(dTruncateWidth - 2, 0) //.1********* -> 1*** -> ***
    )
  // On-The-Fly conversion
  val otf = OTF(radixLog2, n, ohWidth, a)(quotient, quotientMinusOne, selectedQuotientOH)

  val dividerLMap = VecInit((-2 to 2).map {
    case -2 => divider << 1 // -2
    case -1 => divider // -1
    case 0  => 0.U //  0
    case 1  => Fill(1 + radixLog2, 1.U(1.W)) ## ~divider // 1
    case 2  => Fill(radixLog2, 1.U(1.W)) ## ~(divider << 1) // 2
  })

  if (a == 7) {
    val qHigh:    UInt = selectedQuotientOH(9, 5)
    val qLow:     UInt = selectedQuotientOH(4, 0)
    val qdsSign0: Bool = qHigh.head(2).orR
    val qdsSign1: Bool = qLow.head(2).orR
    // csa for SRT8 -> CSA32+CSA32
    val dividerHMap = VecInit((-2 to 2).map {
      case -2 => divider << 3 // -8
      case -1 => divider << 2 // -4
      case 0  => 0.U //  0
      case 1  => Fill(2, 1.U(1.W)) ## ~(divider << 2) // 4
      case 2  => Fill(1, 1.U(1.W)) ## ~(divider << 3) // 8
    })
    val csa0 = addition.csa.c32(
      VecInit(
        leftShift(partialReminderSum, radixLog2).head(wLen - radixLog2),
        leftShift(partialReminderCarry, radixLog2).head(wLen - radixLog2 - 1) ## qdsSign0,
        Mux1H(qHigh, dividerHMap)
      )
    )
    val csa1 = addition.csa.c32(
      VecInit(
        csa0(1).head(wLen - radixLog2),
        leftShift(csa0(0), 1).head(wLen - radixLog2 - 1) ## qdsSign1,
        Mux1H(qLow, dividerLMap)
      )
    )
    partialReminderSumNext := Mux(input.fire, input.bits.dividend, csa1(1) << radixLog2)
    partialReminderCarryNext := Mux(input.fire, 0.U, csa1(0) << 1 + radixLog2)
  } else if (a == 6) {
    val qHigh:    UInt = selectedQuotientOH(7, 5)
    val qLow:     UInt = selectedQuotientOH(4, 0)
    val qdsSign0: Bool = qHigh.head(1).asBool
    val qdsSign1: Bool = qLow.head(2).orR

    // csa for SRT8 -> CSA32+CSA32
    val dividerHMap = VecInit((-1 to 1).map {
      case -1 => divider << 2 // -4
      case 0  => 0.U //  0
      case 1  => Fill(2, 1.U(1.W)) ## ~(divider << 2) // 4
    })
    val csa0 = addition.csa.c32(
      VecInit(
        leftShift(partialReminderSum, radixLog2).head(wLen - radixLog2),
        leftShift(partialReminderCarry, radixLog2).head(wLen - radixLog2 - 1) ## qdsSign0,
        Mux1H(qHigh, dividerHMap)
      )
    )
    val csa1 = addition.csa.c32(
      VecInit(
        csa0(1).head(wLen - radixLog2),
        leftShift(csa0(0), 1).head(wLen - radixLog2 - 1) ## qdsSign1,
        Mux1H(qLow, dividerLMap)
      )
    )
    partialReminderSumNext := Mux(input.fire, input.bits.dividend, csa1(1) << radixLog2)
    partialReminderCarryNext := Mux(input.fire, 0.U, csa1(0) << 1 + radixLog2)
  } else if (a == 5) {
    val qHigh:    UInt = selectedQuotientOH(7, 5)
    val qLow:     UInt = selectedQuotientOH(4, 0)
    val qdsSign0: Bool = qHigh.head(1).asBool
    val qdsSign1: Bool = qLow.head(2).orR

    // csa for SRT8 -> CSA32+CSA32
    val dividerHMap = VecInit((-1 to 1).map {
      case -1 => divider << 2 // -4
      case 0  => 0.U //  0
      case 1  => Fill(2, 1.U(1.W)) ## ~(divider << 2) // 4
    })
    val csa0 = addition.csa.c32(
      VecInit(
        leftShift(partialReminderSum, radixLog2).head(wLen - radixLog2),
        leftShift(partialReminderCarry, radixLog2).head(wLen - radixLog2 - 1) ## qdsSign0,
        Mux1H(qHigh, dividerHMap)
      )
    )
    val csa1 = addition.csa.c32(
      VecInit(
        csa0(1).head(wLen - radixLog2),
        leftShift(csa0(0), 1).head(wLen - radixLog2 - 1) ## qdsSign1,
        Mux1H(qLow, dividerLMap)
      )
    )
    partialReminderSumNext := Mux(input.fire, input.bits.dividend, csa1(1) << radixLog2)
    partialReminderCarryNext := Mux(input.fire, 0.U, csa1(0) << 1 + radixLog2)
  } else if (a == 4) {
    val qHigh:    UInt = selectedQuotientOH(7, 5)
    val qLow:     UInt = selectedQuotientOH(4, 0)
    val qdsSign0: Bool = qHigh.head(1).asBool
    val qdsSign1: Bool = qLow.head(2).orR

    // csa for SRT8 -> CSA32+CSA32
    val dividerHMap = VecInit((-1 to 1).map {
      case -1 => divider << 1 // -2
      case 0  => 0.U //  0
      case 1  => Fill(radixLog2, 1.U(1.W)) ## ~(divider << 1) // 2
    })
    val csa0 = addition.csa.c32(
      VecInit(
        leftShift(partialReminderSum, radixLog2).head(wLen - radixLog2),
        leftShift(partialReminderCarry, radixLog2).head(wLen - radixLog2 - 1) ## qdsSign0,
        Mux1H(qHigh, dividerHMap)
      )
    )
    val csa1 = addition.csa.c32(
      VecInit(
        csa0(1).head(wLen - radixLog2),
        leftShift(csa0(0), 1).head(wLen - radixLog2 - 1) ## qdsSign1,
        Mux1H(qLow, dividerLMap)
      )
    )
    partialReminderSumNext := Mux(input.fire, input.bits.dividend, csa1(1) << radixLog2)
    partialReminderCarryNext := Mux(input.fire, 0.U, csa1(0) << 1 + radixLog2)
  }

  dividerNext := Mux(input.fire, input.bits.divider, divider)
  counterNext := Mux(input.fire, input.bits.counter, counter - 1.U)
  quotientNext := Mux(input.fire, 0.U, otf(0))
  quotientMinusOneNext := Mux(input.fire, 0.U, otf(1))
}
