import TokenType.*
import error.Break
import error.Continue
import error.RuntimeError
import error.Unreachable
import generated.Expr
import std.*
import types.GupUnit

class Interpreter : Expr.Visitor<Any> {
    private var env: LexicalScope = LexicalScope.fromEntries(mapOf(
        Args.name() to Args(),
        AssertEqual.name() to AssertEqual(),
        Epoch.name() to Epoch(),
        Iterate.name() to Iterate(),
        Len.name() to Len(),
        Next.name() to Next(),
        PrintLine.name() to PrintLine(),
        RandInt.name() to RandInt(),
        Swap.name() to Swap(),
        TypeOf.name() to TypeOf()
    ))

    private var loopState = LoopState.NoLoop

    fun interpret(statements: List<Expr>) {
        try {
            for (statement in statements) evaluate(statement)
        } catch (error: RuntimeError) {
            Gup.runtimeError(error)
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any {
        val value = evaluate(expr.value)
        env.assign(expr.name, value)
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

                val l = env.get(lName) as Function
                val r = env.get(rName) as Function

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
                when (left) {
                    is Long -> {
                        val r = Math.toLong(right)
                        val rangeMax = if (expr.operator.type == DOT_DOT) r - 1 else r
                        IntRange(left, rangeMax)
                    }

                    is ULong -> {
                        val r = Math.toULong(right)
                        val rangeMax = if (expr.operator.type == DOT_DOT) r - 1u else r
                        UIntRange(left, rangeMax)
                    }

                    else -> {
                        throw Unreachable()
                    }
                }
            }

            PLUS -> {
                if (left is String && right is String) left + right
                else Math.add(left, right)
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
        if (expr.name != null) env = env.define(expr.name.lexeme)
        val function = Function(expr, env)
        if (expr.name != null) env.assign(expr.name, function)
        return function
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any {
        return evaluate(expr.expression)
    }

    override fun visitIfExpr(expr: Expr.If): Any {
        if (evaluate(expr.condition) == true) {
            evaluate(expr.thenBranch)
        } else if (expr.elseBranch != null) {
            evaluate(expr.elseBranch)
        }

        return GupUnit()
    }

    override fun visitLetExpr(expr: Expr.Let): Any {
        val value = if (expr.initializer != null)  evaluate(expr.initializer) else null
        env = env.define(expr.name.identifier.lexeme, value)
        return GupUnit()
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
            NOT -> !(right as Boolean)
            MINUS -> Math.negate(right)
            PLUS -> right
            TILDE -> (right as Long).inv()
            else -> throw Unreachable()
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any {
        return env.get(expr.name) ?: throw RuntimeError(expr.name, "Undefined variable")
    }

    override fun visitLoopExpr(expr: Expr.Loop): Any {
        val lastLoopState = loopState
        loopState = LoopState.InLoop

        try {
            while (evaluate(expr.condition) == true) {
                try {
                    evaluate(expr.body)
                } catch (_: Continue) {
                } catch (_: Break) {
                    break
                }
            }
        } finally {
            loopState = lastLoopState
        }

        return GupUnit()
    }

    private fun evaluate(expr: Expr): Any {
        return expr.accept(this)
    }

    fun evaluateWithEnv(expr: Expr, env: LexicalScope): Any {
        val previous = this.env
        this.env = env

        return try {
            evaluate(expr)
        } finally {
            this.env = previous
        }
    }

    override fun visitBlockExpr(expr: Expr.Block): Any {
        val previous = this.env

        try {
            val init: Any = GupUnit()
            return expr.expressions.fold(init) { _, x -> evaluate(x) }
        } finally {
            this.env = previous
        }
    }
}
