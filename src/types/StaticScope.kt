package types

class StaticScope<K, V>(private val parent: StaticScope<K, V>? = null) {
    private val values = HashMap<K, V>()

    operator fun get(key: K): V? {
        val localEntry = values.entries.find { (k, _) -> k == key }
        if (localEntry != null) return localEntry.value

        return parent?.get(key)
    }

    operator fun set(key: K, value: V) {
        values[key] = value
    }
}
