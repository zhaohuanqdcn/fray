package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.logger.ConsoleLogger
import cmu.pasta.sfuzz.core.runtime.AnalysisResult
import cmu.pasta.sfuzz.runtime.Delegate
import cmu.pasta.sfuzz.runtime.Runtime
import java.lang.reflect.InvocationTargetException
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
fun prepareReportPath(reportPath: String) {
  val path = Paths.get(reportPath)
  path.deleteRecursively()
  path.createDirectories()
}

fun run(config: Configuration) {
  println("Start analysing ${config.clazz}:main")
  println("Report is available at: ${config.report}")
  prepareReportPath(config.report)
  GlobalContext.registerLogger(config.logger)
  GlobalContext.registerLogger(ConsoleLogger())
  GlobalContext.scheduler = config.scheduler
  GlobalContext.config = config
  GlobalContext.bootstrap()
  for (i in 0 ..< config.iter) {
    println("Starting iteration $i")
    try {
      Runtime.DELEGATE = RuntimeDelegate()
      Runtime.start()
      val clazz = Class.forName(config.clazz)
      if (config.targetArgs.isEmpty() && config.method != "main") {
        val m = clazz.getMethod(config.method)
        m.invoke(null)
      } else {
        val m = clazz.getMethod(config.method, Array<String>::class.java)
        m.invoke(null, config.targetArgs.split(" ").toTypedArray())
      }
      Runtime.onMainExit()
    } catch (e: InvocationTargetException) {
      GlobalContext.errorFound = true
      Runtime.onMainExit()
      println(e.cause)
    }
    Runtime.DELEGATE = Delegate()
    GlobalContext.done(AnalysisResult.COMPLETE)
    if (GlobalContext.errorFound) {
      println("error found at iter: $i")
      break
    }
  }
  GlobalContext.shutDown()
  println("Analysis done!")
}

fun main(args: Array<String>) {
  val config = ConfigurationCommand().apply { main(args) }.toConfiguration()
  run(config)
}
