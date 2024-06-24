package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
import kotlin.reflect.KFunction
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter

class TimedWaitInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {

  fun findRuntimeMethod(owner: String, name: String, descriptor: String): KFunction<*>? {
    if (owner == "java/util/concurrent/locks/LockSupport" &&
        (name == "parkNanos" || name == "parkUntil")) {
      return if (name == "parkNanos") {
        if (descriptor == "(J)V") {
          Runtime::onThreadParkNanos
        } else {
          Runtime::onThreadParkNanosWithBlocker
        }
      } else {
        if (descriptor == "(J)V") {
          Runtime::onThreadParkUntil
        } else {
          Runtime::onThreadParkUntilWithBlocker
        }
      }
    }
    if (owner == "java/util/concurrent/locks/Condition" &&
        name.contains("await") &&
        descriptor != "()V") {
      return if (name == "await") {
        Runtime::onConditionAwaitTime
      } else if (name == "awaitNanos") {
        Runtime::onConditionAwaitNanos
      } else {
        Runtime::onConditionAwaitUntil
      }
    }
    if (owner == "java/util/concurrent/CountDownLatch" &&
        name == "await" &&
        descriptor == "(JLjava/util/concurrent/TimeUnit;)Z") {
      return Runtime::onLatchAwaitTimeout
    }
    return null
  }

  override fun visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    return object :
        GeneratorAdapter(
            ASM9,
            super.visitMethod(access, name, descriptor, signature, exceptions),
            access,
            name,
            descriptor) {
      override fun visitMethodInsn(
          opcode: Int,
          owner: String,
          name: String,
          descriptor: String,
          isInterface: Boolean
      ) {
        findRuntimeMethod(owner, name, descriptor)?.let {
          invokeStatic(
              Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
              Utils.kFunctionToASMMethod(it),
          )
        } ?: super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      }
    }
  }
}
