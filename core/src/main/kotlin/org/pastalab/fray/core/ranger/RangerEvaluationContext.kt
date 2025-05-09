package org.pastalab.fray.core.ranger

import java.util.concurrent.CountDownLatch
import org.pastalab.fray.core.RunContext

class RangerEvaluationContext(val runContext: RunContext) {
  fun latchAwait(latch: CountDownLatch) {
    val context = runContext.latchManager.getContext(latch)
    if (context.count > 0) {
      throw AbortEvaluationException("Abort ranger condition evaluation because of deadlock.")
    }
  }

  fun lockImpl(
      lock: Any,
  ) {
    val lockContext = runContext.lockManager.getContext(lock)
    if (!lockContext.canLock(Thread.currentThread().id)) {
      throw AbortEvaluationException("Abort ranger condition evaluation because of deadlock.")
    } else {
      return
    }
  }
}
