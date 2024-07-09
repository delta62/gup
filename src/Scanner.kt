import TokenType.*
import error.ScanError
import kotlin.math.pow

class Scanner(private val source: String) {
    private var start = 0
    private var current = 0
    private var line = 1
    private val tokens = ArrayList<Token>()

    companion object {
        private val keywords = mapOf(
            "and" to AND,
            "break" to BREAK,
            "continue" to CONTINUE,
            "else" to ELSE,
            "end" to END,
            "false" to FALSE,
            "fn" to FN,
            "for" to FOR,
            "if" to IF,
            "in" to IN,
            "is" to IS,
            "isnt" to ISNT,
            "let" to LET,
            "loop" to LOOP,
            "not" to NOT,
            "or" to OR,
            "return" to RETURN,
            "then" to THEN,
            "true" to TRUE,
            "while" to WHILE,
        )
    }

    fun scan(): List<Token> {
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
            '~' -> addToken(TILDE)
            '^' -> addToken(CARET)
            '+' -> addToken(PLUS)
            '*' -> addToken(STAR)
            '=' -> addToken(EQ)
            '|' -> addToken(PIPE)
            '&' -> addToken(AMPERSAND)
            '%' -> addToken(PERCENT)
            '.' -> scanDot()
            '-' -> scanMinus()
            '<' -> scanLess()
            '>' -> scanGreater()
            '/' -> scanSlash()
            ' ', '\r', '\t' -> {}
            '\n' -> scanNewline()
            '"' -> string()
            else -> {
                if (c.isDigit()) number()
                else if (c.isLetter()) identifier()
                else Gup.error(line, "Unexpected character")
            }
        }
    }

    private fun scanNewline() {
        line += 1
        addToken(NEWLINE)
    }

    private fun scanDot() {
        val tokenType = if (match('.')) {
            if (match('=')) {
                DOT_DOT_EQ
            } else {
                DOT_DOT
            }
        } else {
            DOT
        }

        addToken(tokenType)
    }

    private fun scanMinus() {
        val tokenType = if (match('>')) {
            ARROW
        } else {
            MINUS
        }

        addToken(tokenType)
    }

    private fun scanSlash() {
        if (match('/')) {
            while (peek() != '\n' && !isAtEnd()) advance()
        } else {
            addToken(SLASH)
        }
    }

    private fun scanLess() {
        val tokenType = if (match('=')) {
            LESS_EQ
        } else if (match('<')) {
            LESS_LESS
        } else {
            LESS
        }

        addToken(tokenType)
    }

    private fun scanGreater() {
        val tokenType = if (match('=')) {
            GREATER_EQ
        } else if (match('>')) {
            GREATER_GREATER
        } else {
            GREATER
        }

        addToken(tokenType)
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
        val radix = if (previous() == '0') {
            when (val c = peek()) {
                'x' -> 16
                'o' -> 8
                'b' -> 2
                else -> {
                    if (c.isDigit()) {
                        Gup.error(line, "Numbers starting with '0' must be followed with 'x', 'o', or 'b'")
                        throw ScanError()
                    } else {
                        10
                    }
                }
            }
        } else {
            10
        }

        if (radix != 10) {
            advance()
            start += 2
        }

        while (peek().isDigit() || peek() == '_') advance()


        val num = if (radix != 10) {
            source
                .substring(start..<current)
                .replace("_", "")
                .toLong(radix).toDouble()
        } else {
            // Base 10 can have decimal points
            if (peek() == '.' && peekNext().isDigit()) {
                advance()
                while (peek().isDigit() || peek() == '_') advance()
            }

            var x = source
                .substring(start..<current)
                .replace("_", "")
                .toDouble()

            // 1.23e-4
            if (peek() == 'e' && (peekNext() == '-' || peekNext().isDigit())) {
                advance()
                val multiplier = if (peek() == '-') {
                    advance()
                    -1
                } else 1

                start = current
                while (peek().isDigit() || peek() == '_') advance()
                val exponent = source
                    .substring(start..<current)
                    .replace("_", "")
                    .toDouble()

                x *= 10.0.pow(exponent * multiplier)
            }

            x
        }

        addToken(NUMBER, num)
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

    private fun previous(): Char {
        return source[current - 1]
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
