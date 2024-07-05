import TokenType.*

class Interpreter : Expr.Visitor<Any>, Stmt.Visitor<Unit> {
    private var environment = Environment()
    private var inLoop = false
    private var breakingLoop = false
    private var continuingLoop = false

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                execute(statement)
            }
        } catch (error: RuntimeError) {
            SamLang.runtimeError(error)
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any {
        val value = evaluate(expr.value)
        environment.assign(expr.name, value)
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
                    TODO("unreachable")
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

    override fun visitLogicalExpr(expr: Expr.Logical): Any {
        val left = evaluate(expr.left)

        if (expr.operator.type == TokenType.OR) {
            if (left == true) return left
        } else {
            if (left == true) return left
        }

        return evaluate(expr.right)
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

    override fun visitVariableExpr(expr: Expr.Variable): Any {
        val value = environment.get(expr.name) ?: throw RuntimeError(expr.name, "Undefined variable '${expr.name.lexeme}'")
        return value
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

    private fun executeBlock(statements: List<Stmt>, environment: Environment) {
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
        if (!inLoop) throw RuntimeError(stmt.token, "Cannot break outside of loops")
        breakingLoop = true
    }

    override fun visitContinueStmt(stmt: Stmt.Continue) {
        if (!inLoop) throw RuntimeError(stmt.token, "Cannot continue outside of loops")
        continuingLoop = true
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

    override fun visitLetStmt(stmt: Stmt.Let) {
        var value: Any? = null

        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer)
        }

        environment.define(stmt.name.lexeme, value)
    }

    override fun visitLoopStmt(stmt: Stmt.Loop) {
        inLoop = true
        while (true) {
            execute(stmt.body)

            if (breakingLoop) {
                breakingLoop = false
                break
            }

            if (continuingLoop) {
                continuingLoop = false
                continue
            }
        }
        inLoop = false
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        inLoop = true
        while (evaluate(stmt.condition) == true) {
            execute(stmt.body)

            if (breakingLoop) {
                breakingLoop = false
                break
            }

            if (continuingLoop) {
                continuingLoop = false
                continue
            }
        }
        inLoop = false
    }
}
