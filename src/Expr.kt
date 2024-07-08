sealed class Expr {
    interface Visitor<R> {
        fun visitAssignExpr(expr: Assign): R
        fun visitBinaryExpr(expr: Binary): R
        fun visitBlockExpr(expr: Block): R
        fun visitBreakExpr(expr: Break): R
        fun visitCallExpr(expr: Call): R
        fun visitContinueExpr(expr: Continue): R
        fun visitForLoopExpr(expr: ForLoop): R
        fun visitFunctionExpr(expr: Function): R
        fun visitGroupingExpr(expr: Grouping): R
        fun visitIfExpr(expr: If): R
        fun visitLetExpr(expr: Let): R
        fun visitLiteralExpr(expr: Literal): R
        fun visitLogicalExpr(expr: Logical): R
        fun visitLoopExpr(expr: Loop): R
        fun visitReturnExpr(expr: Return): R
        fun visitUnaryExpr(expr: Unary): R
        fun visitVariableExpr(expr: Variable): R
        fun visitWhileExpr(expr: While): R
    }

    data class Assign(val name: Token, val value: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitAssignExpr(this)
        }
    }

    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitBinaryExpr(this)
        }
    }

    data class Block(val expressions: List<Expr>) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitBlockExpr(this)
        }
    }

    data class Break(val token: Token) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitBreakExpr(this)
        }
    }

    data class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitCallExpr(this)
        }
    }

    data class Continue(val token: Token) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitContinueExpr(this)
        }
    }

    data class ForLoop(val name: Token, val iterator: Token, val body: List<Expr>) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitForLoopExpr(this)
        }
    }

    data class Function(val name: Token, val params: List<Token>, val body: List<Expr>) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitFunctionExpr(this)
        }
    }

    data class Grouping(val expression: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitGroupingExpr(this)
        }
    }

    data class If(val condition: Expr, val thenBranch: List<Expr>, val elseBranch: List<Expr>?) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitIfExpr(this)
        }
    }

    data class Let(val name: Token, val initializer: Expr?) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitLetExpr(this)
        }
    }

    data class Literal(val value: Any) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitLiteralExpr(this)
        }
    }

    data class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitLogicalExpr(this)
        }
    }

    data class Loop(val body: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitLoopExpr(this)
        }
    }

    data class Return(val keyword: Token, val value: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitReturnExpr(this)
        }
    }

    data class Unary(val operator: Token, val right: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitUnaryExpr(this)
        }
    }

    data class Variable(val name: Token) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitVariableExpr(this)
        }
    }

    data class While(val condition: Expr, val body: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitWhileExpr(this)
        }
    }

    abstract fun <R> accept(visitor: Visitor<R>): R
}
