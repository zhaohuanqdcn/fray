package cmu.pasta.sfuzz.core.concurrency.operations

import cmu.pasta.sfuzz.runtime.MemoryOpType


class MemoryOperation(obj: Int, type: MemoryOpType): ConflictingOperation(obj, type) {
}