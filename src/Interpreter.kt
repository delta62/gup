import TokenType.*
import error.Break
import error.Continue
import error.RuntimeError
import error.Unreachable
import std.*

class Interpreter : Expr.Visitor<Any> {
    private val globals = Environment()
    private var environment = globals
    private var loopState = LoopState.NoLoop
    private var locals = HashMap<Expr, Int>()

    init {
        globals.define("assertEqual", AssertEqual())
        globals.define("epoch", Epoch())
        globals.define("next", Next())
        globals.define("println", PrintLine())
    }

    fun interpret(statements: List<Expr>) {
        try {
            for (statement in statements) {
                evaluate(statement)
            }
        } catch (error: RuntimeError) {
            Gup.runtimeError(error)
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any {
        val value = evaluate(expr.value)
        val distance = locals[expr]

        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }

        return value
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            MINUS -> left as Double - right as Double
            PLUS -> {
                if (left is Double && right is Double) {
                    left + right
                } else if (left is String && right is String) {
                    left + right
                } else {
                    throw Unreachable()
                }
            }

            SLASH -> {
                val (l, r) = checkNumberOperands(expr.operator, left, right)
                l / r
            }

            STAR -> {
                val (l, r) = checkNumberOperands(expr.operator, left, right)
                l * r
            }

            PERCENT -> {
                val (l, r) = checkNumberOperands(expr.operator, left, right)
                l % r
            }

            DOT -> {
                val lName = (expr.left as Expr.Variable).name
                val rName = (expr.right as Expr.Variable).name

                val l = environment.get(lName) as Function
                val r = environment.get(rName) as Function

                return object : Callable {
                    override fun arity(): Int {
                        return l.arity()
                    }

                    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
                        val rResult = r.call(interpreter, arguments)
                        return l.call(interpreter, listOf(rResult))
                    }
                }
            }

            DOT_DOT, DOT_DOT_EQ -> {
                var (min, max) = checkIntegerOperands(expr.operator, left, right)
                if (expr.operator.type == DOT_DOT_EQ) { max += 1 }
                Range(min, max)
            }

            AMPERSAND -> {
                val (l, r) = checkIntegerOperands(expr.operator, left, right)
                l.and(r).toDouble()
            }

            PIPE -> {
                val (l, r) = checkIntegerOperands(expr.operator, left, right)
                l.or(r).toDouble()
            }

            CARET -> {
                val (l, r) = checkIntegerOperands(expr.operator, left, right)
                l.xor(r).toDouble()
            }

            GREATER -> {
                val (l, r) = checkNumberOperands(expr.operator, left, right)
                l > r
            }

            GREATER_GREATER -> {
                val (l, r) = checkIntegerOperands(expr.operator, left, right)
                l.shr(r).toDouble()
            }

            GREATER_EQ -> {
                val (l, r) = checkNumberOperands(expr.operator, left, right)
                l >= r
            }

            LESS -> {
                val (l, r) = checkNumberOperands(expr.operator, left, right)
                l < r
            }

            LESS_EQ -> {
                val (l, r) = checkNumberOperands(expr.operator, left, right)
                l <= r
            }

            LESS_LESS -> {
                val (l, r) = checkIntegerOperands(expr.operator, left, right)
                l.shl(r).toDouble()
            }

            ISNT -> left != right
            IS -> left == right
            else -> throw Unreachable()
        }
    }

    override fun visitBreakExpr(expr: Expr.Break): Any {
        if (loopState != LoopState.InLoop) throw RuntimeError(expr.token, "Cannot break outside of loops")
        throw Break()
    }

    override fun visitContinueExpr(expr: Expr.Continue): Any {
        if (loopState != LoopState.InLoop) throw RuntimeError(expr.token, "Cannot continue outside of loops")
        throw Continue()
    }

    override fun visitCallExpr(expr: Expr.Call): Any {
        val callee = evaluate(expr.callee)
        val arguments = ArrayList<Any>()

        for (argument in expr.arguments) {
            arguments.add(evaluate(argument))
        }

        if (callee !is Callable) {
            throw RuntimeError(expr.paren, "Can only call functions")
        }

        if (arguments.size != callee.arity()) {
            throw RuntimeError(expr.paren, "Expected ${callee.arity()} arguments but got ${arguments.size}")
        }

        return callee.call(this, arguments)
    }

    override fun visitFunctionExpr(expr: Expr.Function): Any {
        val function = Function(expr, environment)

        if (expr.name != null) {
            environment.define(expr.name.lexeme, function)
        }

        return function
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any {
        return evaluate(expr.expression)
    }

    override fun visitIfExpr(expr: Expr.If): Any {
        if (evaluate(expr.condition) == true) {
            evaluateBlock(expr.thenBranch, environment)
        } else if (expr.elseBranch != null) {
            evaluateBlock(expr.elseBranch, environment)
        }

        return GUnit()
    }

    override fun visitLetExpr(expr: Expr.Let): Any {
        var value: Any? = null

        if (expr.initializer != null) {
            value = evaluate(expr.initializer)
        }

        environment.define(expr.name.lexeme, value)

        return GUnit()
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any {
        return expr.value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any {
        val left = evaluate(expr.left)

        if (expr.operator.type == OR) {
            if (left == true) return left
        } else {
            if (left == false) return left
        }

        return evaluate(expr.right)
    }

    override fun visitReturnExpr(expr: Expr.Return): Any {
        val value = evaluate(expr.value)
        throw Return(value)
    }

    override fun visitTemplateExpr(expr: Expr.Template): Any {
        val builder = StringBuilder()
        for (part in expr.parts) {
            val text = when (part) {
                is TemplateString.Expression -> evaluate(part.expression).toString()
                is TemplateString.Text -> part.text
            }
            builder.append(text)
        }

        return builder.toString()
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            NOT -> !checkBooleanOperand(expr.operator, right)
            MINUS -> -checkNumberOperand(expr.operator, right)
            PLUS -> checkNumberOperand(expr.operator, right)
            TILDE -> checkIntegerOperand(expr.operator, right).inv().toDouble()
            else -> throw Unreachable()
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any {
        return lookUpVariable(expr.name, expr)
    }

    override fun visitLoopExpr(expr: Expr.Loop): Any {
        val lastLoopState = loopState
        loopState = LoopState.InLoop

        try {
            while (evaluate(expr.condition) == true) {
                try { evaluate(expr.body) }
                catch (_: Continue) {}
                catch (_: Break) { break }
            }
        } finally {
            loopState = lastLoopState
        }

        return GUnit()
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any {
        val distance = locals[expr]
        val value = if (distance != null) {
            environment.getAt(distance, name.lexeme)
        } else {
            globals.get(name)
        }

        return value ?: throw RuntimeError(name, "Undefined variable")
    }

    private fun checkBooleanOperand(operator: Token, operand: Any): Boolean {
        if (operand is Boolean) return operand
        throw RuntimeError(operator, "Operand must be a boolean")
    }

    private fun checkNumberOperand(operator: Token, operand: Any): Double {
        if (operand is Double) return operand
        throw RuntimeError(operator, "Operand must be a number")
    }

    private fun checkNumberOperands(operator: Token, left: Any, right: Any): Pair<Double, Double> {
        if (left is Double && right is Double) return Pair(left, right)
        throw RuntimeError(operator, "Operands must be numbers")
    }

    private fun checkIntegerOperands(operator: Token, left: Any, right: Any): Pair<Int, Int> {
        val (l, r) = checkNumberOperands(operator, left, right)
        if (l % 1 == 0.0 && r % 1 == 0.0) return Pair(l.toInt(), r.toInt())
        throw RuntimeError(operator, "Operands must be integers")
    }

    private fun checkIntegerOperand(operator: Token, right: Any): Int {
        val r = checkNumberOperand(operator, right)
        if (r % 1 == 0.0) return r.toInt()
        throw RuntimeError(operator, "Operand must be an integer")
    }

    private fun evaluate(expr: Expr): Any {
        return expr.accept(this)
    }

    internal fun evaluateBlock(block: Expr.Block, environment: Environment): Any {
        val previous = this.environment
        try {
            this.environment = environment

            var ret: Any? = null
            for (expression in block.expressions) {
                if (expression is Expr.Break) break
                ret = evaluate(expression)
            }

            return ret ?: GUnit()
        } finally {
            this.environment = previous
        }
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    override fun visitBlockExpr(expr: Expr.Block) {
        evaluateBlock(expr, Environment(environment))
    }
}
