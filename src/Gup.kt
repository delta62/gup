import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Gup {
    companion object {
        private var hadError = false
        private var hadRuntimeError = false

        fun runSource(source: String) {
            try {
                run(source)
                if (hadError) throw CompileError()
                if (hadRuntimeError) throw RuntimeException("Runtime Error")
            } finally {
                hadError = false
                hadRuntimeError = false
            }
        }

        fun runFile(path: String) {
            val bytes = Files.readAllBytes(Paths.get(path))
            run(String(bytes, Charset.defaultCharset()))
            if (hadError) exitProcess(65)
            if (hadRuntimeError) exitProcess(70)
        }

        fun runPrompt() {
            val input = InputStreamReader(System.`in`)
            val reader = BufferedReader(input)

            while (true) {
                print("> ")
                val line = reader.readLine() ?: break
                run(line)
                hadError = false
            }
        }

        private fun run(source: String) {
            val scanner = Scanner(source)
            val tokens = scanner.scanTokens()
            val parser = Parser(tokens)
            val statements = parser.parse()
            val interpreter = Interpreter()

            if (hadError) return

            val resolver = Resolver(interpreter)
            resolver.resolve(statements)

            if (hadError) return

            interpreter.interpret(statements)
        }

        private fun report(line: Int, where: String, message: String) {
            println("[line $line] Error $where: $message")
            hadError = true
        }

        fun error(line: Int, message: String) {
            report(line, "", message)
        }

        fun error(token: Token, message: String) {
            if (token.type == TokenType.EOF) {
                report(token.line, " at end", message)
            } else {
                report(token.line, "at '${token.lexeme}'", message)
            }
        }

        fun runtimeError(error: RuntimeError) {
            System.err.println("${error.message}\n[line ${error.token.line}]")
            hadRuntimeError = true
        }
    }
}

fun main(args: Array<String>) {
    when (args.size) {
        0 -> Gup.runPrompt()
        1 -> Gup.runFile(args[0])
        else -> {
            println("Usage: sl [script]")
            exitProcess(64)
        }
    }
}
