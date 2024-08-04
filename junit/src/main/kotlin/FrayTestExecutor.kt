package cmu.edu.pasta.fray.junit

import cmu.pasta.fray.core.TestRunner
import cmu.pasta.fray.core.command.Configuration
import cmu.pasta.fray.core.command.ExecutionInfo
import cmu.pasta.fray.core.command.LambdaExecutor
import cmu.pasta.fray.core.logger.JsonLogger
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.EngineDescriptor

class FrayTestExecutor {
  fun execute(request: ExecutionRequest, descriptor: TestDescriptor) {
    if (descriptor is EngineDescriptor) {
      executeContainer(request, descriptor)
    }
    if (descriptor is ClassTestDescriptor) {
      executeContainer(request, descriptor)
    }
    if (descriptor is MethodTestDescriptor) {
      executeTest(request, descriptor)
    }
  }

  fun executeTest(request: ExecutionRequest, descriptor: MethodTestDescriptor) {
    request.engineExecutionListener.executionStarted(descriptor)
    val testInstance = descriptor.parent.testClass.getDeclaredConstructor().newInstance()
    val testMethod = descriptor.testMethod
    val workDir = createTempDirectory(WORK_DIR).absolutePathString()
    val jsonLogger = JsonLogger(workDir, false)
    val eventLogger = EventLogger()
    val config =
        Configuration(
            ExecutionInfo(
                LambdaExecutor { testMethod.invoke(testInstance) },
                false,
                true,
                false,
                -1,
            ),
            workDir,
            descriptor.analyzeConfig.iteration,
            descriptor.getScheduler(),
            false,
            listOf(jsonLogger, eventLogger),
            false,
            true,
            false,
        )
    val runner = TestRunner(config)
    val result = runner.run()
    verifyTestResult(request, descriptor, result, eventLogger, workDir, jsonLogger)
  }

  fun verifyTestResult(
      request: ExecutionRequest,
      descriptor: MethodTestDescriptor,
      result: Throwable?,
      logger: EventLogger,
      workDir: String,
      jsonLogger: JsonLogger
  ) {
    var testResult = TestExecutionResult.successful()
    if (result != null) {
      if (descriptor.analyzeConfig.expectedException.java != result.javaClass) {
        testResult = TestExecutionResult.failed(result)
      }
    } else {
      if (descriptor.analyzeConfig.expectedException != Any::class) {
        testResult =
            TestExecutionResult.failed(
                RuntimeException(
                    "Expected exception not thrown: ${descriptor.analyzeConfig.expectedException.simpleName}"))
      }
    }
    if (descriptor.analyzeConfig.expectedLog != "" &&
        !logger.sb.toString().contains(descriptor.analyzeConfig.expectedLog)) {
      jsonLogger.saveLogs()
      testResult =
          TestExecutionResult.failed(
              RuntimeException("Expected log not found: ${descriptor.analyzeConfig.expectedLog}"))
    }
    if (testResult == TestExecutionResult.successful()) {
      File(workDir).deleteRecursively()
    } else {
      println("Test: ${descriptor.uniqueId} failed: report can be found at: $workDir")
    }
    request.engineExecutionListener.executionFinished(descriptor, testResult)
  }

  fun executeContainer(request: ExecutionRequest, container: TestDescriptor) {
    request.engineExecutionListener.executionStarted(container)
    container.children.forEach { execute(request, it) }
    request.engineExecutionListener.executionFinished(container, TestExecutionResult.successful())
  }

  companion object {
    val WORK_DIR = Paths.get(System.getProperty("fray.workDir", "fray/fray-report"))

    init {
      WORK_DIR.toFile().mkdirs()
    }
  }
}
