package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.logger.CsvLogger
import cmu.pasta.sfuzz.runtime.Runtime
import cmu.pasta.sfuzz.core.logger.JsonLogger
import cmu.pasta.sfuzz.core.runtime.AnalysisResult
import cmu.pasta.sfuzz.runtime.TargetTerminateException
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
    val clazz = Class.forName(config.clazz)
    val m = clazz.getMethod("main", Array<String>::class.java)
    val logger = CsvLogger(config.report, config.fullSchedule)
    GlobalContext.registerLogger(logger)
    GlobalContext.scheduler = config.scheduler
    GlobalContext.config = config
    GlobalContext.bootStrap()
    Runtime.DELEGATE = RuntimeDelegate()
    for (i in 0..<config.iter) {
        Runtime.start()
        try {
            m.invoke(null, config.targetArgs.split(" ").toTypedArray())
        } catch (e: InvocationTargetException) {
            if (e.cause is TargetTerminateException) {

                println("target terminated: ${(e.cause as TargetTerminateException).status}")
            } else {
                println(e.toString())
                e.cause?.printStackTrace()
            }
        }
        GlobalContext.done(AnalysisResult.COMPLETE)
    }
    GlobalContext.shutDown()
    println("Analysis done!")
}


fun main(args: Array<String>) {
    val config = ConfigurationCommand().apply { main(args) }.toConfiguration()
    run(config)
}
