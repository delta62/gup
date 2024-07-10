import error.RuntimeError

class TypeCheck {
    companion object {
        fun checkIntegerOperand(operator: Token, right: Any): Int {
            val r = checkNumberOperand(operator, right)
            if (r % 1 == 0.0) return r.toInt()
            throw RuntimeError(operator, "Operand must be an integer")
        }

        fun checkBooleanOperand(operator: Token, operand: Any): Boolean {
            if (operand is Boolean) return operand
            throw RuntimeError(operator, "Operand must be a boolean")
        }

        fun checkNumberOperand(operator: Token, operand: Any): Double {
            if (operand is Double) return operand
            throw RuntimeError(operator, "Operand must be a number")
        }

        fun checkNumberOperands(operator: Token, left: Any, right: Any): Pair<Double, Double> {
            if (left is Double && right is Double) return Pair(left, right)
            throw RuntimeError(operator, "Operands must be numbers")
        }
    }
}
