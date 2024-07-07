package error

import Token

class RuntimeError(val token: Token, message: String) : RuntimeException(message)
