import java.util.*
import kotlin.collections.HashMap

class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private var currentFunction = FunctionType.None

    enum class FunctionType {
        None,
        Function,
    }

    private val scopes = Stack<HashMap<String, Boolean>>()
    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)
        for (argument in expr.arguments) resolve(argument)
    }

    override fun visitFunctionExpr(expr: Expr.Function) {
        val enclosingFunction = currentFunction
        currentFunction = FunctionType.Function

        beginScope()
        for (param in expr.params) {
            declare(param)
            define(param)
        }
        resolve(expr.body)
        endScope()

        currentFunction = enclosingFunction
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {}

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.isEmpty() && scopes.peek()[expr.name.lexeme] == false) {
            Gup.error(expr.name, "Can't read local variable in its own initializer")
        }

        resolveLocal(expr, expr.name)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {}

    override fun visitContinueStmt(stmt: Stmt.Continue) {}

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visitForLoopStmt(stmt: Stmt.ForLoop) {
        beginScope()
        declare(stmt.name)
        resolve(stmt.body)
        endScope()
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null) resolve(stmt.elseBranch)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction != FunctionType.Function) {
            Gup.error(stmt.keyword, "Can't return from top-level code")
        }

        if (stmt.value != null) resolve(stmt.value)
    }

    override fun visitLetStmt(stmt: Stmt.Let) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }

        define(stmt.name)
    }

    override fun visitLoopStmt(stmt: Stmt.Loop) {
        resolve(stmt.body)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    internal fun resolve(statements: List<Stmt>) {
        for (statement in statements) resolve(statement)
    }

    private fun resolve(statement: Stmt) {
        statement.accept(this)
    }

    private fun resolve(expression: Expr) {
        expression.accept(this)
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.size - 1 downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun beginScope() {
        scopes.push(HashMap())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return
        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)) {
            Gup.error(name, "Already a variable with this name in this scope")
        }

        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.peek()[name.lexeme] = true
    }
}
