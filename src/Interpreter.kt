import TokenType.*

class Interpreter : Expr.Visitor<Any> {
    fun interpret(expression: Expr) {
        try {
            val value = evaluate(expression)
            println(stringify(value))
        } catch (error: RuntimeError) {
            SamLang.runtimeError(error)
        }
    }
    override fun visitBinaryExpr(expr: Expr.Binary): Any {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            MINUS -> left as Double - right as Double
            PLUS -> {
                if (left is Double && right is Double) {
                    left + right
                }

                if (left is String && right is String) {
                    left + right
                }

                TODO("unreachable")
            }
            SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double / right as Double
            }
            STAR -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double * right as Double
            }
            GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double > right as Double
            }
            GREATER_EQ -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double >= right as Double
            }
            LESS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) < right as Double
            }
            LESS_EQ -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double <= right as Double
            }
            BANG_EQ -> !isEqual(left, right)
            EQ_EQ -> isEqual(left, right)
            else -> TODO("unreachable")
        }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any {
        return expr.value
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            BANG -> !(right as Boolean)
            MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }
            else -> TODO("unreachable")
        }
    }

    private fun checkNumberOperand(operator: Token, operand: Any) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number")
    }

    private fun checkNumberOperands(operator: Token, left: Any, right: Any) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands must be numbers")
    }

    private fun evaluate(expr: Expr): Any {
        return expr.accept(this)
    }

    private fun isEqual(a: Any, b: Any): Boolean {
        return a == b
    }

    private fun stringify(obj: Any): String {
        if (obj is Double) {
            var text = obj.toString()
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length - 2)
                return text
            }
        }
        return obj.toString()
    }
}
