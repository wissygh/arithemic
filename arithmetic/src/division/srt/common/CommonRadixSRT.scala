package division.srt.common

import division.srt.RadixSRT
import chisel3._

trait CommonRadixSRT extends RadixSRT{

  // produce Seq(-ad, (-a+1)d, ... , ad)
  def divisorMultipler(a: Int, width: Int, radixLog2: Int, q_next: SInt, d: SInt): SInt = {
    val bMultipleWidth = (width + radixLog2).W
    def prepareBMultiples(digits: Int): Seq[SInt] = {
        if (digits == 0) {
            Seq(extend(d, bMultipleWidth.get, signed).asSInt)
        } else {
            val lowerMultiples = prepareBMultiples(digits - 1)
            val bPower2 = extend(d << (digits - 1), bMultipleWidth.get, signed)
            val higherMultiples = lowerMultiples.dropRight(1).map { m =>
            addition.prefixadder.apply(sumUpAdder)(bPower2.asUInt, m.asUInt)(bMultipleWidth.get - 1, 0)
            } :+ (bPower2 << 1)(bMultipleWidth.get - 1, 0)
            lowerMultiples ++ higherMultiples.map(_.asSInt)
        }
    }
    val bMultiples = prepareBMultiples(radixLog2 - 1)
    val encodedWidth = (radixLog2 + 1).W
    val partialProductLookupTable: Seq[(UInt, SInt)] = Range(-a, a).map {
      case 0 =>
        0.U(encodedWidth) -> 0.S(bMultipleWidth)
      case i if i > 0 =>
        i.U(encodedWidth) -> bMultiples(i - 1)
      case i if i < 0 =>
        i.S(encodedWidth).asUInt -> (~bMultiples(-i - 1)).asSInt
    }
    MuxLookup(q_next, 0.S(bMultipleWidth), partialProductLookupTable).asUInt
  }

    def sz(y : Seq[(Bool, Bool)]) :(Bool, Bool) = {
        val psc : Seq[(Bool,Bool)] = y.map{case(ws,wc) => (~(ws ^ wc),(ws | wc))}
        val ps  : Seq[Bool] = false.B +: psc.map(_(0))
        val pc  : Seq[Bool] = psc.map(_(1))
        val p   : Seq[Bool] = ps.zip(pc).map{case(ps,pc) => ps ^ pc}
        val zero   : Bool      = p0 P_out;
        val sign   : Bool      = (p0 ^ G_out) & (~zero)
        (zero,sign)
    }

    def onthefly(radix: Int, radixLog2: Int, q: Seq[Bool]): Seq[Bool] = {
        val q_next: SInt = q.asSInt
        val cShiftQ : Bool = (q_next >= 0.S)
        val cShiftQM: Bool = (q_next >  0.S)

        val qIn  = Mux(cShiftQ, q_next, (radix.S + q_next))
        val qmIn = Mux(cShiftQM, (q_next - 1.S), (radix-1).S + q_next )

        val Q  =  RegEable(0.U, Mux(cShiftQ, Q<<radixLog2 + qIn, QM<<radixLog2 + qIn), true)
        val QM =  RegEable(0.U, Mux(cShiftQM, QM<<radixLog2 + qmIn, QM<<radixLog2 + qmIn), true)
        Q
    }

}