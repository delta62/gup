import error.TypeError
import generated.Expr
import TokenType.*
import std.AssertEqual
import std.TypeOf
import types.Type
import types.TypeEnv
import types.TypeSource

class TypeChecker : Expr.Visitor<Type> {
    private var env = TypeEnv()
    private var functionReturnType: Type? = null

    init {
        env[Token(IDENTIFIER, "assertEqual", null, 0)] = AssertEqual.type()
        env[Token(IDENTIFIER, "typeof", null, 0)] = TypeOf.type()
    }

    fun typeCheck(expressions: List<Expr>) {
        for (expr in expressions) resolve(expr)
    }

    private fun resolve(expression: Expr): Type {
        return expression.accept(this)
    }

    override fun visitAssignExpr(expr: Expr.Assign): Type {
        val targetType = env[expr.name] ?: throw TypeError("$expr.name has not been initialized yet")
        val valueType = resolve(expr.value)

        if (!targetType.compatibleWith(valueType)) {
            throw TypeError("Cannot assign value of type $valueType to variable of type $targetType")
        }

        return targetType.merge(valueType)
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Type {
        val left = resolve(expr.left)
        val right = resolve(expr.right)

        if (!left.compatibleWith(right)) {
            throw TypeError("Type $left is incompatible with $right")
        }

        return left.merge(right)
    }

    override fun visitBlockExpr(expr: Expr.Block): Type {
        var ret: Type = Type.Unit(TypeSource.InferredStrong)

        for (expression in expr.expressions) {
            ret = resolve(expression)
        }

        return ret
    }

    override fun visitBreakExpr(expr: Expr.Break): Type {
        return Type.Unit(TypeSource.ByDefinition)
    }

    override fun visitCallExpr(expr: Expr.Call): Type {
        if (expr.callee !is Expr.Variable) TODO("typing non-identifier functions not implemented yet")
        val callee = env[expr.callee.name]

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
        TODO("Not yet implemented")
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
        val annotatedType = if (expr.name.type != null) Type.byName(expr.name.type.lexeme) else null

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

        env[expr.name.identifier] = type

        return type
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Type {
        return when (expr.value) {
            is String -> Type.String(TypeSource.Hardcoded)
            is ULong -> Type.ULong(TypeSource.Hardcoded)
            is Long -> Type.Long(TypeSource.Hardcoded)
            is Double -> Type.Double(TypeSource.Hardcoded)
            is Boolean -> Type.Bool(TypeSource.Hardcoded)
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
        return env[expr.name] ?: throw TypeError("Type for ${expr.name} not defined")
    }

    private inline fun <reified T> expectType(expr: Expr) {
        val actualType = resolve(expr)
        if (actualType !is T) throw TypeError("Expected ${T::class.simpleName} but got type $actualType")
    }
}
