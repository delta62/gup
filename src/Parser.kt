import TokenType.*

class Parser(private val tokens: List<Token>) {
    private class ParseError : RuntimeException()
    private var current: Int = 0

    fun parse(): List<Stmt> {
        val statements = ArrayList<Stmt>()
        while (!isAtEnd()) {
            val declaration = declaration()
            if (declaration != null) statements.add(declaration)
        }

        return statements
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun declaration(): Stmt? {
        try {
            if (match(LET)) return letDeclaration()
            return statement()
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun statement(): Stmt {
        if (check(BREAK)) return breakStatement()
        if (match(CONTINUE)) return continueStatement()
        if (match(FOR)) return forStatement()
        if (match(IF)) return ifStatement()
        if (match(PRINT)) return printStatement()
        if (match(WHILE)) return whileStatement()
        if (match(LOOP)) return loopStatement()
        if (match(DO)) return Stmt.Block(block())
        return expressionStatement()
    }

    private fun breakStatement(): Stmt {
        return Stmt.Break(consume(BREAK, "Expected 'break'"))
    }

    private fun continueStatement(): Stmt {
//        return Stmt.Continue()
        TODO()
    }

    private fun forStatement(): Stmt {
        val name = consume(IDENTIFIER, "Expected local binding in for statement")
        consume(IN, "Expected 'in' in for statement")
        val iterable = consume(IDENTIFIER, "Expected iterable in for statement")
        val body = statement()

        return Stmt.ForLoop(name, iterable, body)
    }

    private fun ifStatement(): Stmt {
        val condition = expression()
        consume(THEN, "Expected 'then' after if expression")

        val thenBranch = statement()
        val elseBranch = if (match(ELSE)) statement() else null

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        return Stmt.Print(value)
    }

    private fun whileStatement(): Stmt {
        val condition = expression()
        val body = statement()

        return Stmt.While(condition, body)
    }

    private fun loopStatement(): Stmt {
        val body = statement()
        return Stmt.Loop(body)
    }

    private fun letDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expected a variable name")
        var initializer: Expr? = null
        if (match(EQ)) initializer = expression()

        return Stmt.Let(name, initializer)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        return Stmt.Expression(expr)
    }

    private fun block(): List<Stmt> {
        val statements = ArrayList<Stmt>()
        while (!check(END) && !isAtEnd()) {
            val declaration = declaration()
            if (declaration != null) statements.add(declaration)
        }

        consume(END, "Expected 'end' after block")
        return statements
    }

    private fun assignment(): Expr {
        val expr = or()

        if (match(EQ)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }

            error(equals, "Invalid assignment target")
        }

        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (match(BANG_EQ, EQ_EQ)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(GREATER, GREATER_EQ, LESS, LESS_EQ)) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(MINUS, PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(SLASH, STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return primary()
    }

    private fun primary(): Expr {
        if (match(FALSE)) return Expr.Literal(false)
        if (match(TRUE)) return Expr.Literal(true)

        if (match(NUMBER, STRING)) {
            return Expr.Literal(previous().literal!!)
        }

        if (match(IDENTIFIER)) {
            return Expr.Variable(previous())
        }

        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expected ')' after expression")
            return Expr.Grouping(expr)
        }

        throw error(peek(), "Expected an expression")
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }

        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        SamLang.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            when (peek().type) {
                LET, FOR, IF, WHILE, PRINT, RETURN -> return
                else -> advance()
            }
        }
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean {
        return peek().type == EOF
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }
}
