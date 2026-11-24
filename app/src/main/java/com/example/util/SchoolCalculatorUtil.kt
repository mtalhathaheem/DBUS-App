package com.example.util

import java.util.Stack

/**
 * School Calculator Utility for DBUS Pakistan School System
 * Supports:
 * 1. Arithmetic evaluation (+, -, *, /, %, parentheses)
 * 2. Attendance percentage & status calculation (<75% alert)
 * 3. Pakistani Board Grade & GPA calculation (A+, A, B, C, D, F)
 */
object SchoolCalculatorUtil {

    data class GradeResult(
        val percentage: Float,
        val gradeLetter: String,
        val urduGrade: String,
        val gpa: Float,
        val remarks: String,
        val isPass: Boolean
    )

    /**
     * Pakistani Board Grading Formula
     */
    fun calculateGrade(obtainedMarks: Double, totalMarks: Double): GradeResult {
        if (totalMarks <= 0.0) {
            return GradeResult(0f, "F", "فیل", 0.0f, "Invalid total marks", false)
        }
        val pct = ((obtainedMarks / totalMarks) * 100.0).toFloat().coerceIn(0f, 100f)
        return when {
            pct >= 80f -> GradeResult(pct, "A+", "ممتاز (A+)", 4.0f, "Outstanding performance", true)
            pct >= 70f -> GradeResult(pct, "A", "بہت خوب (A)", 3.7f, "Excellent performance", true)
            pct >= 60f -> GradeResult(pct, "B", "اچھا (B)", 3.0f, "Good performance", true)
            pct >= 50f -> GradeResult(pct, "C", "تسلی بخش (C)", 2.0f, "Satisfactory performance", true)
            pct >= 40f -> GradeResult(pct, "D", "پاس (D)", 1.0f, "Pass grade", true)
            else -> GradeResult(pct, "F", "فیل (F)", 0.0f, "Needs support & improvement", false)
        }
    }

    /**
     * Attendance percentage calculation with alert trigger
     */
    fun calculateAttendancePercentage(presentDays: Int, totalWorkingDays: Int): Pair<Float, Boolean> {
        if (totalWorkingDays <= 0) return 100f to false
        val rate = (presentDays.toFloat() / totalWorkingDays.toFloat() * 100f).coerceIn(0f, 100f)
        val isLow = rate < 75.0f
        return rate to isLow
    }

    /**
     * Arithmetic expression evaluator (handles +, -, *, /, %, parentheses)
     */
    fun evaluateExpression(expr: String): Result<Double> {
        val sanitized = expr.replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")

        if (sanitized.isBlank()) return Result.success(0.0)

        return try {
            val tokens = tokenize(sanitized)
            val result = parseAndEvaluate(tokens)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid arithmetic expression: ${e.message}"))
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    tokens.add(sb.toString())
                    continue
                }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '(' || c == ')' -> {
                    // Check for unary minus
                    if (c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/", "%", "("))) {
                        // Unary minus attached to next number
                        i++
                        val sb = StringBuilder("-")
                        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                            sb.append(expr[i])
                            i++
                        }
                        tokens.add(sb.toString())
                        continue
                    } else {
                        tokens.add(c.toString())
                    }
                }
            }
            i++
        }
        return tokens
    }

    private fun parseAndEvaluate(tokens: List<String>): Double {
        val values = Stack<Double>()
        val ops = Stack<String>()

        fun applyOp() {
            if (values.size < 2 || ops.isEmpty()) return
            val op = ops.pop()
            val b = values.pop()
            val a = values.pop()
            val res = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b == 0.0) throw ArithmeticException("Division by zero") else a / b
                "%" -> a % b
                else -> 0.0
            }
            values.push(res)
        }

        fun precedence(op: String): Int = when (op) {
            "+", "-" -> 1
            "*", "/", "%" -> 2
            else -> 0
        }

        for (token in tokens) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                values.push(num)
            } else if (token == "(") {
                ops.push(token)
            } else if (token == ")") {
                while (ops.isNotEmpty() && ops.peek() != "(") {
                    applyOp()
                }
                if (ops.isNotEmpty() && ops.peek() == "(") {
                    ops.pop()
                }
            } else if (token in listOf("+", "-", "*", "/", "%")) {
                while (ops.isNotEmpty() && precedence(ops.peek()) >= precedence(token)) {
                    applyOp()
                }
                ops.push(token)
            }
        }

        while (ops.isNotEmpty()) {
            applyOp()
        }

        return if (values.isNotEmpty()) values.pop() else 0.0
    }
}
