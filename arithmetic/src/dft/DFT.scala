package dft

import scala.math.pow
import chisel3._
import chisel3.util.{Decoupled, RegEnable, ValidIO, log2Ceil}

// FixedPoint???
class MyComplex(width: Int) extends Bundle {
  val re: UInt = UInt(width.W)
  val im: UInt = UInt(width.W)
}
object MyComplex{
  def apply(width: Int): Unit ={}
}

class DFTInput(m: Int, width: Int) extends Bundle{
  val x = Vec(pow(m,2).toInt, MyComplex(width))
}

class DFTOutput(m: Int, width: Int) extends  Bundle{
  val xw = Vec(pow(m,2).toInt, MyComplex(width))
}

class DFT(radixLog2: Int = 1, m: Int, width: Int) extends Module{
  val input = IO(Decoupled(new DFTInput(m, width)))
  val output = IO(ValidIO(new DFTOutput(m, width)))

  val counterNext: UInt = Wire(UInt(log2Ceil(m).W))
  val xNext: Vec[UInt] = Vec(pow(m,2).toInt, MyComplex(width))
  val xMid: Vec[UInt] = Vec(pow(m,2).toInt, MyComplex(width))
  val isLastCyle: Bool = Wire(Bool())

  //  state
  val counter = RegEnable(counterNext, 0.U, input.fire || !isLastCyle )
  val xReg = RegEnable(xNext, 0.U, input.fire || !isLastCyle)

  // controlpath
  isLastCyle := !counter.orR

  // datapath

  counterNext := Mux(input.fire, m.U, counter - 1.U)
  xNext := Mux(input.fire, input.bits.x, xMid)
}