import TokenType.*
import error.ParseError
import generated.Expr
import std.Iterate
import std.Next
import types.GupUnit
import types.GupList

class Parser(private val tokens: List<Token>) {
    private var current: Int = 0

    fun parse(): List<Expr> {
        val declarations = ArrayList<Expr>()
        while (!isAtEnd()) {
            val expr = declaration()
            if (expr != null) declarations.add(expr)
            while (peek().type == NEWLINE) advance()
        }

        return declarations
    }

    private fun declaration(): Expr? {
        try {
            return expression()
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun expression(): Expr {
        if (match(BREAK)) return breakExpression()
        if (match(CONTINUE)) return continueExpression()
        if (match(FOR)) return forExpression()
        if (match(IF)) return ifExpression()
        if (match(LET)) return letExpression()
        if (match(LOOP)) return loopExpression()
        if (match(NEWLINE)) return expression()
        if (match(RETURN)) return returnExpression()
        if (match(WHILE)) return whileExpression()
        return assignment()
    }

    private fun breakExpression(): Expr {
        return Expr.Break(previous())
    }

    private fun continueExpression(): Expr {
        return Expr.Continue(previous())
    }

    /* For loops are desugared into while loops. Given:
     *
     * `for x in [0..3]` -> body()
     *
     * We'll transform it into:
     *
     * do
     *      let iterator = iterate([0..3])
     *      let x = next(iterator)
     *      while x isnt unit
     *          do
     *              body()
     *          end
     *          x = next(iterator)
     *      end
     * end
     */
    private fun forExpression(): Expr {
        // First, parse the code as written
        val name = consume(IDENTIFIER, "Expected local binding in for expression")
        val localBinding = TypedIdentifier(name, null)
        val inToken = consume(IN, "Expected 'in' in for expression")
        val iterable = expression()
        skipWhitespace()
        val body = if (match(ARROW)) blockOf(expression()) else block(END)

        // Next, build up a parse tree de-sugared down to a while loop
        val iteratorName = Token(IDENTIFIER, "iterator", null, inToken.location)
        val iteratorInit = TypedIdentifier(iteratorName, null)
        val iterableInitializer = Expr.Let(iteratorInit, inlineCall(Iterate.name(), inToken, iterable))
        val localInitializer = Expr.Let(localBinding, inlineCall(Next.name(), name, Expr.Variable(iteratorName)))
        val increment = Expr.Assign(name, inlineCall(Next.name(), name, Expr.Variable(iteratorName)))
        val loopBody = Expr.Block(listOf(body, increment))
        val condition = Expr.Binary(Expr.Variable(name), Token.isnt(name.location), Expr.Literal(GupUnit()))
        val loop = Expr.Loop(condition, loopBody)

        return Expr.Block(listOf(iterableInitializer, localInitializer, loop))
    }

    private fun inlineCall(name: String, token: Token, vararg args: Expr): Expr {
        val tok = Token(IDENTIFIER, name, null, token.location)
        val variable = Expr.Variable(tok)
        return Expr.Call(variable, token, args.toList())
    }

    private fun ifExpression(): Expr {
        val condition = expression()
        consume(THEN, "expected 'then' after if expression")

        val thenBranch = block(ELSE, END)
        val elseBranch = if (previous().type == ELSE) block(ELSE, END) else null

        return Expr.If(condition, thenBranch, elseBranch)
    }

    private fun returnExpression(): Expr {
        val keyword = previous()
        val value = if (match(NEWLINE)) Expr.Literal(GupUnit()) else expression()

        return Expr.Return(keyword, value)
    }

    private fun whileExpression(): Expr {
        val condition = expression()
        val body = if (match(ARROW)) {
            Expr.Block(listOf(expression()))
        } else {
            block(END)
        }

        return Expr.Loop(condition, body)
    }

    private fun loopExpression(): Expr {
        val body = if (match(ARROW)) {
            Expr.Block(listOf(expression()))
        } else {
            block(END)
        }

        return Expr.Loop(Expr.Literal(true), body)
    }

    private fun letExpression(): Expr {
        val identifier = consume(IDENTIFIER, "Expected a variable name")
        val type = if (match(COLON)) consume(IDENTIFIER, "Expected a type") else null
        val name = TypedIdentifier(identifier, type)
        val initializer = if (match(EQ)) expression() else null

        if (initializer is Expr.Function && initializer.name != null) {
            throw error(initializer.name, "Assigned functions cannot have names")
        }

        return Expr.Let(name, initializer)
    }

    private fun block(vararg terminals: TokenType): Expr.Block {
        val expressions = ArrayList<Expr>()
        while (!check(*terminals) && !isAtEnd()) {
            expressions.add(expression())
            skipWhitespace()
        }

        consume("Unterminated block", *terminals)
        return Expr.Block(expressions)
    }

    private fun blockOf(expr: Expr): Expr.Block {
        return Expr.Block(listOf(expr))
    }

    private fun assignment(): Expr {
        val expr = composeLow()

        if (match(EQ)) {
            val equals = previous()
            val value = composeLow()

            if (expr is Expr.Variable) {
                if (value is Expr.Function && value.name != null) {
                    throw error(value.name, "Assigned functions cannot have names")
                }

                return Expr.Assign(expr.name, value)
            }

            error(equals, "Invalid assignment target")
        } else if (match(PLUS_EQ)) {
            val assignment = compoundAssignment(expr, PLUS, "+")
            if (assignment != null) return assignment
        } else if (match(MINUS_EQ)) {
            val assignment = compoundAssignment(expr, MINUS, "+")
            if (assignment != null) return assignment
        } else if (match(STAR_EQ)) {
            val assignment = compoundAssignment(expr, STAR, "*")
            if (assignment != null) return assignment
        } else if (match(SLASH_EQ)) {
            val assignment = compoundAssignment(expr, SLASH, "/")
            if (assignment != null) return assignment
        } else if (match(PERCENT_EQ)) {
            val assignment = compoundAssignment(expr, PERCENT, "%")
            if (assignment != null) return assignment
        } else if (match(AMPERSAND_EQ)) {
            val assignment = compoundAssignment(expr, AMPERSAND, "&")
            if (assignment != null) return assignment
        } else if (match(PIPE_EQ)) {
            val assignment = compoundAssignment(expr, PIPE, "|")
            if (assignment != null) return assignment
        } else if (match(CARET_EQ)) {
            val assignment = compoundAssignment(expr, CARET, "^")
            if (assignment != null) return assignment
        } else if (match(TILDE_EQ)) {
            val assignment = compoundAssignment(expr, TILDE, "~")
            if (assignment != null) return assignment
        } else if (match(LESS_LESS_EQ)) {
            val assignment = compoundAssignment(expr, LESS_LESS, "<<")
            if (assignment != null) return assignment
        } else if (match(GREATER_GREATER_EQ)) {
            val assignment = compoundAssignment(expr, GREATER_GREATER, ">>")
            if (assignment != null) return assignment
        }

        return expr
    }

    private fun compoundAssignment(expr: Expr, desugaredType: TokenType, lexeme: String): Expr.Assign? {
        val op = previous()
        val value = composeLow()

        if (expr is Expr.Variable) {
            val token = Token(desugaredType, lexeme, null, op.location)
            val right = Expr.Binary(expr, token, value)
            return Expr.Assign(expr.name, right)
        }

        error(op, "Invalid assignment target")
        return null
    }

    private fun composeLow(): Expr {
        var expr = range()

        while (match(DOLLAR)) {
            val operator = previous()
            val right = range()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun range(): Expr {
        var expr = or()

        if (match(DOT_DOT, DOT_DOT_EQ)) {
            val operator = previous()
            val right = or()
            expr = Expr.Binary(expr, operator, right)
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
        var expr = compose()

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr)
            } else {
                break
            }
        }

        return expr
    }

    private fun compose(): Expr {
        var expr = primary()

        while (match(DOT)) {
            val operator = previous()
            val right = primary()
            expr = Expr.Binary(expr, operator, right)
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
        if (match(LEFT_BRACKET)) return listLiteral()
        if (match(FN)) return functionDefinition()
        if (match(INT, UINT, DOUBLE, STRING)) return Expr.Literal(previous().literal!!)
        if (match(IDENTIFIER)) return Expr.Variable(previous())

        if (match(STRING_HEAD)) {
            val parts = ArrayList<TemplateString>()
            parts.add(TemplateString.Text(previous().literal as String))

            while (true) {
                parts.add(TemplateString.Expression(expression()))

                if (match(STRING_END)) {
                    parts.add(TemplateString.Text(previous().literal as String))
                    break
                } else if (match(STRING_MIDDLE)) {
                    parts.add(TemplateString.Text(previous().literal as String))
                } else {
                    throw error(previous(), "Expected '}'")
                }
            }

            return Expr.Template(parts)
        }

        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expected ')' after expression")
            return Expr.Grouping(expr)
        }

        throw error(peek(), "Expected an expression")
    }

    private fun listLiteral(): Expr {
        val list = GupList()

        while (!match(RIGHT_BRACKET)) {
            if (isAtEnd()) error("Expected ']' following list declaration")
            list.add(expression())
            if (!match(COMMA)) break
        }

        consume(RIGHT_BRACKET, "Expected ']' following list declaration")

        return Expr.Literal(list)
    }

    private fun functionDefinition(): Expr {
        val params = ArrayList<TypedIdentifier>()
        val functionName = if (check(IDENTIFIER)) advance() else null

        consume(LEFT_PAREN, "Expected '(' after function name")
        if (!check(RIGHT_PAREN)) {
            do {
                if (params.size > 255)  {
                    error(peek(), "Can't have more than 255 parameters")
                }

                val name = consume(IDENTIFIER, "Expected parameter name")
                consume(COLON, "Expected ':' after parameter name")
                val type = consume(IDENTIFIER, "Expected type name after ':'")

                params.add(TypedIdentifier(name, type))
            } while (match(COMMA))
        }

        consume(RIGHT_PAREN, "Expected '|' after parameters")

        consume(COLON, "Expected ':' after function parameters")
        val returnType = consume(IDENTIFIER, "Expected return type after ':'")

        val body = if (match(ARROW)) {
            val expr = expression()
            skipWhitespace()
            listOf(expr)
        } else {
            val expressions = ArrayList<Expr>()
            while (!check(END) && !isAtEnd()) {
                expressions.add(expression())
                skipWhitespace()
            }
            consume("Unterminated block", END)
            expressions
        }

        return Expr.Function(functionName, params, body, returnType)
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
        return consume(message, type)
    }

    private fun consume(message: String, vararg types: TokenType): Token {
        if (check(*types)) {
            val ret = advance()
            skipWhitespace()
            return ret
        }
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
                LET, FOR, IF, WHILE, LOOP, RETURN, FN -> return
                else -> advance()
            }
        }
    }

    private fun check(vararg types: TokenType): Boolean {
        if (isAtEnd()) return false
        return types.contains(peek().type)
    }

    private fun skipWhitespace() {
        while (match(NEWLINE)) {
            // Skip newlines
        }
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
        var location = current - 1
        while (location >= 0) {
            val tok = tokens[location]
            if (tok.type != NEWLINE) return tok
            location -= 1
        }

        throw RuntimeException("Reached start of input while attempting to find previous token")
    }
}
