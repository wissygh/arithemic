package divisoin.srt.commom
// 带csa的srt除法器 1/2<= d < 1, 1/2 < rho <=1, 0 < q  < 2
// 0, radix = 4
// 1，商数范围 :a = 2, {-2, -1, 0, 1, -2},
// 2, 冗余因子rho = 2/(4-1) =2/3
// 3，估值(截断位宽)：3位整数，4位小数 t = 4
// 4，选商函数 通过输入的y^（xxx.xxxx）和截断的d（0.1xxx）来进行选商，计算出选商查找表，来进行查找选商[d_i,d_i+1)
//            2 : [ 24/16,  42/16]
//            1 : [  8/16,  23/16]
//            0 : [ -7/16,   7/16]
//            -1: [-23/16,  -8/16]
//            -2: [-44/16, -24/16]

object Radix4SRT extends CommonRadixSRT{
  def apply(xq: Seq[(Bool, Bool)]): Vector[(Bool, Bool)] ={

  }
}

class Radix4SRTDvider(width: Int) extends SRTDivider(width, Radix4SRT)