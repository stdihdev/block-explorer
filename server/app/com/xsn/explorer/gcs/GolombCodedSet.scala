package com.xsn.explorer.gcs

import com.xsn.explorer.models.values.HexString

class GolombCodedSet(val p: Int, val m: Int, val n: Int, val hex: HexString)

object GolombCodedSet {

  def apply(p: Int, m: Int, n: Int, data: List[UnsignedByte]): GolombCodedSet = {
    val string = data.map(_.byte).map("%02x".format(_)).mkString("")
    HexString.from(string) match {
      case Some(value) => new GolombCodedSet(p = p, m = m, n = n, hex = value)
      case None => throw new RuntimeException("Unexpected error, unable to create hex value")
    }
  }
}
