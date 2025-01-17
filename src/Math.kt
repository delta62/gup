import error.Unreachable

class Math {
    companion object {
        fun mul(x: Any, y: Any): Any {
            return when (x) {
                is Long -> x * y as Long
                is ULong -> x * y as ULong
                is Double -> x * y as Double
                else -> throw Unreachable()
            }
        }

        fun div(x: Any, y: Any): Any {
            return when (x) {
                is Long -> x / y as Long
                is ULong -> x / y as ULong
                is Double -> x / y as Double
                else -> throw Unreachable()
            }
        }

        fun add(x: Any, y: Any): Any {
            return when (x) {
                is Long -> x + y as Long
                is ULong -> x + y as ULong
                is Double -> x + y as Double
                else -> throw Unreachable()
            }
        }

        fun sub(x: Any, y: Any): Any {
            return when (x) {
                is Long -> x - y as Long
                is ULong -> x - toULong(y)
                is Double -> x - y as Double
                else -> throw Unreachable()
            }
        }

        fun toLong(x: Any): Long {
            return when (x) {
                is ULong -> x.toLong()
                is Long -> x
                is Double -> x.toLong()
                else -> throw Unreachable()
            }
        }

        fun toULong(x: Any): ULong {
            return when (x) {
                is ULong -> x
                is Long -> x.toULong()
                is Double -> x.toULong()
                else -> throw Unreachable()
            }
        }

        fun shl(x: Any, y: Any): Any {
            return when (x) {
                is Long -> x.shl((y as Long).toInt())
                is ULong -> x.shl((y as Long).toInt())
                else -> throw Unreachable()
            }
        }

        fun shr(x: Any, y: Any): Any {
            return when (x) {
                is Long -> x.shr((y as Long).toInt())
                is ULong -> x.shr((y as Long).toInt())
                else -> throw Unreachable()
            }
        }

        fun negate(x: Any): Any {
            return when (x) {
                is Long -> -x
                is Double -> -x
                else -> throw Unreachable()
            }
        }

        fun bitwiseAnd(x: Any, y: Any): Any {
            return when (x) {
                is Long -> x.and(y as Long)
                is ULong -> x.and(y as ULong)
                else -> throw Unreachable()
            }
        }

        fun bitwiseOr(x: Any, y: Any): Any {
            return when (x) {
                is Long -> x.or(y as Long)
                is ULong -> x.or(y as ULong)
                else -> throw Unreachable()
            }
        }

        fun bitwiseXor(x: Any, y: Any): Any {
            return when (x) {
                is Long -> x.xor(y as Long)
                is ULong -> x.xor(y as ULong)
                else -> throw Unreachable()
            }
        }

        fun rem(x: Any, y: Any): Any {
            return when (x) {
                is Long -> x.rem(y as Long)
                is ULong -> x.rem(y as ULong)
                else -> throw Unreachable()
            }
        }

        fun less(x: Any, y: Any): Boolean {
            return when (x) {
                is Long -> x < y as Long
                is ULong -> x < y as ULong
                is Double -> x < y as Double
                else -> throw Unreachable()
            }
        }

        fun lessEq(x: Any, y: Any): Boolean {
            return when (x) {
                is Long -> x <= y as Long
                is ULong -> x <= y as ULong
                is Double -> x <= y as Double
                else -> throw Unreachable()
            }
        }

        fun greater(x: Any, y: Any): Boolean {
            return when (x) {
                is Long -> x > y as Long
                is ULong -> x > y as ULong
                is Double -> x > y as Double
                else -> throw Unreachable()
            }
        }

        fun greaterEq(x: Any, y: Any): Boolean {
            return when (x) {
                is Long -> x >= y as Long
                is ULong -> x >= y as ULong
                is Double -> x >= y as Double
                else -> throw Unreachable()
            }
        }
    }
}
