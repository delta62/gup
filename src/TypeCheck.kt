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
    }
}
