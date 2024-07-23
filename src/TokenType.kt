enum class TokenType {
    LEFT_PAREN, RIGHT_PAREN, // ( )
    LEFT_BRACE, RIGHT_BRACE, // { }
    LEFT_BRACKET, RIGHT_BRACKET, // [ ]
    DOT, DOT_DOT, DOT_DOT_EQ,
    DOLLAR, ARROW, LET, END,
    COMMA, COLON,

    // Operators
    // Mathematical
    MINUS, PLUS,
    SLASH, STAR, PERCENT,
    // Bit shifts
    LESS_LESS, GREATER_GREATER,
    // Assignment
    EQ, PLUS_EQ, MINUS_EQ, STAR_EQ, SLASH_EQ, PERCENT_EQ,
    AMPERSAND_EQ, PIPE_EQ, CARET_EQ, TILDE_EQ, LESS_LESS_EQ,
    GREATER_GREATER_EQ,
    // Comparison
    IS, ISNT, NOT, AND, OR,
    GREATER, GREATER_EQ,
    LESS, LESS_EQ,
    // Bitwise
    PIPE, AMPERSAND, TILDE, CARET,

    // Literals
    IDENTIFIER, TRUE, FALSE,
    DOUBLE, INT, UINT,
    STRING, STRING_HEAD, STRING_MIDDLE, STRING_END,

    // Functions
    FN, RETURN,

    // Control flow
    IF, ELSE, FOR, WHILE, LOOP, BREAK, CONTINUE, IN, THEN,

    // Compound types
    // STRUCT, ENUM, WHERE, OVER, WITH,

    // Control/meta characters
    NEWLINE, EOF,
}
