package std

import Callable
import Interpreter
import types.FunctionType
import types.Type
import types.TypeSource

class Epoch : Callable {
    companion object: FunctionType {
        override fun name() = "epoch"
        override fun type() = Type.Function(TypeSource.Hardcoded, emptyList(), Type.ULong(TypeSource.Hardcoded))
    }

    override fun arity(): Int {
        return 0
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        return System.currentTimeMillis().toDouble().toULong()
    }
}
