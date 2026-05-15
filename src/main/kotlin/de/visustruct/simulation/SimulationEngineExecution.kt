package de.visustruct.simulation

import de.visustruct.simulation.model.SimulationCase
import kotlin.math.abs
import kotlin.math.round

internal sealed class SwitchSel {
    data class I(val v: Int) : SwitchSel()
    data class T(val s: String) : SwitchSel()
}

private data class AssignmentTarget(val variableName: String, val valueType: SimulationValueType?)

private data class ArrayDecl(val name: String, val typeName: String, val count: Int, val value: SimulationValue)

private data class ArrayAccess(val arrayName: String, val indexExpression: String)

/** Wie {@code CodeGenRules} / Editor: {@code output: …} oder {@code print: …}. */
private val OUTPUT_PREFIX_PATTERN = Regex("(?i)^\\s*(?:output|print):\\s*(.*)$")

internal fun SimulationEngine.executeStatement(rawText: String) {
    val text = rawText.trim()
    if (text.lowercase().startsWith("input:")) {
        state.inputRequest = parseInputRequest(text)
        state.inputError = null
        return
    }
    normalizeOutputLikeStatement(text)?.let { normalized ->
        executeStatement(normalized)
        return
    }
    val outputMatch = OUTPUT_PREFIX_PATTERN.matchEntire(text)
    if (outputMatch != null) {
        val expression = stripTrailingSemicolon(outputMatch.groupValues[1].trim())
        if (expression.isEmpty()) {
            throw SimulationExprException("Output expression is empty.")
        }
        val rendered = renderOutputExpression(expression)
        val output = evaluateOutputExpression(expression)
        state.outputLines.add(output)
        state.lastTrace = "$rendered -> $output"
        return
    }
    if (text.endsWith("++") || text.endsWith("--") || text.contains("+=") || text.contains("-=")) {
        executeIncrementStatement(text)
        return
    }
    val eq = text.indexOf('=')
    if (eq < 0) {
        executeBareExpressionStatement(text)
        return
    }
    val left = text.take(eq).trim()
    val expression = text.drop(eq + 1)
    val arrayDecl = parseArrayDeclaration(left, expression)
    if (arrayDecl != null) {
        state.variables[arrayDecl.name] = arrayDecl.value
        state.lastTrace = "${arrayDecl.name} = new ${arrayDecl.typeName}[${arrayDecl.count}]"
        return
    }
    val assignment = parseAssignmentTarget(left)
    val renderedExpression = try {
        renderNumericExpression(expression)
    } catch (_: Exception) {
        expression.trim()
    }
    val targetType =
        when {
            assignment.valueType != null -> assignment.valueType
            state.variables.containsKey(assignment.variableName) ->
                inferTargetValueType(assignment.variableName)
            else -> inferTypeForNewDeclaration(expression)
        }
    val value = evaluateExpression(expression, targetType)
    assignValue(value, assignment.variableName)
    state.lastTrace = "${assignment.variableName} = $renderedExpression -> ${value.display}"
}

