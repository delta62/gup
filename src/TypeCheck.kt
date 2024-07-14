import error.RuntimeError

class TypeCheck {
    companion object {
        fun checkIntegerOperand(operator: Token, right: Any): Long {
            return checkNumberOperand<Long>(operator, right)
        }

        fun checkBooleanOperand(operator: Token, operand: Any): Boolean {
            if (operand is Boolean) return operand
            throw RuntimeError(operator, "Operand must be a boolean")
        }

        inline fun <reified T: Number> checkNumberOperand(operator: Token, operand: Any): T {
            if (operand is T) return operand
            throw RuntimeError(operator, "Operand must be a number")
        }

        inline fun <reified T: Number> checkNumberOperands(operator: Token, left: Any, right: Any): Pair<T, T> {
            if (left is T && right is T) return Pair(left, right)
            throw RuntimeError(operator, "Operands must be numbers")
        }
    }
}
