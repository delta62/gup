import error.TypeError
import generated.Expr
import TokenType.*
import std.*
import types.*
import types.FunctionType
import kotlin.reflect.KClass

class TypeChecker : Expr.Visitor<Type> {
    private var env = StaticScope<String, Type>()
    private var functionReturnType: Type? = null

    private fun builtin(func: FunctionType) {
        val token = Token(IDENTIFIER, func.name(), null, 0)
        env[token.lexeme] = func.type()
    }

    init {
        builtin(Args)
        builtin(AssertEqual)
        builtin(Epoch)
        builtin(Iterate)
        builtin(Len)
        builtin(Next)
        builtin(PrintLine)
        builtin(RandInt)
        builtin(ReadLine)
        builtin(Swap)
        builtin(TypeOf)
    }

    fun typeCheck(expressions: List<Expr>) {
        expressions.forEach { expr -> resolve(expr) }
    }

    private fun resolve(expression: Expr): Type {
        return expression.accept(this)
    }

    override fun visitAssignExpr(expr: Expr.Assign): Type {
        val targetType = env[expr.name.lexeme] ?: throw TypeError("$expr.name has not been initialized yet")
        val valueType = resolve(expr.value)
        return targetType.merge(valueType)
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Type {
        return when (expr.operator.type) {
            AMPERSAND, CARET, PIPE, MINUS, STAR, SLASH, GREATER_GREATER, LESS_LESS, PERCENT -> {
                val left = expectOneOf(expr.left, Type.ULong::class, Type.Long::class)
                val right = resolve(expr.right)
                left.merge(right)
            }

            LESS, LESS_EQ, GREATER, GREATER_EQ, IS, ISNT -> {
                val left = expectOneOf(expr.left, Type.ULong::class, Type.Long::class, Type.Double::class, Type.String::class)
                val right = resolve(expr.right)
                expectCompatible(left, right)
                Type.Bool(TypeSource.ByDefinition)
            }

            PLUS -> {
                val left = expectOneOf(expr.left, Type.ULong::class, Type.Long::class, Type.Double::class, Type.String::class)
                val right = resolve(expr.right)
                expectCompatible(left, right)
                left.merge(right)
            }

            DOT, DOLLAR -> {
                val left = resolve(expr.left)
                val right = resolve(expr.right)

                if (left !is Type.Function || right !is Type.Function) {
                    throw TypeError("Cannot compose non-function values")
                }

                val paramRange = 0..<left.parameters.size - 1
                val composedParams = left.parameters.slice(paramRange).toMutableList()
                composedParams += right.parameters
                Type.Function(TypeSource.InferredStrong, composedParams, left.returnType)
            }

            DOT_DOT, DOT_DOT_EQ -> Type.Struct(emptyMap())

            else -> throw TypeError("Unsupported binary operator '${expr.operator.lexeme}'")
        }
    }

    override fun visitBlockExpr(expr: Expr.Block): Type {
        val init: Type = Type.Unit(TypeSource.InferredStrong)
        return expr.expressions.fold(init) { _, x -> resolve(x) }
    }

    override fun visitBreakExpr(expr: Expr.Break): Type {
        return Type.Unit(TypeSource.ByDefinition)
    }

    override fun visitCallExpr(expr: Expr.Call): Type {
        if (expr.callee !is Expr.Variable) return Type.Any(TypeSource.Hardcoded) // TODO("typing non-identifier functions not implemented yet")
        val callee = env[expr.callee.name.lexeme]

        if (callee !is Type.Function) throw TypeError("Can't call '$callee', not a function")
        if (expr.arguments.size > callee.parameters.size) throw TypeError("Too many arguments provided to '$callee'")

        // ensure first n args are compatible
        val argTypes = expr.arguments.map { arg -> resolve(arg) }
        for ((expected, actual) in callee.parameters.zip(argTypes)) {
            if (!expected.compatibleWith(actual)) {
                throw TypeError("Parameter $actual $callee is not compatible with $expected")
            }
        }

        return if (expr.arguments.size < callee.parameters.size) {
            argTypes.drop(expr.arguments.size)
            Type.Function(TypeSource.InferredWeak, argTypes, callee.returnType)
        } else {
            callee.returnType
        }
    }

    override fun visitContinueExpr(expr: Expr.Continue): Type {
        return Type.Unit(TypeSource.ByDefinition)
    }

    override fun visitFunctionExpr(expr: Expr.Function): Type {
        val returnType = if (expr.returnType != null) Type.parsePrimitive(expr.returnType.lexeme) else Type.Unspecified()
        val parameters = expr.params.map { p -> if (p.type != null) Type.parsePrimitive(p.type.lexeme) else Type.Unspecified() }
        val type = Type.Function(TypeSource.Hardcoded, parameters, returnType)

        if (expr.name != null) env[expr.name.lexeme] = type

        return type
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Type {
        return resolve(expr.expression)
    }

    override fun visitIfExpr(expr: Expr.If): Type {
        expectType<Type.Bool>(expr.condition)

        val thenType = resolve(expr.thenBranch)
        val elseType = if (expr.elseBranch != null) resolve(expr.elseBranch) else null

        if (elseType == null) return thenType
        return if (thenType::class == elseType::class) {
            if (thenType.source > elseType.source) thenType else elseType
        } else {
            Type.Unit(TypeSource.InferredStrong)
        }
    }

    override fun visitLetExpr(expr: Expr.Let): Type {
        val actualType = if (expr.initializer != null) resolve(expr.initializer) else null
        val annotatedType = if (expr.name.type != null) Type.parsePrimitive(expr.name.type.lexeme) else null

        val type = if (actualType == null && annotatedType == null) {
            // let x
            throw TypeError("Variable declarations must have a type specified")
        } else if (actualType == null) {
            // let x: int
            annotatedType!!
        } else if (annotatedType == null) {
            // let x = 42
            actualType
        } else {
            actualType.merge(annotatedType)
        }

        env[expr.name.identifier.lexeme] = type

        return type
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Type {
        return when (val value = expr.value) {
            is String -> Type.String(TypeSource.Hardcoded)
            is ULong -> Type.ULong(TypeSource.Hardcoded)
            is Long -> Type.Long(TypeSource.Hardcoded)
            is Double -> Type.Double(TypeSource.Hardcoded)
            is Boolean -> Type.Bool(TypeSource.Hardcoded)
            is GupUnit -> Type.Unit(TypeSource.Hardcoded)
            is GupList -> {
                val init: Type = Type.Unspecified()
                val itemType = value.fold(init) { acc, x ->
                    val resolved = resolve(x as Expr)
                    if (acc == Type.Unspecified()) resolved
                    else acc.merge(resolved)
                }

                Type.List(itemType.source, itemType)
            }
            else -> throw TypeError("Unknown type ${expr.value}")
        }
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Type {
        expectType<Type.Bool>(expr.left)
        expectType<Type.Bool>(expr.right)
        return Type.Bool(TypeSource.InferredStrong)
    }

    override fun visitLoopExpr(expr: Expr.Loop): Type {
        expectType<Type.Bool>(expr.condition)
        resolve(expr.body)
        return Type.Unit(TypeSource.ByDefinition)
    }

    override fun visitReturnExpr(expr: Expr.Return): Type {
        if (functionReturnType == null) {
            throw TypeError("Cannot return from outside of a function")
        }

        val returnType = resolve(expr.value)
        if (!returnType.compatibleWith(functionReturnType!!)) {
            throw TypeError("Expected return type of $functionReturnType, but got $returnType")
        }

        return Type.Unit(TypeSource.ByDefinition)
    }

    override fun visitTemplateExpr(expr: Expr.Template): Type {
        return Type.String(TypeSource.Hardcoded)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Type {
        return when (expr.operator.type) {
            NOT, PLUS, TILDE, CARET -> resolve(expr.right)
            MINUS -> {
                val type = resolve(expr.right)
                if (type.couldBeSigned()) type.convertToSigned() else type
            }
            else -> throw TypeError("Unexpected unary operator ${expr.operator}")
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Type {
        return env[expr.name.lexeme] ?: throw TypeError("Type for variable '${expr.name}' is not defined")
    }

    private inline fun <reified T> expectType(expr: Expr) {
        val actualType = resolve(expr)
        if (actualType is Type.Any) return
        if (actualType !is T) throw TypeError("Expected ${T::class.simpleName} but got type $actualType")
    }

    private fun expectOneOf(expr: Expr, vararg foo: KClass<*>): Type {
        val resolved = resolve(expr)
        if (resolved is Type.Any) return resolved

        if (foo.any { klass -> klass == resolved::class }) return resolved
        throw TypeError("Unexpected type $resolved")
    }

    private fun expectCompatible(a: Type, b: Type) {
        if (a.compatibleWith(b)) return
        throw TypeError("Type $a is incompatible with $b")
    }
}
