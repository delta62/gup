enum class TokenType {
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACE, RIGHT_BRACE,
    COMMA,
    DOT,
    MINUS, PLUS,
    SLASH, STAR,
    COLON,
    PERCENT,

    // Bit shifts
    LESS_LESS, GREATER_GREATER,

    // Assignment
    EQ,

    // Comparison
    IS, ISNT, NOT, AND, OR,
    GREATER, GREATER_EQ,
    LESS, LESS_EQ,

    // Bitwise operators
    PIPE, AMPERSAND, TILDE, CARET,

    // Literals
    IDENTIFIER, STRING, NUMBER, TRUE, FALSE,

    ELSE, STRUCT, ENUM, WHERE, OVER, FOR, WHILE, LOOP,
    IF, RETURN, BREAK, CONTINUE, LET, PRINT, WITH, DO, END,
    THEN, IN,

    EOF,
}
