package types

import error.TypeError

sealed class Type(val source: TypeSource) {
    /**
     * Any type, with no further information available.
     *
     * This is a temporary hack until type classes are implemented.
     */
    class Any(source: TypeSource) : Type(source) {
        override fun toString(): kotlin.String = "any"

        override fun equals(other: kotlin.Any?) = other is Any && other.source == source
    }

    class Bool(source: TypeSource) : Type(source) {
        override fun toString(): kotlin.String = "bool"
    }

    class Double(source: TypeSource) : Type(source) {
        override fun toString(): kotlin.String = "double"
    }

    class Function(
        source: TypeSource,
        val parameters: kotlin.collections.List<Type>,
        val returnType: Type
    ) : Type(source) {
        override fun toString(): kotlin.String {
            val paramTypes = parameters.joinToString(", ") { p -> p.toString() }
            return "function<$paramTypes>: $returnType"
        }
    }

    /**
     * A homogeneous list of items
     */
    class List(source: TypeSource, val items: Type) : Type(source) {
        override fun toString(): kotlin.String {
            return "list<$items>"
        }
    }

    class Struct(val fields: Map<String, Type>) : Type(TypeSource.Hardcoded) {
        override fun toString(): kotlin.String {
            val fieldNames = fields.entries.joinToString(", ") { (k, v) -> "$k: $v" }
            return "struct<$fieldNames>"
        }
    }

    class Long(source: TypeSource) : Type(source) {
        override fun toString(): kotlin.String = "int"
    }

    class String(source: TypeSource) : Type(source) {
        override fun toString(): kotlin.String = "string"
    }

    class ULong(source: TypeSource) : Type(source) {
        override fun toString(): kotlin.String = "uint"
    }

    class Unit(source: TypeSource) : Type(source) {
        override fun toString(): kotlin.String = "unit"

        override fun equals(other: kotlin.Any?) = other is Unit && other.source == source
    }

    class Unspecified : Type(TypeSource.InferredWeak) {
        override fun toString(): kotlin.String = "unspecified"

        override fun equals(other: kotlin.Any?) = other is Unspecified
    }

    fun compatibleWith(other: Type): Boolean {
        // Any is compatible with anything by definition
        if (this is Any || other is Any) return true

        // Functions with the same arity & types can be interchanged
        if (this is Function && other is Function) {
            val compatibleReturn = this.returnType.compatibleWith(other.returnType)

            var compatibleParams = parameters.size == other.parameters.size
            for (i in parameters.indices) {
                if (!compatibleParams) break
                compatibleParams = parameters[i].compatibleWith(other.parameters[i])
            }

            return compatibleReturn && compatibleParams
        }

        if (this is Struct && other is Struct) {
            var compatibleFields = fields.size == other.fields.size
            for ((k, v) in fields.entries) {
                if (!compatibleFields) break
                compatibleFields = other.fields.containsKey(k) && v.compatibleWith(other.fields[k]!!)
            }

            return compatibleFields
        }

        if (this is List && other is List) {
            return this.items.compatibleWith(other.items)
        }

        // Things with the same type are interchangeable
        if (this.javaClass == other.javaClass) return true

        // ULong & Long can be interchanged if they were resolved via weak reference
        if (this is ULong && other is Long || this is Long && other is ULong) {
            return this.source == TypeSource.InferredWeak || other.source == TypeSource.InferredWeak
        }

        // Nothing else is compatible
        return false
    }

    fun merge(other: Type): Type {
        if (!compatibleWith(other)) throw TypeError("Type $this is not compatible with $other")

        // When using `Any`, other types are generalized, and we use the most specific source available
        if (this is Any || other is Any) {
            val src = if (source > other.source) source else other.source
            return Any(src)
        }

        return if (other.source > source) other else this
    }

    fun couldBeSigned(): Boolean {
        return this is Long && source == TypeSource.InferredWeak
    }

    fun convertToSigned(): Type {
        if (!couldBeSigned()) throw TypeError("Illegal conversion of $this to a signed integer")
        return ULong(TypeSource.InferredStrong)
    }

    companion object {
        /**
         * Convenience function for turning a string literal into a type.
         * Only works for primitive types
         */
        fun parsePrimitive(name: kotlin.String): Type {
            return when (name) {
                "int" -> Long(TypeSource.Hardcoded)
                "uint" -> ULong(TypeSource.Hardcoded)
                "unit" -> Unit(TypeSource.Hardcoded)
                "bool" -> Bool(TypeSource.Hardcoded)
                "double" -> Double(TypeSource.Hardcoded)
                "string" -> String(TypeSource.Hardcoded)
                else -> throw TypeError("Unknown type '$name'")
            }
        }
    }
}
