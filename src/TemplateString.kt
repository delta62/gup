import generated.Expr

sealed class TemplateString {
    class Expression(val expression: Expr) : TemplateString()
    class Text(val text: String) : TemplateString()

    override fun toString(): String {
        return when (this) {
            is Expression -> expression.toString()
            is Text -> text
        }
    }
}
