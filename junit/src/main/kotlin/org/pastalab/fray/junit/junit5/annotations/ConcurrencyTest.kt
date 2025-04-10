package org.pastalab.fray.junit.junit5.annotations

import kotlin.reflect.KClass
import org.junit.jupiter.api.TestTemplate
import org.pastalab.fray.core.scheduler.POSScheduler
import org.pastalab.fray.core.scheduler.Scheduler

@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
@Retention(
    AnnotationRetention.RUNTIME,
)
@TestTemplate
annotation class ConcurrencyTest(
    val iterations: Int = 1000,
    val scheduler: KClass<out Scheduler> = POSScheduler::class,
    val name: String = SHORT_DISPLAY_NAME,
    val replay: String = ""
) {
  companion object {

    private const val CURRENT_REPETITION_PLACEHOLDER: String = "{currentRepetition}"

    private const val TOTAL_REPETITIONS_PLACEHOLDER: String = "{totalRepetitions}"

    const val SHORT_DISPLAY_NAME =
        "repetition $CURRENT_REPETITION_PLACEHOLDER of $TOTAL_REPETITIONS_PLACEHOLDER"
  }
}
