package cmu.pasta.fray.core

import cmu.pasta.fray.core.concurrency.Sync
import cmu.pasta.fray.core.concurrency.operations.Operation
import cmu.pasta.fray.core.concurrency.operations.ThreadStartOperation

enum class ThreadState {
  Enabled,
  Running,
  Paused,
  Completed,
}

class ThreadContext(val thread: Thread, val index: Int) {
  var state = ThreadState.Paused
  var unparkSignaled = false
  var interruptSignaled = false
  var isExiting = false

  // Pending operation is null if a thread is just resumed/blocked.
  var pendingOperation: Operation = ThreadStartOperation()
  val sync = Sync(1)

  fun block() {
    sync.block()
  }

  fun schedulable() = state == ThreadState.Enabled || state == ThreadState.Running

  fun unblock() {
    sync.unblock()
  }

  fun checkInterrupt() {
    if (interruptSignaled) {
      interruptSignaled = false
      Thread.interrupted()
      throw InterruptedException()
    }
  }
}
