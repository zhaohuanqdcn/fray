package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import kotlin.reflect.KFunction
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.commons.AdviceAdapter

class MethodEnterVisitor(
    mv: MethodVisitor,
    val method: KFunction<*>,
    access: Int,
    name: String,
    descriptor: String,
    val loadThis: Boolean,
    val loadArgs: Boolean,
    val customizer: MethodEnterVisitor.(v: MethodEnterVisitor) -> Unit = {}
) : AdviceAdapter(ASM9, mv, access, name, descriptor) {
  override fun visitCode() {
    super.visitCode()
    if (loadThis) {
      loadThis()
    }
    if (loadArgs) {
      loadArgs()
    }
    customizer(this)
    visitMethodInsn(
        Opcodes.INVOKESTATIC,
        Runtime::class.java.name.replace(".", "/"),
        method.name,
        Utils.kFunctionToJvmMethodDescriptor(method),
        false)
  }
}
