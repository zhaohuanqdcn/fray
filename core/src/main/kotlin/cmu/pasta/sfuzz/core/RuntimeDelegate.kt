package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.concurrency.SFuzzThread
import cmu.pasta.sfuzz.runtime.Delegate
import cmu.pasta.sfuzz.runtime.MemoryOpType
import cmu.pasta.sfuzz.runtime.TargetTerminateException

class RuntimeDelegate: Delegate() {

    var entered = ThreadLocal.withInitial { false }
    var skipFunctionEntered = ThreadLocal.withInitial { 0 }

    private fun checkEntered(): Boolean {
        if (entered.get()) {
            return true
        }
        entered.set(true)
        if (skipFunctionEntered.get() > 0) {
            entered.set(false)
            return true
        }
        if (Thread.currentThread() is SFuzzThread) {
            entered.set(false)
            return true
        }
        // We do not process threads created outside of application.
        if (!GlobalContext.registeredThreads.containsKey(Thread.currentThread().id)) {
            entered.set(false)
            return true
        }
        return false
    }

    override fun onThreadStart(t: Thread) {
        if (checkEntered()) return
        GlobalContext.threadStart(t)
        entered.set(false)
    }

    override fun onThreadStartDone(t: Thread) {
        if (checkEntered()) return
        GlobalContext.threadStartDone(t)
        entered.set(false)
    }

    override fun onThreadRun() {
        if (checkEntered()) return
        GlobalContext.threadRun()
        entered.set(false)
    }

    override fun onThreadEnd() {
        if (checkEntered()) return
        GlobalContext.threadCompleted(Thread.currentThread())
        entered.set(false)
    }

    override fun onObjectWait(o: Any) {
        if (checkEntered()) return
        GlobalContext.objectWait(o)
        entered.set(false)
    }

    override fun onObjectWaitDone(o: Any) {
        if (checkEntered()) return
        GlobalContext.objectWaitDone(o)
        entered.set(false)
    }

    override fun onObjectNotify(o: Any) {
        if (checkEntered()) return
        GlobalContext.objectNotify(o)
        entered.set(false)
    }

    override fun onReentrantLockUnlockDone(l: Any) {
        if (checkEntered()) return
        GlobalContext.reentrantLockUnlockDone(l)
        entered.set(false)
    }


    override fun onObjectNotifyAll(o: Any) {
        if (checkEntered()) return
        GlobalContext.objectNotifyAll(o)
        entered.set(false)
    }

    override fun onReentrantLockLock(l: Any) {
        if (checkEntered()) return
        GlobalContext.reentrantLockLock(l)
        entered.set(false)
    }

    override fun onReentrantLockTryLock(l: Any) {
        if (checkEntered()) return
        GlobalContext.reentrantLockTrylock(l)
        entered.set(false)
    }

    override fun onReentrantLockUnlock(l: Any) {
        if (checkEntered()) return
        GlobalContext.reentrantLockUnlock(l)
        entered.set(false)
    }

    override fun onAtomicOperation(o: Any, type: MemoryOpType) {
        if (checkEntered()) return
        GlobalContext.atomicOperation(o, type)
        entered.set(false)
    }

    override fun onUnsafeOperation() {
        if (checkEntered()) return
//        GlobalContext.memoryOperation(null, MemoryOperation.Type.UNSAFE)
        entered.set(false)
    }

    override fun onFieldRead(o: Any, owner: String, name: String, descriptor: String) {
        if (checkEntered()) return
        GlobalContext.fieldOperation(o, owner, name, MemoryOpType.MEMORY_READ)
        entered.set(false)
    }

    override fun onFieldWrite(o: Any, owner: String, name: String, descriptor: String) {
        if (checkEntered()) return
        GlobalContext.fieldOperation(o, owner, name, MemoryOpType.MEMORY_WRITE)
        entered.set(false)
    }


    override fun onStaticFieldRead(owner: String, name: String, descriptor: String) {
        if (checkEntered()) return
        GlobalContext.fieldOperation(null, owner, name, MemoryOpType.MEMORY_READ)
        entered.set(false)
    }

    override fun onStaticFieldWrite(owner: String, name: String, descriptor: String) {
        if (checkEntered()) return
        GlobalContext.fieldOperation(null, owner, name, MemoryOpType.MEMORY_WRITE)
        entered.set(false)
    }

    override fun onExit(status: Int) {
        throw TargetTerminateException(status)
    }

    override fun onYield() {
        if (checkEntered()) return
        GlobalContext.scheduleNextOperation(true)
        entered.set(false)
    }

    override fun onLoadClass() {
        skipFunctionEntered.set(1 + skipFunctionEntered.get())
    }

    override fun onLoadClassDone() {
        skipFunctionEntered.set(skipFunctionEntered.get() - 1)
    }

    override fun onThreadPark() {
        if (checkEntered()) return
        GlobalContext.threadPark()
        entered.set(false)
    }

    override fun onThreadParkDone() {
        if (checkEntered()) return
        GlobalContext.threadParkDone()
        entered.set(false)
    }

    override fun onThreadUnpark(t: Thread) {
        if (checkEntered()) return
        GlobalContext.threadUnpark(t)
        entered.set(false)
    }

    override fun onThreadUnparkDone(t: Thread) {
        if (checkEntered()) return
        GlobalContext.threadUnparkDone(t)
        entered.set(false)
    }

    override fun start() {
        // For the first thread, it is not registered.
        // Therefor we cannot call `checkEntered` here.
        try {
            entered.set(true)
            GlobalContext.start()
            entered.set(false)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }
}