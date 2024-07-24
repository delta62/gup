package std

import Callable
import Interpreter
import types.FunctionType
import types.GupList
import types.Type
import types.TypeSource

class Args : Callable {
    companion object : FunctionType {
        override fun name() = "args"

        override fun type(): Type.Function {
            val params = emptyList<Type>()
            val returnType = Type.List(TypeSource.Hardcoded, Type.String(TypeSource.Hardcoded))
            return Type.Function(TypeSource.Hardcoded, params, returnType)
        }

    }

    override fun arity() = 0

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        val list: ArrayList<Any> = ArrayList(Argv.get())
        return GupList(list)
    }
}
