package de.visustruct.simulation

import kotlin.math.abs
import kotlin.math.round
import kotlin.random.Random

internal class SimulationExprException(message: String) : Exception(message)

internal sealed class NumericToken {
    data object PLUS : NumericToken()
    data object MINUS : NumericToken()
    data object MULTIPLY : NumericToken()
    data object DIVIDE : NumericToken()
    data object MODULO : NumericToken()
    data object DOT : NumericToken()
    data object LEFT_PAREN : NumericToken()
    data object RIGHT_PAREN : NumericToken()
    data object LEFT_BRACKET : NumericToken()
    data object RIGHT_BRACKET : NumericToken()

    data class NumberLiteral(val literal: String, val value: Double) : NumericToken()
    data class Identifier(val name: String) : NumericToken()
}

internal object NumericExpressionTokenizer {
    fun tokenize(expression: String): List<NumericToken> {
        val tokens = mutableListOf<NumericToken>()
        var i = 0
        val e = expression
        while (i < e.length) {
            val char = e[i]
            if (char.isWhitespace()) {
                i++
                continue
            }
            if (char.isDigit() || (char == '.' && i + 1 < e.length && e[i + 1].isDigit())) {
                val start = i
                var dotCount = 0
                while (i < e.length) {
                    val c = e[i]
                    if (c == '.') {
                        dotCount++
                        if (dotCount > 1) throw SimulationExprException("Invalid expression: $expression")
                    } else if (!c.isDigit()) break
                    i++
                }
                val literal = e.substring(start, i)
                if (literal == "." || literal.toDoubleOrNull() == null) {
                    throw SimulationExprException("Invalid expression: $expression")
                }
                tokens.add(NumericToken.NumberLiteral(literal, literal.toDouble()))
                continue
            }
            if (char.isLetter() || char == '_') {
                val start = i
                while (i < e.length) {
                    val c = e[i]
                    if (!c.isLetterOrDigit() && c != '_') break
                    i++
                }
                tokens.add(NumericToken.Identifier(e.substring(start, i)))
                continue
            }
            when (char) {
                '+' -> tokens.add(NumericToken.PLUS)
                '-' -> tokens.add(NumericToken.MINUS)
                '*' -> tokens.add(NumericToken.MULTIPLY)
                '/' -> tokens.add(NumericToken.DIVIDE)
                '%' -> tokens.add(NumericToken.MODULO)
                '.' -> tokens.add(NumericToken.DOT)
                '(' -> tokens.add(NumericToken.LEFT_PAREN)
                ')' -> tokens.add(NumericToken.RIGHT_PAREN)
                '[' -> tokens.add(NumericToken.LEFT_BRACKET)
                ']' -> tokens.add(NumericToken.RIGHT_BRACKET)
                else -> throw SimulationExprException("Invalid expression: $expression")
            }
            i++
        }
        return tokens
    }
}

