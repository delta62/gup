import TokenType.*
import error.Break
import error.Continue
import error.RuntimeError
import error.Unreachable
import generated.Expr
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
        globals.define("typeof", TypeOf())
    }

    fun interpret(statements: List<Expr>) {
        try {
            for (statement in statements) evaluate(statement)
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
            AMPERSAND -> Math.bitwiseAnd(left, right)
            CARET -> Math.bitwiseXor(left, right)
            GREATER -> Math.greater(left, right)
            GREATER_EQ -> Math.greaterEq(left, right)
            GREATER_GREATER -> Math.shr(left, right)
            IS -> evaluate(expr.left) == evaluate(expr.right)
            ISNT -> evaluate(expr.left) != evaluate(expr.right)
            LESS -> Math.less(left, right)
            LESS_EQ -> Math.lessEq(left, right)
            LESS_LESS -> Math.shl(left, right)
            MINUS -> Math.sub(left, right)
            PERCENT -> Math.rem(left, right)
            PIPE -> Math.bitwiseOr(left, right)
            SLASH -> Math.div(left, right)
            STAR -> Math.mul(left, right)

            DOT, DOLLAR -> {
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
                if (left is Long && right is Long) {
                    val rangeMax = if (expr.operator.type == DOT_DOT) left else left + 1
                    val range = IntRange(left, rangeMax)
                    environment.defineExpr(expr, range)
                    range
                } else if (left is ULong && right is ULong) {
                    val rangeMax = if (expr.operator.type == DOT_DOT) left else left + 1u
                    val range = UIntRange(left, rangeMax)
                    environment.defineExpr(expr, range)
                    range
                } else {
                    throw Unreachable()
                }
            }

            PLUS -> {
                if (left is Double && right is Double) left + right
                else if (left is Long && right is Long) left + right
                else if (left is ULong && right is ULong) left + right
                else if (left is String && right is String) left + right
                else throw Unreachable()
            }

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
        val args = ArrayList<Any>()

        for (argument in expr.arguments) {
            args.add(evaluate(argument))
        }

        if (callee !is Callable) {
            throw RuntimeError(expr.paren, "Can only call functions")
        }

        if (args.size == callee.arity()) {
            return callee.call(this, args)
        } else if (args.size > callee.arity()) {
            throw RuntimeError(expr.paren, "Expected at most ${callee.arity()} arguments but got ${args.size}")
        }

        // Partial application case
        return object : Callable {
            override fun arity(): Int {
                return callee.arity() - args.size
            }

            override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
                val appliedArgs = ArrayList<Any>(args)
                appliedArgs.addAll(arguments)
                return callee.call(interpreter, appliedArgs)
            }
        }

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

        environment.define(expr.name.identifier.lexeme, value)

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
            NOT -> !TypeCheck.checkBooleanOperand(expr.operator, right)
            MINUS -> Math.negate(right)
            PLUS -> TypeCheck.checkNumberOperand(expr.operator, right)
            TILDE -> TypeCheck.checkIntegerOperand(expr.operator, right).inv()
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

    private fun evaluate(expr: Expr): Any {
        return environment.recallExpr(expr) ?: expr.accept(this)
    }

    internal fun evaluateBlock(block: Expr.Block, environment: Environment): Any {
        val previous = environment
        this.environment = environment

        try {
            var ret: Any? = null
            for (expression in block.expressions) {
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
