package division.srt

import chisel3._

package object srtdivider{
    def apply(
        radixSRT     : RadixSRT,
        width        : Option[Int] = None,
        hasSign      : Boolean = false
    )(
        dividend     : UInt,
        divisor      : UInt
    ) = {
        val w = width.getOrElse(Seq(dividend, divisor).flatMap(_.widthOption).max)
        val m = Module(new SRTDivider(w, radixDivider))
        m.dividend := dividend
        m.divisor  := divisor
        
        m.quotient ## m.remainder
    }

    def radix2(dividend: UInt, divisor: UInt, width: Option[Int] = None, hasSign: Boolean = false) =
    apply(Radix2SRT, width, hasSign)(dividend, divisor)

    def radix4(dividend: UInt, divisor: UInt, width: Option[Int] = None, hasSign: Boolean = false) =
    apply(Radix4SRT, width, hasSign)(dividend, divisor)

    def radix8(dividend: UInt, divisor: UInt, width: Option[Int] = None, hasSign: Boolean = false) =
    apply(Radix8SRT, width, hasSign)(dividend, divisor)
}