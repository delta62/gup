enum class TokenType {
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACE, RIGHT_BRACE,
    DOT, DOT_DOT, DOT_DOT_EQ,
    COMMA, COLON, ARROW, LET, END,

    // Operators
    // Mathematical
    MINUS, PLUS,
    SLASH, STAR, PERCENT,
    // Bit shifts
    LESS_LESS, GREATER_GREATER,
    // Assignment
    EQ,
    // Comparison
    IS, ISNT, NOT, AND, OR,
    GREATER, GREATER_EQ,
    LESS, LESS_EQ,
    // Bitwise
    PIPE, AMPERSAND, TILDE, CARET,

    // Literals
    IDENTIFIER, STRING, NUMBER, TRUE, FALSE,

    // Functions
    FN, RETURN,

    // Control flow
    IF, ELSE, FOR, WHILE, LOOP, BREAK, CONTINUE, IN, THEN,

    // Compound types
    // STRUCT, ENUM, WHERE, OVER, WITH,

    // Control/meta characters
    NEWLINE, EOF,
}
