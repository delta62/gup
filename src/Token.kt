import TokenType.*

class Token(val type: TokenType, val lexeme: String, val literal: Any?, val line: Int) {
    override fun toString(): String {
        return when (type) {
            STRING -> "\"$literal\""
            INT, UINT, DOUBLE -> literal as String
            NEWLINE -> "\\n"
            EOF -> "EOF"
            else -> lexeme
        }
    }
}
