package std

import Callable
import Interpreter

class Epoch : Callable {
    override fun arity(): Int {
        return 0
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        return System.currentTimeMillis().toDouble()
    }
}
