enum class TokenType {
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACE, RIGHT_BRACE,
    DOT,
    MINUS, PLUS,
    SLASH, STAR, PERCENT,
    COMMA, COLON, ARROW,

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

    // Functions
    FN, RETURN,

    ELSE, STRUCT, ENUM, WHERE, OVER, FOR, WHILE, LOOP,
    IF, BREAK, CONTINUE, LET, WITH, DO, END,
    THEN, IN,

    EOF,
}
