import java.util.*
import kotlin.collections.HashMap

class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit> {
    private var currentFunction = FunctionType.None
    private val scopes = Stack<HashMap<String, Boolean>>()

    enum class FunctionType {
        None,
        Function,
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitBreakExpr(expr: Expr.Break) { }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)
        for (argument in expr.arguments) resolve(argument)
    }

    override fun visitContinueExpr(expr: Expr.Continue) { }

    override fun visitFunctionExpr(expr: Expr.Function) {
        if (expr.name != null) {
            declare(expr.name)
            define(expr.name)
        }

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

    override fun visitIfExpr(expr: Expr.If) {
        resolve(expr.condition)
        resolve(expr.thenBranch)
        if (expr.elseBranch != null) resolve(expr.elseBranch)
    }

    override fun visitLetExpr(expr: Expr.Let) {
        declare(expr.name)
        if (expr.initializer != null) {
            resolve(expr.initializer)
        }

        define(expr.name)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {}

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitLoopExpr(expr: Expr.Loop) {
        resolve(expr.condition)
        resolve(expr.body)
    }

    override fun visitReturnExpr(expr: Expr.Return) {
        if (currentFunction != FunctionType.Function) {
            Gup.error(expr.keyword, "Can't return from top-level code")
        }

        resolve(expr.value)
    }

    override fun visitTemplateExpr(expr: Expr.Template) { }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.isEmpty() && scopes.peek()[expr.name.lexeme] == false) {
            Gup.error(expr.name, "Can't read local variable in its own initializer")
        }

        resolveLocal(expr, expr.name)
    }

    override fun visitBlockExpr(expr: Expr.Block) {
        beginScope()
        resolve(expr.expressions)
        endScope()
    }

    internal fun resolve(expressions: List<Expr>) {
        for (expression in expressions) resolve(expression)
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
