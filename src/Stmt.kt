sealed class Stmt {
    interface Visitor<R> {
        fun visitBlockStmt(stmt: Block): R
        fun visitBreakStmt(stmt: Break): R
        fun visitContinueStmt(stmt: Continue): R
        fun visitExpressionStmt(stmt: Expression): R
        fun visitForLoopStmt(stmt: ForLoop): R
        fun visitIfStmt(stmt: If): R
        fun visitReturnStmt(stmt: Return): R
        fun visitLetStmt(stmt: Let): R
        fun visitLoopStmt(stmt: Loop): R
        fun visitWhileStmt(stmt: While): R
    }

    data class Block(val statements: List<Stmt>) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitBlockStmt(this)
        }
    }
    data class Break(val token: Token) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitBreakStmt(this)
        }
    }
    data class Continue(val token: Token) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitContinueStmt(this)
        }
    }
    data class Expression(val expression: Expr) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitExpressionStmt(this)
        }
    }
    data class ForLoop(val name: Token, val iterator: Token, val body: Stmt) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitForLoopStmt(this)
        }
    }
    data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitIfStmt(this)
        }
    }
    data class Return(val keyword: Token, val value: Expr?) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitReturnStmt(this)
        }
    }
    data class Let(val name: Token, val initializer: Expr?) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitLetStmt(this)
        }
    }
    data class Loop(val body: Stmt) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitLoopStmt(this)
        }
    }
    data class While(val condition: Expr, val body: Stmt) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitWhileStmt(this)
        }
    }

    abstract fun <R> accept(visitor: Visitor<R>): R
}
