import TokenType.*

class Interpreter : Expr.Visitor<Any>, Stmt.Visitor<Unit> {
    internal val globals = Environment()
    private var environment = globals
    private var loopState = LoopState.NoLoop
    private var locals = HashMap<Expr, Int>()
    var assertions = 0

    init {
        globals.define("clock", object : Callable {
            override fun arity(): Int {
                return 0
            }

            override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
                return System.currentTimeMillis().toDouble() / 1000.0
            }
        })

        globals.define("assertEqual", object : Callable {
            override fun arity(): Int {
                return 2
            }

            override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
                val expected = arguments[0]
                val actual = arguments[1]
                assert(expected == actual)
                assertions += 1
                return SamUnit()
            }
        })
    }

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                execute(statement)
            }
        } catch (error: RuntimeError) {
            Gup.runtimeError(error)
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any {
        val value = evaluate(expr.value)

        val distance = locals.get(expr)
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
                checkNumberOperands(expr.operator, left, right)
                left as Double / right as Double
            }

            STAR -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double * right as Double
            }

            PERCENT -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double % right as Double
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

            ISNT -> !isEqual(left, right)
            IS -> isEqual(left, right)
            else -> throw Unreachable()
        }
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
        return Function(expr)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any {
        return expr.value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any {
        val left = evaluate(expr.left)

        if (expr.operator.type == OR) {
            if (left == true) return left
        } else {
            if (left == true) return left
        }

        return evaluate(expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            NOT -> !(right as Boolean)
            MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }

            else -> throw Unreachable()
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any {
        return lookUpVariable(expr.name, expr)
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

    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    internal fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment

            for (statement in statements) {
                if (statement is Stmt.Break) break
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
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

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        if (loopState != LoopState.InLoop) throw RuntimeError(stmt.token, "Cannot break outside of loops")
        loopState = LoopState.BrokenLoop
    }

    override fun visitContinueStmt(stmt: Stmt.Continue) {
        if (loopState != LoopState.InLoop) throw RuntimeError(stmt.token, "Cannot continue outside of loops")
        loopState = LoopState.ContinuedLoop
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitForLoopStmt(stmt: Stmt.ForLoop) {
        TODO("Not yet implemented")
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (evaluate(stmt.condition) == true) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        var value: Any? = null
        if (stmt.value != null) value = evaluate(stmt.value)

        throw Return(value)
    }

    override fun visitLetStmt(stmt: Stmt.Let) {
        var value: Any? = null

        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer)
        }

        environment.define(stmt.name.lexeme, value)
    }

    override fun visitLoopStmt(stmt: Stmt.Loop) {
        val lastLoopState = loopState
        loopState = LoopState.InLoop
        while (true) {
            execute(stmt.body)

            if (loopState == LoopState.BrokenLoop) {
                break
            }

            if (loopState == LoopState.ContinuedLoop) {
                continue
            }
        }
        loopState = lastLoopState
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        val lastLoopState = loopState
        loopState = LoopState.InLoop
        while (evaluate(stmt.condition) == true) {
            execute(stmt.body)

            if (loopState == LoopState.BrokenLoop) {
                break
            }

            if (loopState == LoopState.ContinuedLoop) {
                continue
            }
        }
        loopState = lastLoopState
    }
}