internal fun SimulationEngine.executeIncrementStatement(text: String) {
    when {
        text.endsWith("++") -> {
            val name = text.dropLast(2).trim()
            if (!isIdentifier(name)) throw SimulationExprException("Unsupported statement: $text")
            val old = state.variables[name] ?: throw SimulationExprException("Unsupported statement: $text")
            when (old) {
                is SimulationValue.VInt -> {
                    state.variables[name] = SimulationValue.VInt(old.value + 1)
                    state.lastTrace = "$name = ${old.value} + 1 -> ${old.value + 1}"
                }
                is SimulationValue.VDouble -> {
                    val r = old.value + 1
                    state.variables[name] = SimulationValue.VDouble(r)
                    state.lastTrace = "$name = ${old.display} + 1 -> ${SimulationValue.formatDouble(r)}"
                }
                else -> throw SimulationExprException("Unsupported statement: $text")
            }
        }
        text.endsWith("--") -> {
            val name = text.dropLast(2).trim()
            if (!isIdentifier(name)) throw SimulationExprException("Unsupported statement: $text")
            val old = state.variables[name] ?: throw SimulationExprException("Unsupported statement: $text")
            when (old) {
                is SimulationValue.VInt -> {
                    state.variables[name] = SimulationValue.VInt(old.value - 1)
                    state.lastTrace = "$name = ${old.value} - 1 -> ${old.value - 1}"
                }
                is SimulationValue.VDouble -> {
                    val r = old.value - 1
                    state.variables[name] = SimulationValue.VDouble(r)
                    state.lastTrace = "$name = ${old.display} - 1 -> ${SimulationValue.formatDouble(r)}"
                }
                else -> throw SimulationExprException("Unsupported statement: $text")
            }
        }
        text.contains("+=") -> {
            val idx = text.indexOf("+=")
            val name = text.take(idx).trim()
            val rhs = text.drop(idx + 2)
            if (!isIdentifier(name)) throw SimulationExprException("Unsupported statement: $text")
            val old = state.variables[name] ?: throw SimulationExprException("Unsupported statement: $text")
            when (old) {
                is SimulationValue.VInt -> {
                    val inc = evaluateIntExpression(rhs)
                    state.variables[name] = SimulationValue.VInt(old.value + inc)
                    state.lastTrace = "$name = ${old.value} + $inc -> ${old.value + inc}"
                }
                is SimulationValue.VDouble -> {
                    val inc = evaluateDoubleExpression(rhs)
                    val r = old.value + inc
                    state.variables[name] = SimulationValue.VDouble(r)
                    state.lastTrace = "$name = ${old.display} + ${SimulationValue.formatDouble(inc)} -> ${SimulationValue.formatDouble(r)}"
                }
                else -> throw SimulationExprException("Unsupported statement: $text")
            }
        }
        text.contains("-=") -> {
            val idx = text.indexOf("-=")
            val name = text.take(idx).trim()
            val rhs = text.drop(idx + 2)
            if (!isIdentifier(name)) throw SimulationExprException("Unsupported statement: $text")
            val old = state.variables[name] ?: throw SimulationExprException("Unsupported statement: $text")
            when (old) {
                is SimulationValue.VInt -> {
                    val dec = evaluateIntExpression(rhs)
                    state.variables[name] = SimulationValue.VInt(old.value - dec)
                    state.lastTrace = "$name = ${old.value} - $dec -> ${old.value - dec}"
                }
                is SimulationValue.VDouble -> {
                    val dec = evaluateDoubleExpression(rhs)
                    val r = old.value - dec
                    state.variables[name] = SimulationValue.VDouble(r)
                    state.lastTrace = "$name = ${old.display} - ${SimulationValue.formatDouble(dec)} -> ${SimulationValue.formatDouble(r)}"
                }
                else -> throw SimulationExprException("Unsupported statement: $text")
            }
        }
        else -> executeStatement(text)
    }
}

private fun SimulationEngine.executeBareExpressionStatement(text: String) {
    val v = evaluateNumericExpression(text)
    state.lastTrace = "$text -> ${v.display} (no assignment)"
}

/** `output:`, `print:`, `System.out.println(…)`, `console.log(…)`, Python `print(…)`. */
private fun normalizeOutputLikeStatement(text: String): String? {
    extractSystemOutPrintlnArgument(text)?.let { return "output: $it" }
    extractConsoleLogArgument(text)?.let { return "print: $it" }
    extractPythonPrintArgument(text)?.let { return "output: $it" }
    return null
}

private fun stripTrailingSemicolon(text: String): String {
    var result = text.trim()
    while (result.endsWith(';')) {
        result = result.dropLast(1).trim()
    }
    return result
}

/** Einfaches `console.log(a + b)` → wie `print:`. */
private fun extractConsoleLogArgument(text: String): String? {
    val m =
        Regex("^console\\.log\\s*\\((.+)\\)\\s*;?\\s*$", RegexOption.IGNORE_CASE).find(text.trim())
            ?: return null
    return m.groupValues[1].trim()
}

/** Wie vom Java-Codegenerator erzeugte Ausgabezeilen im Diagramm. */
private fun extractSystemOutPrintlnArgument(text: String): String? {
    val m =
        Regex("^System\\.out\\.println\\s*\\((.+)\\)\\s*;?\\s*$", RegexOption.IGNORE_CASE)
            .find(text.trim()) ?: return null
    return m.groupValues[1].trim()
}

/** Python-ähnliche Ausgabe im Block. */
private fun extractPythonPrintArgument(text: String): String? {
    val m =
        Regex("^print\\s*\\((.+)\\)\\s*;?\\s*$", RegexOption.IGNORE_CASE).find(text.trim())
            ?: return null
    return m.groupValues[1].trim()
}

