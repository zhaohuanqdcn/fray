package cmu.pasta.sfuzz.core.concurrency.locks

import java.util.concurrent.Semaphore

class SemaphoreManager {
    val lockContextManager = ReferencedContextManager {it ->
        if (it is Semaphore) {
            SemaphoreContext(it.availablePermits())
        } else {
            throw IllegalArgumentException("SemaphoreManager can only manage Semaphore objects")
        }
    }

    fun init(sem: Semaphore) {
        val context = SemaphoreContext(sem.availablePermits())
        lockContextManager.addContext(sem, context)
    }

    fun acquire(sem: Semaphore, permits: Int, shouldBlock: Boolean): Boolean {
        return lockContextManager.getLockContext(sem).acquire(permits, shouldBlock)
    }

    fun release(sem: Semaphore, permits: Int) {
        lockContextManager.getLockContext(sem).release(permits)
    }

    fun drainPermits(sem: Semaphore): Int {
        return lockContextManager.getLockContext(sem).drainPermits()
    }

    fun reducePermits(sem: Semaphore, permits: Int) {
        lockContextManager.getLockContext(sem).reducePermits(permits)
    }
}