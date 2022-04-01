package divisoin.srt.common

import utils._
import addition.csa
import chisel3._
import division.srt.SRTDivider

// 带csa的srt除法器 1/2<= d < 1, 1/2 < rho <=1, 0 < q < 2
// 0, radix = 2
// 1，商数范围 :a = 1, {-1 ,0 ,1},
// 2, 冗余因子rho = 1/(2-1) =1 
// 3，估值(截断位宽)：3位整数，1位小数 t =1
// 4，选择常数 1 : 0<y^<3/2
//           0 : y^ = -1/2
//           -1: -5/2< y^ < -1
// 5，init：2w[0] = 2(x/2) = x
// 问题：余数和商的精度怎么确定,或者说要递归多少次？

object Radix2SRT extends CommonRadixSRT{

  def qSEL(rws: Seq[Bool], rwc: Seq[Bool]): Seq[Bool] = {
    val yEstimate: SInt = (rws.asUInt.head(4) + rwc.asUInt.head(4)).asSInt  // prefixadder??
    when(yEstimate >= 0.S && yEstimate <= 3.S){
      Seq(false.B, true.B)
    }.elsewhen(yEstimate === -1.S){
      Seq(false.B, false.B)
    }.otherwise{
      Seq(true.B, true.B)
    }
  }

  def onthefly(q_next: Seq[Bool]): Seq[Bool] = { 

  }

  def apply(x: Seq[Bool], d: Seq[Bool]): Vector[(Bool, Bool)] ={
      // init，w[0] = x/2, 2*w[0] = x, q_comp = q/2
    val rws: Seq[Bool] = x +: Seq.fill(2)(if (x(length-1) == true.B) true.B else false.B ) 
    val rwc: Seq[Bool] = Seq.fill(rws.length)(false.B)

    def helper(rws: Seq[Bool], rwc: Seq[Bool]): Vector[(Bool, Bool)]={

      // 递归的终止条件是啥？
      if(true){
        q.zip(r)
      }
      // qSEL: input(ws_j,wc_j|4) 2*4bit, output(q_j+1) 2bitq_j+1 = def(WS_j<<1, WC_j <<1)
      val q_next: SInt  = qSEL(rws, rwc)

      // multiplierc = dq_j+1
      val divisorMulti: SInt = divisorMultipler(a, width, radixLog2, q_next, d.asSInt)

      // csa: Input(ws_j,wc_j,q_j+1*d) 3*(width +2)bit Output: ws_j+1, wc_j+1
      val adderabc = rws.zip(Mux(q_next>0 ,rwc + 1.U, rwc)).zip(divisorMulti)
      val c32 =addition.csa.c32(adderabc).map(_.asBool).reverse

      // convert,SZ: input q_j+1, ws_j+1, wc_j+1  
      val q: Seq[Bool] = onthefly(q_next)
      // q,sz = def()

      //   shift  w[j] -> rw[j] ->ws_j,wc_j
      val shiftrws : Seq[Bool] = false.B +: Seq(c32(0))
      val shiftrwc : Seq[Bool] = false.B +: Seq(c32(1))
      helper(shiftrws, shiftrwc)
    }
    helper(rws, rwc)
  }
}

class Radix2SRTDvider(width: Int) extends SRTDivider(width, Radix2SRT)
