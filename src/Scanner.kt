import TokenType.*
import error.ScanError
import kotlin.math.pow

class Scanner(private val source: String) {
    private var start = 0
    private var current = 0
    private var line = 1
    private var inInterpolation = false
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
            ',' -> addToken(COMMA)
            ':' -> addToken(COLON)
            '=' -> addToken(EQ)
            '$' -> addToken(DOLLAR)
            '~' -> scanOptionalAssignment(TILDE, TILDE_EQ)
            '^' -> scanOptionalAssignment(CARET, CARET_EQ)
            '+' -> scanOptionalAssignment(PLUS, PLUS_EQ)
            '*' -> scanOptionalAssignment(STAR, STAR_EQ)
            '|' -> scanOptionalAssignment(PIPE, PIPE_EQ)
            '&' -> scanOptionalAssignment(AMPERSAND, AMPERSAND_EQ)
            '%' -> scanOptionalAssignment(PERCENT, PERCENT_EQ)
            '-' -> scanMinus()
            '{' -> addToken(LEFT_BRACE)
            '}' -> scanRightBrace()
            '.' -> scanDot()
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

    private fun scanOptionalAssignment(term: TokenType, assignment: TokenType) {
        if (match('=')) addToken(assignment)
        else addToken(term)
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
        } else if (match('=')) {
            MINUS_EQ
        } else {
            MINUS
        }

        addToken(tokenType)
    }

    private fun scanSlash() {
        if (match('/')) {
            while (peek() != '\n' && !isAtEnd()) advance()
        } else if (match('=')) {
            addToken(SLASH_EQ)
        } else {
            addToken(SLASH)
        }
    }

    private fun scanLess() {
        val tokenType = if (match('=')) {
            LESS_EQ
        } else if (match('<')) {
            if (match('=')) LESS_LESS_EQ
            else LESS_LESS
        } else {
            LESS
        }

        addToken(tokenType)
    }

    private fun scanGreater() {
        val tokenType = if (match('=')) {
            GREATER_EQ
        } else if (match('>')) {
            if (match('=')) GREATER_GREATER_EQ
            else GREATER_GREATER
        } else {
            GREATER
        }

        addToken(tokenType)
    }

    private fun string() {
        if (inInterpolation && previous() != '}') {
            Gup.error(line, "Cannot embed string literals inside of string interpolation")
            return
        }

        val builder = StringBuilder()
        val wasInInterpolation = inInterpolation
        inInterpolation = false

        while (true) {
            if (isAtEnd()) {
                return Gup.error(line, "Unterminated string")
            }

            if (peek() == '"') {
                advance()
                break
            }

            if (match('\\')) {
                if (match('n')) builder.append('\n')
                else if (match('r')) builder.append('\r')
                else if (match('t')) builder.append('\t')
                else if (match('\\')) builder.append('\\')
                else if (match('"')) builder.append('"')
                else if (match('u')) {
                    val escapeBuffer = StringBuilder(6)
                    consume('{')
                    for (i in 0..<6) {
                        if (peek() == '}') break
                        escapeBuffer.append(advance())
                    }
                    val codePoint = escapeBuffer.toString().toInt(16)
                    builder.appendCodePoint(codePoint)
                    consume('}')
                }
                else return Gup.error(line, "Unknown escape sequence")
                continue
            }

            if (peek() == '#' && peekNext() == '{') {
                inInterpolation = true
                advance()
                advance()

                val tokenType = if (wasInInterpolation) STRING_MIDDLE else STRING_HEAD
                return addToken(tokenType, builder.toString())
            }

            if (peek() == '\n') line++
            builder.append(advance())
        }

        val tokenType = if (wasInInterpolation) STRING_END else STRING
        addToken(tokenType, builder.toString())
    }

    private fun scanRightBrace() {
        if (inInterpolation) return string()
        return addToken(RIGHT_BRACE)

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

        if (radix != 10) {
            val num = source
                .substring(start..<current)
                .replace("_", "")
                .toLong(radix).toDouble()
            return addToken(INT, num)
        }

        var isDecimal = false

        // Base 10 can have decimal points
        if (peek() == '.' && peekNext().isDigit()) {
            isDecimal = true
            advance()
            while (peek().isDigit() || peek() == '_') advance()
        }

        var coefficient = source
            .substring(start..<current)
            .replace("_", "")
            .toDouble()

        // 1.23e-4
        if (peek() == 'e' && (peekNext() == '-' || peekNext().isDigit())) {
            advance()
            val multiplier = if (match('-')) -1 else 1
            start = current

            while (peek().isDigit() || peek() == '_') advance()
            val exponent = source
                .substring(start..<current)
                .replace("_", "")
                .toDouble()

            coefficient *= 10.0.pow(exponent * multiplier)
        }

        if (isDecimal) {
            addToken(DOUBLE, coefficient)
        } else {
            addToken(INT, coefficient.toLong())
        }
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

    private fun consume(expected: Char) {
        if (match(expected)) return
        throw ScanError()
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
