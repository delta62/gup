package types

interface FunctionType {
    /**
     * The name of the built-in function
     */
    fun name(): String

    /**
     * The type signature of the built-in function
     */
    fun type(): Type.Function
}
