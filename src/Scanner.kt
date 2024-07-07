import TokenType.*

class Scanner(private val source: String) {
    private var start = 0
    private var current = 0
    private var line = 1
    private val tokens = ArrayList<Token>()

    companion object {
        val keywords = mapOf(
            "and" to AND,
            "break" to BREAK,
            "continue" to CONTINUE,
            "else" to ELSE,
            "end" to END,
            "enum" to ENUM,
            "false" to FALSE,
            "fn" to FN,
            "for" to FOR,
            "if" to IF,
            "is" to IS,
            "isnt" to ISNT,
            "let" to LET,
            "loop" to LOOP,
            "not" to NOT,
            "or" to OR,
            "over" to OVER,
            "return" to RETURN,
            "struct" to STRUCT,
            "then" to THEN,
            "true" to TRUE,
            "where" to WHERE,
            "while" to WHILE,
            "with" to WITH,
        )
    }

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens.add(Token(EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            ':' -> addToken(COLON)
            '.' -> addToken(DOT)
            '-' -> addToken(scanMinus())
            '~' -> addToken(TILDE)
            '^' -> addToken(CARET)
            '+' -> addToken(PLUS)
            '*' -> addToken(STAR)
            '=' -> addToken(EQ)
            '|' -> addToken(PIPE)
            '&' -> addToken(AMPERSAND)
            '%' -> addToken(PERCENT)
            '<' -> addToken(scanLess())
            '>' -> addToken(scanGreater())
            '/' -> scanSlash()
            ' ', '\r', '\t' -> {}
            '\n' -> line++
            '"' -> string()
            else -> {
                if (c.isDigit()) number()
                else if (c.isLetter()) identifier()
                else Gup.error(line, "Unexpected character")
            }
        }
    }

    private fun scanMinus(): TokenType {
        return if (match('>')) {
            ARROW
        } else {
            MINUS
        }
    }

    private fun scanSlash() {
        if (match('/')) {
            while (peek() != '\n' && !isAtEnd()) advance()
        } else {
            addToken(SLASH)
        }
    }

    private fun scanLess(): TokenType {
        return if (match('=')) {
            LESS_EQ
        } else if (match('<')) {
            LESS_LESS
        } else {
            LESS
        }
    }

    private fun scanGreater(): TokenType {
        return if (match('=')) {
            GREATER_EQ
        } else if (match('>')) {
            GREATER_GREATER
        } else {
            GREATER
        }
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            Gup.error(line, "Unterminated string")
            return
        }

        advance()

        val value = source.substring(start + 1..<current - 1)
        addToken(STRING, value)
    }

    private fun number() {
        while (peek().isDigit()) advance()

        if (peek() == '.' && peekNext().isDigit()) {
            advance()
            while (peek().isDigit()) advance()
        }

        addToken(NUMBER, source.substring(start..<current).toDouble())
    }

    private fun identifier() {
        while (peek().isLetterOrDigit()) advance()

        val text = source.substring(start..<current)
        var type = keywords[text]
        if (type == null) type = IDENTIFIER
        addToken(type)
    }

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }

    private fun advance(): Char {
        return source[current++]
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false

        current++
        return true
    }

    private fun peek(): Char {
        if (isAtEnd()) return '\u0000'
        return source[current]
    }

    private fun peekNext(): Char {
        if (current + 1 >= source.length) return '\u0000'
        return source[current + 1]
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = source.substring(start..<current)
        tokens.add(Token(type, text, literal, line))
    }
}