private fun inferTypeForNewDeclaration(expression: String): SimulationValueType {
    val trimmed = expression.trim()
    unquotedString(trimmed)?.let { return SimulationValueType.STRING }
    if (trimmed.toIntOrNull() != null) return SimulationValueType.INT
    if (trimmed.toDoubleOrNull() != null) return SimulationValueType.DOUBLE
    return SimulationValueType.INT
}

private fun SimulationEngine.parseAssignmentTarget(text: String): AssignmentTarget {
    val parts = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val name: String
    val vt: SimulationValueType?
    when {
        parts.size == 1 -> {
            name = parts[0]
            vt = null
        }
        parts.size == 2 && parts[0].lowercase() == "let" -> {
            name = parts[1]
            vt = null
        }
        parts.size == 2 && parts[0].lowercase() == "var" -> {
            name = parts[1]
            vt = null
        }
        parts.size == 2 -> {
            val t = valueTypeKeyword(parts[0])
            if (t != null) {
                name = parts[1]
                vt = t
            } else throw SimulationExprException("Unsupported statement: $text")
        }
        parts.size == 3 && parts[0].lowercase() == "const" -> {
            val t = valueTypeKeyword(parts[1]) ?: throw SimulationExprException("Unsupported statement: $text")
            name = parts[2]
            vt = t
        }
        else -> throw SimulationExprException("Unsupported statement: $text")
    }
    if (!isAssignableTarget(name)) throw SimulationExprException("Unsupported statement: $text")
    return AssignmentTarget(name, vt)
}

private fun SimulationEngine.parseInputRequest(text: String): SimulationInputRequest {
    val after = text.drop("input:".length).trim()
    val parts = after.split(Regex("\\s+"), limit = 4).filter { it.isNotEmpty() }.toMutableList()
    val target: String
    val valueType: SimulationValueType
    val promptRaw: String
    if (parts.size >= 2 && valueTypeKeyword(parts[0]) != null) {
        val t = valueTypeKeyword(parts[0])!!
        target = parts[1]
        valueType = t
        promptRaw = parts.drop(2).joinToString(" ").trim()
    } else if (parts.isNotEmpty()) {
        target = parts[0]
        valueType = inferTargetValueType(target)
        promptRaw = parts.drop(1).joinToString(" ").trim()
    } else throw SimulationExprException("Unsupported statement: $text")
    if (!isAssignableTarget(target)) throw SimulationExprException("Unsupported statement: $text")
    val prompt = unquotedString(promptRaw) ?: promptRaw
    return SimulationInputRequest(
        variableName = target,
        valueType = valueType,
        prompt = if (prompt.isEmpty()) "$target:" else prompt,
    )
}

private fun SimulationEngine.parseArrayDeclaration(left: String, expression: String): ArrayDecl? {
    val leftParts = left.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (leftParts.size != 2 || !leftParts[0].endsWith("[]")) return null
    val typeName = leftParts[0].dropLast(2)
    val valueType = valueTypeKeyword(typeName) ?: throw SimulationExprException("Unsupported statement: $left")
    if (!isIdentifier(leftParts[1])) throw SimulationExprException("Unsupported statement: $left")
    val trimmedExpr = expression.trim()
    if (trimmedExpr.startsWith("{") && trimmedExpr.endsWith("}")) {
        val inner = trimmedExpr.substring(1, trimmedExpr.length - 1)
        val rawItems = inner.split(',').map { it.trim() }
        val value: SimulationValue = when (valueType) {
            SimulationValueType.INT ->
                SimulationValue.VIntArray(rawItems.map { evaluateIntExpression(it) }.toMutableList())
            SimulationValueType.DOUBLE ->
                SimulationValue.VDoubleArray(rawItems.map { evaluateDoubleExpression(it) }.toMutableList())
            SimulationValueType.STRING ->
                SimulationValue.VStringArray(rawItems.map { item ->
                    unquotedString(item) ?: evaluateOutputExpression(item)
                }.toMutableList())
        }
        return ArrayDecl(leftParts[1], typeName, rawItems.size, value)
    }
    val prefix = "new $typeName["
    if (!trimmedExpr.startsWith(prefix, ignoreCase = false) || !trimmedExpr.endsWith("]")) {
        throw SimulationExprException("Unsupported statement: $expression")
    }
    val sizeExpr = trimmedExpr.substring(prefix.length, trimmedExpr.length - 1)
    val count = evaluateIntExpression(sizeExpr)
    if (count < 0) throw SimulationExprException("Invalid expression: $sizeExpr")
    val value: SimulationValue = when (valueType) {
        SimulationValueType.INT -> SimulationValue.VIntArray(MutableList(count) { 0 })
        SimulationValueType.DOUBLE -> SimulationValue.VDoubleArray(MutableList(count) { 0.0 })
        SimulationValueType.STRING -> SimulationValue.VStringArray(MutableList(count) { "" })
    }
    return ArrayDecl(leftParts[1], typeName, count, value)
}

