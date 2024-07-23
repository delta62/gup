package types

enum class TypeSource {
    // NOTE: Order of these types is important to support comparison operators

    /**
     * A weakly inferred type that is subject to change if the compiler gets
     * better information later. For example, `let x = 42` could be an int or
     * a uint.
     */
    InferredWeak,

    /**
     * A strongly inferred type that cannot be recalculated as a different type.
     * For example, `let x = 3.21` can only be a double.
     */
    InferredStrong,

    /**
     * Cases where the user has explicitly stated what the type is, such
     * as `let x: int = 42`
     */
    Hardcoded,

    /**
     * Things that are by definition a certain type and the user has no control over.
     * For example,
     *
     * ```
     * while condition -> thing()
     * ```
     *
     * will always have the type `unit` as loops never resolve to a type
     */
    ByDefinition
}
