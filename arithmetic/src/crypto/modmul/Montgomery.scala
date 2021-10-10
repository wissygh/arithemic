package crypto.modmul

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.{Counter, Log2, Mux1H, log2Ceil}

class Montgomery(pWidth: Int = 4096, addCycles: Int = 2) extends Module {
  val p = IO(Input(UInt(pWidth.W)))
  val pPrime = IO(Input(Bool()))
  val a = IO(Input(UInt(pWidth.W)))
  val b = IO(Input(UInt(pWidth.W)))
  val bp = IO(Input(UInt((pWidth + 1).W)))
  val out = IO(Output(UInt(pWidth.W)))

  val t = Reg(UInt((pWidth + 2).W))
  val u = Reg(Bool())
  val i = Reg(UInt(pWidth.W))

  i := Mux1H(
    ??? -> 1.U,
    ??? -> i << 1
  )

  // 4096 multicycle prefixadder
  val adder = Module(new DummyAdd(pWidth, 0))
  val addCounter = Counter(addCycles)

  object StateType extends ChiselEnum {
    val setup = Value(0.U)
    val counting = Value(1.U)
    val end = Value(2.U)
  }

  adder.a := t
  adder.b := Mux1H(
    ??? -> bp,
    ??? -> b,
    ??? -> p,
    // last select
    ??? -> -p
  )

  val nextT = Mux1H(
    Map(
      // add ready, counting
      ??? -> adder.z,
      // add ready, counting
      ??? -> t,
      // setup
      ??? -> 0
    )
  ) >> 1

  t := nextT

  u := Mux1H(
    Map(
      ??? -> (a(0)&b(0)&pPrime)(0),
      ??? -> (nextT(0) + (a & i).orR & b(0)) & pPrime,
    )
  )
  out := t

}
