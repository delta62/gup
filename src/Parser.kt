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
        if (match(RETURN)) return returnStatement()
        if (match(WHILE)) return whileStatement()
        if (match(LOOP)) return loopStatement()
        if (match(DO)) return Stmt.Block(block())
        return expressionStatement()
    }

    private fun breakStatement(): Stmt {
        return Stmt.Break(consume(BREAK, "Expected 'break'"))
    }

    private fun continueStatement(): Stmt {
        return Stmt.Continue(consume(CONTINUE, "Expected 'continue'"))
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
        consume(END, "Expected 'end' after if expression")

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        return Stmt.Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = expression()

        return Stmt.Return(keyword, value)
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

        while (match(IS, ISNT)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = bitOr()

        while (match(GREATER, GREATER_EQ, LESS, LESS_EQ)) {
            val operator = previous()
            val right = bitOr()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun bitOr(): Expr {
        var expr = bitXor()

        while (match(PIPE)) {
            val operator = previous()
            val right = bitXor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun bitXor(): Expr {
        var expr = bitAnd()

        while (match(CARET)) {
            val operator = previous()
            val right = bitAnd()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun bitAnd(): Expr {
        var expr = shift()

        while (match(AMPERSAND)) {
            val operator = previous()
            val right = shift()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun shift(): Expr {
        var expr = term()

        while (match(LESS_LESS, GREATER_GREATER)) {
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

        while (match(SLASH, STAR, PERCENT)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(NOT, MINUS, PLUS, TILDE, CARET)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return call()
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr)
            } else {
                break
            }
        }

        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = ArrayList<Expr>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size > 255) {
                    error(peek(), "Can't have more than 255 arguments")
                }
                arguments.add(expression())
            } while (match(COMMA))
        }

        val paren = consume(RIGHT_PAREN, "Expected ')' after arguments")
        return Expr.Call(callee, paren, arguments)
    }

    private fun primary(): Expr {
        if (match(FALSE)) return Expr.Literal(false)
        if (match(TRUE)) return Expr.Literal(true)

        if (match(PIPE)) {
            return functionDefinition()
        }

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

    private fun functionDefinition(): Expr {
        val params = ArrayList<Token>()
        if (!check(PIPE)) {
            do {
                if (params.size > 255)  {
                    error(peek(), "Can't have more than 255 parameters")
                }

                params.add(consume(IDENTIFIER, "Expected parameter name"))
            } while (match(COMMA))
        }

        consume(PIPE, "Expected '|' after parameters")
        consume(DO, "Expected 'do' before function body")
        val body = block()

        return Expr.Function(params, body)
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
        Gup.error(token, message)
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
