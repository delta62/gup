import TokenType.*

class Token(val type: TokenType, val lexeme: String, val literal: Any?, val location: Int) {
    companion object {
        fun isnt(location: Int): Token {
            return Token(ISNT, "isnt", null, location)
        }
    }

    override fun toString(): String {
        return when (type) {
            STRING -> "\"$literal\""
            INT, UINT, DOUBLE -> literal as String
            NEWLINE -> "\\n"
            EOF -> "EOF"
            else -> lexeme
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Token) return false
        return location == other.location && type == other.type
    }
}