internal class NumericExpressionParser(
    private val tokens: List<NumericToken>,
    private val variables: Map<String, SimulationValue>,
) {
    private var index = 0

    fun parseInt(): Int {
        val value = parseDoubleExpression()
        if (index != tokens.size) throw SimulationExprException("Invalid expression: ")
        if (!value.isFinite()) throw SimulationExprException("Invalid expression: ")
        return value.toInt()
    }

    fun parseDouble(): Double {
        val value = parseDoubleExpression()
        if (index != tokens.size) throw SimulationExprException("Invalid expression: ")
        return value
    }

    private fun parseIntExpression(): Int {
        var value = parseIntTerm()
        while (true) {
            val t = current() ?: break
            if (t !is NumericToken.PLUS && t !is NumericToken.MINUS) break
            index++
            val rhs = parseIntTerm()
            value = if (t is NumericToken.PLUS) value + rhs else value - rhs
        }
        return value
    }

    private fun parseIntTerm(): Int {
        var value = parseIntFactor()
        while (true) {
            val t = current() ?: break
            if (t !is NumericToken.MULTIPLY && t !is NumericToken.DIVIDE && t !is NumericToken.MODULO) break
            index++
            val rhs = parseIntFactor()
            when (t) {
                NumericToken.MULTIPLY -> value *= rhs
                NumericToken.DIVIDE -> {
                    if (rhs == 0) throw SimulationExprException("Division by zero.")
                    value /= rhs
                }
                NumericToken.MODULO -> {
                    if (rhs == 0) throw SimulationExprException("Division by zero.")
                    value %= rhs
                }
                else -> {}
            }
        }
        return value
    }

    private fun parseIntFactor(): Int {
        val token = current() ?: throw SimulationExprException("Invalid expression: ")
        index++
        when (token) {
            is NumericToken.NumberLiteral -> {
                if (token.literal.contains('.') || abs(token.value - round(token.value)) > 1e-12) {
                    throw SimulationExprException("Invalid expression: ${token.literal}")
                }
                return token.value.toInt()
            }
            is NumericToken.Identifier -> {
                val v = variables[token.name] ?: throw SimulationExprException("Unknown variable: ${token.name}")
                when (v) {
                    is SimulationValue.VInt -> return v.value
                    else -> throw SimulationExprException("Invalid expression: ${token.name}")
                }
            }
            NumericToken.MINUS -> return -parseIntFactor()
            NumericToken.LEFT_PAREN -> {
                val v = parseIntExpression()
                if (current() != NumericToken.RIGHT_PAREN) throw SimulationExprException("Invalid expression: ")
                index++
                return v
            }
            else -> throw SimulationExprException("Invalid expression: ")
        }
    }

    private fun parseDoubleExpression(): Double {
        var value = parseDoubleTerm()
        while (true) {
            val t = current() ?: break
            if (t !is NumericToken.PLUS && t !is NumericToken.MINUS) break
            index++
            val rhs = parseDoubleTerm()
            value = if (t is NumericToken.PLUS) value + rhs else value - rhs
        }
        return value
    }

    private fun parseDoubleTerm(): Double {
        var value = parseDoubleFactor()
        while (true) {
            val t = current() ?: break
            if (t !is NumericToken.MULTIPLY && t !is NumericToken.DIVIDE && t !is NumericToken.MODULO) break
            index++
            val rhs = parseDoubleFactor()
            when (t) {
                NumericToken.MULTIPLY -> value *= rhs
                NumericToken.DIVIDE -> {
                    if (rhs == 0.0) throw SimulationExprException("Division by zero.")
                    value /= rhs
                }
                NumericToken.MODULO -> {
                    if (rhs == 0.0) throw SimulationExprException("Division by zero.")
                    value %= rhs
                }
                else -> {}
            }
        }
        return value
    }

    private fun parseDoubleFactor(): Double {
        val token = current() ?: throw SimulationExprException("Invalid expression: ")
        index++
        when (token) {
            is NumericToken.NumberLiteral -> return token.value
            is NumericToken.Identifier -> {
                if (token.name == "Math" && current() == NumericToken.DOT) {
                    index++
                    val id = current() as? NumericToken.Identifier
                        ?: throw SimulationExprException("Invalid expression: ${token.name}")
                    if (id.name != "random") throw SimulationExprException("Invalid expression: ${token.name}")
                    index++
                    if (current() != NumericToken.LEFT_PAREN) throw SimulationExprException("Invalid expression: Math.random")
                    index++
                    if (current() != NumericToken.RIGHT_PAREN) throw SimulationExprException("Invalid expression: Math.random")
                    index++
                    return Random.nextDouble(0.0, 1.0)
                }
                val value = variables[token.name] ?: throw SimulationExprException("Unknown variable: ${token.name}")
                if (current() == NumericToken.DOT) {
                    index++
                    val field = current() as? NumericToken.Identifier
                        ?: throw SimulationExprException("Invalid expression: ${token.name}")
                    if (field.name != "length") throw SimulationExprException("Invalid expression: ${token.name}")
                    index++
                    if (current() == NumericToken.LEFT_PAREN) {
                        index++
                        if (current() != NumericToken.RIGHT_PAREN) {
                            throw SimulationExprException("Invalid expression: ${token.name}.length")
                        }
                        index++
                    }
                    return when (value) {
                        is SimulationValue.VIntArray -> value.values.size.toDouble()
                        is SimulationValue.VDoubleArray -> value.values.size.toDouble()
                        is SimulationValue.VStringArray -> value.values.size.toDouble()
                        else -> throw SimulationExprException("Invalid expression: ${token.name}")
                    }
                }
                if (current() == NumericToken.LEFT_BRACKET) {
                    index++
                    val elementIndex = parseDoubleExpression().toInt()
                    if (current() != NumericToken.RIGHT_BRACKET) {
                        throw SimulationExprException("Invalid expression: ${token.name}")
                    }
                    index++
                    return when (value) {
                        is SimulationValue.VIntArray -> {
                            if (elementIndex !in value.values.indices) {
                                throw SimulationExprException("Invalid expression: ${token.name}[$elementIndex]")
                            }
                            value.values[elementIndex].toDouble()
                        }
                        is SimulationValue.VDoubleArray -> {
                            if (elementIndex !in value.values.indices) {
                                throw SimulationExprException("Invalid expression: ${token.name}[$elementIndex]")
                            }
                            value.values[elementIndex]
                        }
                        else -> throw SimulationExprException("Invalid expression: ${token.name}")
                    }
                }
                val d = value.asDouble()
                if (!d.isFinite()) throw SimulationExprException("Invalid expression: ${token.name}")
                return d
            }
            NumericToken.MINUS -> return -parseDoubleFactor()
            NumericToken.LEFT_PAREN -> {
                val v = parseDoubleExpression()
                if (current() != NumericToken.RIGHT_PAREN) throw SimulationExprException("Invalid expression: ")
                index++
                return v
            }
            else -> throw SimulationExprException("Invalid expression: ")
        }
    }

    private fun current(): NumericToken? =
        if (index < tokens.size) tokens[index] else null
}

internal fun normalizeNumericExpression(expression: String): String =
    expression
        .replace("(int)", "", ignoreCase = true)
        .replace("(double)", "", ignoreCase = true)
        .trim()
