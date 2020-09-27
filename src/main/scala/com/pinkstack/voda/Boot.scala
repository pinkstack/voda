package com.pinkstack.voda

object Boot {
  def main(args: Array[String] = Array.empty): Unit = {
    if (args.contains("--archive"))
      ArchiveMain.main(args)
    else
      FetchMain.main(args)
  }
}
