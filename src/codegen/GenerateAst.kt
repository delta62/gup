package codegen

import java.io.PrintWriter
import java.util.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output_directory>")
        exitProcess(64)
    }

    val outputDir = args[0]

    defineAst(outputDir, "Expr", listOf(
        "Assign   : val name: Token, val value: Expr",
        "Binary   : val left: Expr, val operator: Token, val right: Expr",
        "Grouping : val expression: Expr",
        "Literal  : val value: Any",
        "Logical  : val left: Expr, val operator: Token, val right: Expr",
        "Unary    : val operator: Token, val right: Expr",
        "Variable : val name: Token"
    ))

    defineAst(outputDir, "Stmt", listOf(
        "Block      : val statements: List<Stmt>",
        "Break      : val token: Token",
        "Continue   : val token: Token",
        "Expression : val expression: Expr",
        "ForLoop    : val name: Token, val iterator: Token, val body: Stmt",
        "If         : val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?",
        "Print      : val expression: Expr",
        "Let        : val name: Token, val initializer: Expr?",
        "Loop       : val body: Stmt",
        "While      : val condition: Expr, val body: Stmt",
    ))

    println("Done!")
}

private fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, "UTF-8")

    println("Generating $path")

    writer.println("sealed class $baseName {")

    defineVisitor(writer, baseName, types)

    for (type in types) {
        var (className, fields) = type.split(':', limit = 2)
        className = className.trim()
        fields = fields.trimStart()
        defineType(writer, baseName, className, fields)
    }

    writer.println()
    writer.println("    abstract fun <R> accept(visitor: Visitor<R>): R")

    writer.println("}")
    writer.close()
}

private fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
    println("  Generating $baseName.$className")
    writer.println("    data class $className($fieldList) : $baseName() {")

    writer.println("        override fun <R> accept(visitor: Visitor<R>): R {")
    writer.println("            return visitor.visit$className$baseName(this)")
    writer.println("        }")

    writer.println("    }")
}

private fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
    writer.println("    interface Visitor<R> {")

    for (type in types) {
        val typeName = type.split(':')[0].trim()
        writer.println("        fun visit$typeName$baseName(${baseName.lowercase(Locale.getDefault())}: $typeName): R")
    }

    writer.println("    }")
    writer.println()
}