private fun parseArrayElementAccess(text: String): ArrayAccess? {
    val trimmed = text.trim()
    if (!trimmed.endsWith(']')) return null
    val open = trimmed.indexOf('[')
    if (open < 0) return null
    val name = trimmed.take(open).trim()
    if (!isIdentifier(name)) return null
    val idxExpr = trimmed.substring(open + 1, trimmed.length - 1)
    return ArrayAccess(name, idxExpr)
}

private fun SimulationEngine.arrayIndex(expression: String): Int {
    val index = evaluateIntExpression(expression)
    if (index < 0) throw SimulationExprException("Invalid expression: $expression")
    return index
}

private fun SimulationEngine.evaluateReference(text: String): SimulationValue? {
    val trimmed = text.trim()
    state.variables[trimmed]?.let { return it }
    val access = parseArrayElementAccess(trimmed) ?: return null
    val index = arrayIndex(access.indexExpression)
    return valueInArray(access.arrayName, index)
}

private fun SimulationEngine.valueInArray(name: String, index: Int): SimulationValue {
    val value = state.variables[name] ?: throw SimulationExprException("Unknown variable: $name")
    return when (value) {
        is SimulationValue.VIntArray -> {
            if (index !in value.values.indices) throw SimulationExprException("Invalid expression: $name[$index]")
            SimulationValue.VInt(value.values[index])
        }
        is SimulationValue.VDoubleArray -> {
            if (index !in value.values.indices) throw SimulationExprException("Invalid expression: $name[$index]")
            SimulationValue.VDouble(value.values[index])
        }
        is SimulationValue.VStringArray -> {
            if (index !in value.values.indices) throw SimulationExprException("Invalid expression: $name[$index]")
            SimulationValue.VString(value.values[index])
        }
        else -> throw SimulationExprException("Invalid expression: $name")
    }
}

internal fun SimulationEngine.assignValue(value: SimulationValue, target: String) {
    val trimmed = target.trim()
    val access = parseArrayElementAccess(trimmed)
    if (access != null) {
        val index = arrayIndex(access.indexExpression)
        val old = state.variables[access.arrayName] ?: throw SimulationExprException("Unknown variable: ${access.arrayName}")
        when (old) {
            is SimulationValue.VIntArray -> {
                if (index !in old.values.indices) throw SimulationExprException("Invalid expression: $trimmed")
                val v = value as? SimulationValue.VInt ?: throw SimulationExprException("Invalid expression: $trimmed")
                old.values[index] = v.value
            }
            is SimulationValue.VDoubleArray -> {
                if (index !in old.values.indices) throw SimulationExprException("Invalid expression: $trimmed")
                when (value) {
                    is SimulationValue.VDouble -> old.values[index] = value.value
                    is SimulationValue.VInt -> old.values[index] = value.value.toDouble()
                    else -> throw SimulationExprException("Invalid expression: $trimmed")
                }
            }
            is SimulationValue.VStringArray -> {
                if (index !in old.values.indices) throw SimulationExprException("Invalid expression: $trimmed")
                val v = value as? SimulationValue.VString ?: throw SimulationExprException("Invalid expression: $trimmed")
                old.values[index] = v.value
            }
            else -> throw SimulationExprException("Invalid expression: $trimmed")
        }
        return
    }
    state.variables[trimmed] = value
}

private fun SimulationEngine.evaluateOutputExpression(expression: String): String =
    splitByPlusOutsideQuotes(expression).joinToString("") { part ->
        val t = part.trim()
        unquotedString(t) ?: evaluateReference(t)?.display ?: evaluateNumericExpression(t).display
    }

private fun SimulationEngine.renderOutputExpression(expression: String): String =
    splitByPlusOutsideQuotes(expression).joinToString(" + ") { part ->
        val t = part.trim()
        when {
            unquotedString(t) != null -> t
            evaluateReference(t) != null -> evaluateReference(t)!!.display
            else -> try {
                renderNumericExpression(t)
            } catch (_: Exception) {
                t
            }
        }
    }

