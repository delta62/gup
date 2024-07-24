class Argv {
    companion object {
        private lateinit var ARGS: List<String>

        fun set(args: List<String>) {
            ARGS = args
        }

        fun get(): List<String> {
            return ARGS
        }
    }
}
