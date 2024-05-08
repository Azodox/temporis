package fr.azodox.test

var list = mutableListOf<String>()

fun main() {
    list.add(0, "a")
    list.add(1, "b")

    list[0] = "b"
    println(list)
}