private fun SimulationEngine.evaluateExpression(expression: String, valueType: SimulationValueType): SimulationValue {
    return when (valueType) {
        SimulationValueType.INT -> SimulationValue.VInt(evaluateIntExpression(expression))
        SimulationValueType.DOUBLE -> SimulationValue.VDouble(evaluateDoubleExpression(expression))
        SimulationValueType.STRING -> {
            val trimmed = expression.trim()
            unquotedString(trimmed)?.let { return SimulationValue.VString(it) }
            evaluateReference(trimmed)?.let { return SimulationValue.VString(it.display) }
            SimulationValue.VString(evaluateOutputExpression(expression))
        }
    }
}

private fun SimulationEngine.evaluateNumericExpression(expression: String): SimulationValue {
    val v = evaluateDoubleExpression(expression)
    return if (v.isFinite() && abs(v - round(v)) < 1e-12) SimulationValue.VInt(v.toInt()) else SimulationValue.VDouble(v)
}

private fun SimulationEngine.evaluateIntExpression(expression: String): Int {
    var p = NumericExpressionParser(
        NumericExpressionTokenizer.tokenize(normalizeNumericExpression(expression)),
        state.variables,
    )
    return p.parseInt()
}

private fun SimulationEngine.evaluateDoubleExpression(expression: String): Double {
    var p = NumericExpressionParser(
        NumericExpressionTokenizer.tokenize(normalizeNumericExpression(expression)),
        state.variables,
    )
    return p.parseDouble()
}

internal fun SimulationEngine.evaluateBoolExpression(expression: String): Boolean {
    val trimmed = expression.trim()
    if (trimmed == "true") return true
    if (trimmed == "false") return false
    val orParts = splitBoolExpression(trimmed, "||")
    if (orParts.size > 1) {
        return orParts.any { evaluateBoolExpression(it) }
    }
    val andParts = splitBoolExpression(trimmed, "&&")
    if (andParts.size > 1) {
        return andParts.all { evaluateBoolExpression(it) }
    }
    val operators = listOf(">=", "<=", "==", "!=", ">", "<")
    for (op in operators) {
        val idx = trimmed.indexOf(op)
        if (idx >= 0) {
            val lhs = trimmed.take(idx)
            val rhs = trimmed.drop(idx + op.length)
            val leftValue = evaluateDoubleExpression(lhs)
            val rightValue = evaluateDoubleExpression(rhs)
            return when (op) {
                ">=" -> leftValue >= rightValue
                "<=" -> leftValue <= rightValue
                "==" -> leftValue == rightValue
                "!=" -> leftValue != rightValue
                ">" -> leftValue > rightValue
                "<" -> leftValue < rightValue
                else -> false
            }
        }
    }
    return evaluateDoubleExpression(trimmed) != 0.0
}

internal fun SimulationEngine.evaluateSwitchSelector(selector: String): SwitchSel {
    val trimmed = selector.trim()
    unquotedString(trimmed)?.let { return SwitchSel.T(it) }
    state.variables[trimmed]?.let { v ->
        return when (v) {
            is SimulationValue.VInt -> SwitchSel.I(v.value)
            is SimulationValue.VDouble -> SwitchSel.I(v.value.toInt())
            is SimulationValue.VString -> SwitchSel.T(v.value)
            else -> throw SimulationExprException("Invalid expression: $trimmed")
        }
    }
    trimmed.toIntOrNull()?.let { return SwitchSel.I(it) }
    return SwitchSel.I(evaluateIntExpression(trimmed))
}

internal fun matchingCaseIndex(selector: SwitchSel, cases: List<SimulationCase>): Int? {
    cases.forEachIndexed { index, branchCase ->
        val label = branchCase.name.trim()
        if (label.lowercase() == "default") return@forEachIndexed
        when (selector) {
            is SwitchSel.I -> if (label.toIntOrNull() == selector.v) return index
            is SwitchSel.T -> if (unquotedString(label) == selector.s || label == selector.s) return index
        }
    }
    return null
}

private fun splitBoolExpression(expression: String, separator: String): List<String> =
    expression.split(separator).map { it.trim() }.filter { it.isNotEmpty() }

