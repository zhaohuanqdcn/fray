package cmu.pasta.fray.instrumentation

import java.io.File

fun main(args: Array<String>) {
  var ba = File(args[0]).inputStream()

  instrumentClass(args[0], ba)
  //  val appTransformer = ApplicationCodeTransformer()
  //  appTransformer.transform(null, "", null, null, ba.readBytes())
}
