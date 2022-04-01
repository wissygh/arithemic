package division.srt

import chisel3._

trait RadixSRT extends LazyLogging {

    // def init(x: Seq[Bool], d: Seq[Bool]): Seq[(Bool, Bool)]

    def qSEL(rws: Seq[Bool], rwc: Seq[Bool]): Seq[Bool]

    def divisorMultipler(a: Int, width: Int, radixLog2: Int, q_next: SInt, d: SInt): SInt

    // def csa32(ws: Seq[Bool], wc: Seq[Bool], c: Seq[Bool]) : Seq[(Bool, Bool)]

    def sz(wsc_next: Seq[(Bool, Bool)]): (Bool, Bool)

    def onthefly(radix: Int, radixLog2: Int, q: Seq[Bool]): Seq[Bool]

    def apply(x: Seq[Bool], d: Seq[Bool]): Vector[(Bool, Bool)]
}

class SRTDivider(val width: Int, radixSRT: RadixSRT) extends Divider {

    // Split up bit vectors into individual bits.  dividend,divisor => x，d
    val x: Seq[Bool] = dividend.asBools
    val d: Seq[Bool] = divisor.asBools

    val qrs: Vector[(Bool, Bool)] = radixSRT(x,d)

    // get q,r     qrs => q,r
    val q: UInt = qrs.map(_._1).asUInt  
    val r: UInt = qrs.map(_._2).asUInt
    quotient := q
    remainder:= r
}