private fun SimulationEngine.splitByPlusOutsideQuotes(expression: String): List<String> {
    val parts = mutableListOf<String>()
    var current = StringBuilder()
    var inString = false
    var escaped = false
    for (char in expression) {
        if (escaped) {
            current.append(char)
            escaped = false
            continue
        }
        if (char == '\\') {
            current.append(char)
            escaped = true
            continue
        }
        if (char == '"') {
            inString = !inString
            current.append(char)
            continue
        }
        if (char == '+' && !inString) {
            parts.add(current.toString())
            current = StringBuilder()
            continue
        }
        current.append(char)
    }
    if (inString) throw SimulationExprException("Invalid expression: $expression")
    parts.add(current.toString())
    return parts
}

private fun SimulationEngine.renderNumericExpression(expression: String): String =
    NumericExpressionTokenizer.tokenize(normalizeNumericExpression(expression)).joinToString(" ") { token ->
        when (token) {
            is NumericToken.NumberLiteral -> token.literal
            is NumericToken.Identifier -> {
                val v = state.variables[token.name] ?: throw SimulationExprException("Unknown variable: ${token.name}")
                v.display
            }
            NumericToken.PLUS -> "+"
            NumericToken.MINUS -> "-"
            NumericToken.MULTIPLY -> "*"
            NumericToken.DIVIDE -> "/"
            NumericToken.MODULO -> "%"
            NumericToken.DOT -> "."
            NumericToken.LEFT_PAREN -> "("
            NumericToken.RIGHT_PAREN -> ")"
            NumericToken.LEFT_BRACKET -> "["
            NumericToken.RIGHT_BRACKET -> "]"
        }
    }.replace("( ", "(").replace(" )", ")").replace(" . ", ".").replace("[ ", "[").replace(" ]", "]")

internal fun stripConditionKeyword(text: String): String {
    val trimmed = text.trim()
    for (keyword in listOf("if", "while", "until", "switch")) {
        if (!trimmed.startsWith(keyword)) continue
        val after = trimmed.drop(keyword.length)
        if (after.isNotEmpty() && !after.first().isWhitespace() && after.first() != '(') continue
        var stripped = after.trim()
        if (stripped.startsWith('(') && stripped.endsWith(')')) {
            stripped = stripped.substring(1, stripped.length - 1).trim()
        }
        return if (stripped.isEmpty()) "true" else stripped
    }
    return if (trimmed.isEmpty()) "true" else trimmed
}

private fun isIdentifier(text: String): Boolean {
    if (text.isEmpty()) return false
    val first = text.first()
    if (!first.isLetter() && first != '_') return false
    return text.all { it.isLetterOrDigit() || it == '_' }
}

private fun valueTypeKeyword(text: String): SimulationValueType? =
    when (text.lowercase()) {
        "int" -> SimulationValueType.INT
        "double" -> SimulationValueType.DOUBLE
        "string" -> SimulationValueType.STRING
        else -> null
    }

private fun valueTypeOf(value: SimulationValue): SimulationValueType =
    when (value) {
        is SimulationValue.VInt -> SimulationValueType.INT
        is SimulationValue.VDouble -> SimulationValueType.DOUBLE
        is SimulationValue.VString -> SimulationValueType.STRING
        is SimulationValue.VIntArray, is SimulationValue.VDoubleArray, is SimulationValue.VStringArray -> SimulationValueType.STRING
    }

private fun SimulationEngine.inferTargetValueType(target: String): SimulationValueType {
    val trimmed = target.trim()
    state.variables[trimmed]?.let { return valueTypeOf(it) }
    val access = parseArrayElementAccess(trimmed) ?: throw SimulationExprException("Unsupported statement: $target")
    val arr = state.variables[access.arrayName] ?: throw SimulationExprException("Unknown variable: ${access.arrayName}")
    return when (arr) {
        is SimulationValue.VIntArray -> SimulationValueType.INT
        is SimulationValue.VDoubleArray -> SimulationValueType.DOUBLE
        is SimulationValue.VStringArray -> SimulationValueType.STRING
        else -> throw SimulationExprException("Invalid expression: ${access.arrayName}")
    }
}

private fun isAssignableTarget(text: String): Boolean =
    isIdentifier(text) || parseArrayElementAccess(text) != null

private fun unquotedString(text: String): String? {
    if (text.length < 2 || text.first() != '"' || text.last() != '"') return null
    return text.substring(1, text.length - 1)
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
}
