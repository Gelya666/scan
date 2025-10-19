package com.example.scanner.utils

fun Thread.getFormattedStackTrace(): String {
    return buildString {
        appendLine("Stack trace:")
        stackTrace.forEachIndexed { index, element ->
            if (index >= 2) { // Пропускаем системные вызовы
                appendLine("  at ${element.className}.${element.methodName} (${element.fileName}:${element.lineNumber})")
            }
        }
    }
}