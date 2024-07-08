package codegen

import java.io.PrintWriter
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output_directory>")
        exitProcess(64)
    }

    val outputDir = args[0]

    defineAst(outputDir, listOf(
        "Assign   : val name: Token, val value: Expr",
        "Binary   : val left: Expr, val operator: Token, val right: Expr",
        "Block    : val expressions: List<Expr>",
        "Break    : val token: Token",
        "Call     : val callee: Expr, val paren: Token, val arguments: List<Expr>",
        "Continue : val token: Token",
        "ForLoop  : val name: Token, val iterator: Token, val body: List<Expr>",
        "Function : val name: Token?, val params: List<Token>, val body: List<Expr>",
        "Grouping : val expression: Expr",
        "If       : val condition: Expr, val thenBranch: List<Expr>, val elseBranch: List<Expr>?",
        "Let      : val name: Token, val initializer: Expr?",
        "Literal  : val value: Any",
        "Logical  : val left: Expr, val operator: Token, val right: Expr",
        "Loop     : val body: Expr",
        "Return   : val keyword: Token, val value: Expr",
        "Unary    : val operator: Token, val right: Expr",
        "Variable : val name: Token",
        "While    : val condition: Expr, val body: Expr",
    ))

    println("Done!")
}

private fun defineAst(outputDir: String, types: List<String>) {
    val path = "$outputDir/Expr.kt"
    val writer = PrintWriter(path, "UTF-8")

    println("Generating $path")

    writer.println("sealed class Expr {")

    defineVisitor(writer, types)

    for (type in types) {
        var (className, fields) = type.split(':', limit = 2)
        className = className.trim()
        fields = fields.trimStart()
        defineType(writer, className, fields)
    }

    writer.println("    abstract fun <R> accept(visitor: Visitor<R>): R")

    writer.println("}")
    writer.close()
}

private fun defineType(writer: PrintWriter, className: String, fieldList: String) {
    println("  Generating Expr.$className")

    writer.println("    data class $className($fieldList) : Expr() {")
    writer.println("        override fun <R> accept(visitor: Visitor<R>): R {")
    writer.println("            return visitor.visit${className}Expr(this)")
    writer.println("        }")
    writer.println("    }")
    writer.println()
}

private fun defineVisitor(writer: PrintWriter, types: List<String>) {
    writer.println("    interface Visitor<R> {")

    for (type in types) {
        val typeName = type.split(':')[0].trim()
        writer.println("        fun visit${typeName}Expr(expr: $typeName): R")
    }

    writer.println("    }")
    writer.println()
}
