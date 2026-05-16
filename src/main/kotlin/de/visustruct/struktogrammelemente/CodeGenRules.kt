package de.visustruct.struktogrammelemente

import de.visustruct.view.CodeErzeuger
import java.util.HashSet
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.jvm.JvmStatic

/** Zielsprachen-Regeln aus der SwiftUI-Version des Codegenerators — Port von CodeGenRules.java */
object CodeGenRules {
    private val OUTPUT_PREFIX =
        Pattern.compile("(?i)^\\s*(?:output|print):\\s*(.+?)\\s*;?\\s*$")
    private val INPUT_TYPED = Pattern.compile(
        "(?i)^\\s*(integer|int|long|float|double|string|boolean|char|bool)\\s+([A-Za-z_$][A-Za-z0-9_$]*(?:\\s*\\[[^\\]]+\\])?)(?:\\s+\"([^\"]*)\")?\\s*$",
    )
    private val INPUT_NAME_QUOTED = Pattern.compile(
        "(?i)^\\s*([A-Za-z_$][A-Za-z0-9_$]*(?:\\s*\\[[^\\]]+\\])?)\\s+\"([^\"]*)\"\\s*$",
    )
    private val CONST_TYPED = Pattern.compile(
        "(?i)^\\s*const\\s+(integer|int|long|float|double|string|boolean|char|bool)\\s+([A-Za-z_$][A-Za-z0-9_$]*)(?:\\s*=\\s*(.*))?$",
    )
    private val JAVA_STYLE_ASSIGNMENT = Pattern.compile(
        "(?i)^\\s*(integer|int|long|float|double|string|boolean|char|bool)(?:\\s*\\[\\s*\\]\\s+([A-Za-z_$][A-Za-z0-9_$]*)|\\s+([A-Za-z_$][A-Za-z0-9_$]*)(\\s*\\[\\s*\\])?)\\s*=\\s*(.*)$",
    )
    private val JAVA_NEW_ARRAY = Pattern.compile(
        "(?i)^\\s*new\\s+(?:byte|short|int|long|float|double|boolean|char|string)\\s*\\[\\s*(.+?)\\s*\\]\\s*$",
    )
    private val JAVA_INT_CAST =
        Pattern.compile("(?i)^\\s*\\(\\s*int(?:eger)?\\s*\\)\\s*\\(?\\s*(.+?)\\s*\\)?\\s*$")
    private val JAVA_LENGTH = Pattern.compile("\\b([A-Za-z_$][A-Za-z0-9_$]*)\\.length(?!\\()")
    private val JAVA_ARRAY_DECL_1 = Pattern.compile(
        "\\b(?:final\\s+)?(?:byte|short|int|long|float|double|boolean|char|String)\\s*\\[\\s*\\]\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b",
    )
    private val JAVA_ARRAY_DECL_2 = Pattern.compile(
        "\\b(?:final\\s+)?(?:byte|short|int|long|float|double|boolean|char|String)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\[\\s*\\]",
    )
    /** Während der Java-Quelltext-Erzeugung … */
    private val JAVA_SCOPE_DEPTH = ThreadLocal<Int>()
    private val JAVA_DECLARED_NAMES = ThreadLocal<HashSet<String>>()

    private val JAVA_EMITTED_TYPED_ASSIGN =
        Pattern.compile(
            "(?i)^\\s*(?:final\\s+)?(int|long|float|double|boolean|byte|short|char|String)(?:\\s*\\[\\s*\\])?\\s+([A-Za-z_$][\\w]*)\\s*=",
        )
    private val JAVA_EMITTED_TYPED_ASSIGN_BRACKETS_AFTER =
        Pattern.compile(
            "(?i)^\\s*(?:final\\s+)?(int|long|float|double|boolean|byte|short|char|String)\\s+([A-Za-z_$][\\w]*)\\s*\\[\\s*\\]\\s*=",
        )
    /** Während der JS-Quelltext-Erzeugung … */
    private val JS_INTEGER_VARIABLES = ThreadLocal<HashSet<String>>()
    private val MATH_RANDOM_NO_PARENS = Pattern.compile("(?i)\\bMath\\.random\\b(?!\\s*\\()")
    private val JS_INT_ASSIGN_DIV = Pattern.compile(
        "^\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*([A-Za-z_$][A-Za-z0-9_$]*|\\d+)\\s*/\\s*([A-Za-z_$][A-Za-z0-9_$]*|\\d+)\\s*$",
    )
    private val JS_INT_DIV_EQ =
        Pattern.compile("^\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*/=\\s*([A-Za-z_$][A-Za-z0-9_$]*|\\d+)\\s*$")
    private val JS_INT_DECL_ASSIGN = Pattern.compile(
        "(?i)^\\s*(integer|int|long|short|byte)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*=",
    )
    /** Diagramm-Zeilen ohne „=”: z.&nbsp;B. `zahl / 2`, Collatz `zahl * 3 + 1` / `3 * zahl + 1`. */
    private val SHORTHAND_SELF_DIV = Pattern.compile(
        "^\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*/\\s*([A-Za-z_$][A-Za-z0-9_$]*|\\d+)\\s*$",
    )
    private val SHORTHAND_VAR_MUL_ADD = Pattern.compile(
        "^\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\*\\s*(\\d+)\\s*\\+\\s*(\\d+)\\s*$",
    )
    private val SHORTHAND_LIT_MUL_VAR_ADD = Pattern.compile(
        "^\\s*(\\d+)\\s*\\*\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\+\\s*(\\d+)\\s*$",
    )
    private val LINE_SPLIT_PATTERN = Pattern.compile("\\R")
    private val PYTHON_DOT_LENGTH_REPLACE =
        Pattern.compile("\\b([A-Za-z_$][A-Za-z0-9_$]*)\\.length(?:\\(\\))?\\b")

    @JvmStatic
    fun beginJavaScriptCodeGeneration(root: StruktogrammElementListe?) {
        @Suppress("UNCHECKED_CAST")
        JS_INTEGER_VARIABLES.set(inferIntegerVariablesForJavaScript(root) as HashSet<String>)
    }

    @JvmStatic
    fun endJavaScriptCodeGeneration() {
        JS_INTEGER_VARIABLES.remove()
    }

    /** Pro `quellcodeAllerUnterelementeGenerieren` … */
    @JvmStatic
    fun enterJavaGenerationScope() {
        val d = JAVA_SCOPE_DEPTH.get()
        val n = if (d == null) 0 else d
        if (n == 0) {
            JAVA_DECLARED_NAMES.set(HashSet())
        }
        JAVA_SCOPE_DEPTH.set(n + 1)
    }

    @JvmStatic
    fun leaveJavaGenerationScope() {
        val d = JAVA_SCOPE_DEPTH.get() ?: return
        val n = d - 1
        if (n <= 0) {
            JAVA_SCOPE_DEPTH.remove()
            JAVA_DECLARED_NAMES.remove()
        } else {
            JAVA_SCOPE_DEPTH.set(n)
        }
    }

    private fun currentJavaDeclaredNames(): Set<String> =
        JAVA_DECLARED_NAMES.get() ?: emptySet()

    private fun registerJavaNamesFromEmitted(block: String?) {
        val s = JAVA_DECLARED_NAMES.get()
        if (s == null || block.isNullOrEmpty()) return
        for (rawLine in LINE_SPLIT_PATTERN.split(block, -1)) {
            val ln = rawLine.trim()
            if (ln.isEmpty()) continue
            val m1 = JAVA_EMITTED_TYPED_ASSIGN.matcher(ln)
            if (m1.find()) {
                s.add(m1.group(2))
                continue
            }
            val m2 = JAVA_EMITTED_TYPED_ASSIGN_BRACKETS_AFTER.matcher(ln)
            if (m2.find()) {
                s.add(m2.group(2))
            }
        }
    }

    private fun javaVariableAlreadyDeclared(simpleName: String?): Boolean {
        if (simpleName.isNullOrEmpty()) return false
        return currentJavaDeclaredNames().contains(simpleName)
    }

    private fun currentJsIntegerVariables(): Set<String> =
        JS_INTEGER_VARIABLES.get() ?: emptySet()

    @JvmStatic
    fun indent(codeLine: String, spaces: Int): String =
        if (spaces <= 0) codeLine else " ".repeat(spaces) + codeLine

    @JvmStatic
    fun generateInstructionLine(raw: String?, target: Int, indent: Int): String {
        val line = raw?.trim().orEmpty()
        if (line.isEmpty()) return ""
        val translated = translateInstructionLine(line, target)
        if (target == CodeErzeuger.typJava) {
            registerJavaNamesFromEmitted(translated)
        }
        return indentMultiline(translated, indent)
    }

    private fun translateInstructionLine(line: String, target: Int): String {
        if (target == CodeErzeuger.typJava) {
            emitJavaInput(line)?.let { return it }
            emitOutput(line, target)?.let { return it }
        } else if (target == CodeErzeuger.typJavaScript) {
            emitJavaScriptInput(line)?.let { return it }
            emitOutput(line, target)?.let { return it }
        } else if (target == CodeErzeuger.typPython) {
            emitPythonInput(line)?.let { return it }
            emitOutput(line, target)?.let { return it }
        }
        emitConstTypedLine(line, target)?.let { return it }
        emitTranslatedJavaLikeAssignment(line, target)?.let { return it }
        emitBareLeadingStringAsPrint(line, target)?.let { return it }

        if (target == CodeErzeuger.typJavaScript) {
            javaScriptShorthandSelfAssignIfNeeded(line, currentJsIntegerVariables())
                ?.let { return formatCStyleStatementLine(it) }
            javaScriptIntegerDivisionIfNeeded(line, currentJsIntegerVariables())
                ?.let { return formatCStyleStatementLine(it) }
        } else if (target == CodeErzeuger.typJava) {
            javaShorthandSelfAssignIfNeeded(line)?.let { return formatCStyleStatementLine(it) }
        } else if (target == CodeErzeuger.typPython) {
            pythonShorthandSelfAssignIfNeeded(line)?.let { return it }
        }

        return if (isCStyleTarget(target)) {
            formatCStyleStatementLine(line)
        } else {
            pythonRhsFromJavaRhs(stripTrailingSemicolon(line))
        }
    }

    private fun emitOutput(raw: String, target: Int): String? {
        val expr = expressionAfterOutputPrefix(raw) ?: return null
        when (target) {
            CodeErzeuger.typJava -> return "System.out.println($expr);"
            CodeErzeuger.typJavaScript -> return "console.log($expr);"
            CodeErzeuger.typPython -> return "print($expr)"
        }
        return null
    }

    private fun expressionAfterOutputPrefix(raw: String): String? {
        val m = OUTPUT_PREFIX.matcher(raw)
        if (!m.matches()) return null
        val expr = stripTrailingSemicolon(m.group(1))
        return expr.takeUnless { it.isEmpty() }
    }

    private fun emitBareLeadingStringAsPrint(raw: String, target: Int): String? {
        val text = stripTrailingSemicolon(raw).trim()
        if (!text.startsWith("\"")) return null
        when (target) {
            CodeErzeuger.typJava -> return "System.out.println($text);"
            CodeErzeuger.typJavaScript -> return "console.log($text);"
            CodeErzeuger.typPython -> return "print($text)"
        }
        return null
    }

    private fun emitJavaInput(raw: String): String? {
        val p = parseInputLine(raw) ?: return null
        val sb = StringBuilder()
        sb.append("System.out.println(\"").append(escapeJava(promptText(p))).append("\");\n")
        sb.append(javaInputTarget(p)).append(" = ").append(scannerExpression(p.type)).append(";")
        return sb.toString()
    }

    private fun emitJavaScriptInput(raw: String): String? {
        val p = parseInputLine(raw) ?: return null
        val prompt = "prompt(\"" + escapeJavaScript(promptText(p)) + "\")"
        val fallback = when (p.type) {
            "long", "int" -> "0"
            "float", "double" -> "0"
            else -> ""
        }
        val promptExpr = "($prompt ?? \"$fallback\")"
        val value = when (p.type) {
            "long", "int" -> "parseInt($promptExpr, 10)"
            "float", "double" -> "Number($promptExpr)"
            else -> promptExpr
        }
        return jsInputTarget(p) + " = " + value + ";"
    }

    private fun emitPythonInput(raw: String): String? {
        val p = parseInputLine(raw) ?: return null
        val expr = "input(\"" + escapePython(promptText(p)) + "\")"
        val value = when (p.type) {
            "long", "int" -> "int($expr)"
            "float", "double" -> "float($expr)"
            else -> expr
        }
        return p.name + " = " + value
    }

    private fun parseInputLine(raw: String): InputLineParse? {
        val t = raw.trim()
        if (!t.regionMatches(0, "input:", 0, "input:".length, ignoreCase = true)) return null
        val rest = stripTrailingSemicolon(t.substring("input:".length).trim())
        if (rest.isEmpty()) return null

        INPUT_TYPED.matcher(rest).takeIf { it.matches() }?.let { typed ->
            val name = typed.group(2).replace(" ", "")
            val customPrompt = typed.group(3)
            return InputLineParse(normalizeType(typed.group(1)), name, emptyToNull(customPrompt))
        }

        INPUT_NAME_QUOTED.matcher(rest).takeIf { it.matches() }?.let { nq ->
            val name = nq.group(1).replace(" ", "")
            val inferredType = if (name.contains("[")) "String" else "int"
            return InputLineParse(inferredType, name, nq.group(2))
        }

        val parts = rest.split(Regex("\\s+"))
        if (parts.size >= 2) {
            return InputLineParse(normalizeType(parts[0]), parts[1], null)
        }
        return if (isTypeKeyword(parts[0])) {
            null
        } else {
            InputLineParse("int", parts[0], null)
        }
    }

    private fun promptText(p: InputLineParse): String {
        val cp = p.customPrompt
        return if (cp != null && !cp.isBlank()) cp else "${p.name}: "
    }

    private fun javaInputTarget(p: InputLineParse): String {
        if (p.isIndexedTarget()) return p.name
        var simple = p.name.replace(" ", "")
        val lb = simple.indexOf('[')
        if (lb >= 0) simple = simple.substring(0, lb)
        return if (javaVariableAlreadyDeclared(simple)) {
            p.name.replace(" ", "")
        } else {
            p.type + " " + p.name.replace(" ", "")
        }
    }

    private fun jsInputTarget(p: InputLineParse): String =
        if (p.isIndexedTarget()) p.name else "let " + p.name

    private fun scannerExpression(type: String): String = when (type) {
        "int" -> "scanner.nextInt()"
        "long" -> "scanner.nextLong()"
        "float" -> "scanner.nextFloat()"
        "double" -> "scanner.nextDouble()"
        "boolean" -> "scanner.nextBoolean()"
        "char" -> "scanner.next().charAt(0)"
        "String" -> "scanner.next()"
        else -> "scanner.next()"
    }

    private fun emitConstTypedLine(raw: String, target: Int): String? {
        val m = CONST_TYPED.matcher(stripTrailingSemicolon(raw))
        if (!m.matches()) return null
        val javaType = normalizeType(m.group(1))
        val name = m.group(2)
        val rhs = emptyToNull(m.group(3))
        when (target) {
            CodeErzeuger.typJava -> {
                val line =
                    if (rhs == null) {
                        "final $javaType $name"
                    } else {
                        "final $javaType $name = $rhs"
                    }
                return formatCStyleStatementLine(line)
            }

            CodeErzeuger.typJavaScript -> {
                return if (rhs == null) {
                    "let $name;  // from diagram: const $javaType $name"
                } else {
                    formatCStyleStatementLine("const $name = " + javaScriptRhsFromJavaRhs(rhs, false))
                }
            }

            CodeErzeuger.typPython -> {
                return if (rhs == null) {
                    "$name = None  # const $javaType"
                } else {
                    "$name = " + pythonRhsFromJavaRhs(rhs) + "  # const $javaType"
                }
            }

            else -> return null
        }
    }

    private fun emitTranslatedJavaLikeAssignment(raw: String, target: Int): String? {
        if (target != CodeErzeuger.typJavaScript && target != CodeErzeuger.typPython) return null
        val m = JAVA_STYLE_ASSIGNMENT.matcher(stripTrailingSemicolon(raw))
        if (!m.matches()) return null

        val g2empty = m.group(2).isNullOrBlank()
        val name = if (!g2empty) m.group(2) else m.group(3)
        val array = !g2empty || !(m.group(4).isNullOrBlank())
        val rhs = m.group(5).trim()

        return if (target == CodeErzeuger.typJavaScript) {
            formatCStyleStatementLine("let $name = " + javaScriptRhsFromJavaRhs(rhs, array))
        } else {
            "$name = " + pythonRhsFromJavaRhs(rhs, array)
        }
    }

    @JvmStatic
    fun cStyleForLoopHeader(lines: Array<String>?): String {
        val parts = cleanedParts(lines)
        return when (parts.size) {
            0 -> ";;"
            1 -> parts[0]
            else -> parts.joinToString("; ")
        }
    }

    @JvmStatic
    fun javaScriptForLoopHeader(lines: Array<String>?): String {
        val header = cStyleForLoopHeader(lines)
        val parts = ";".toRegex().split(header, -1).toMutableList()
        if (parts.isNotEmpty()) {
            val init = javaScriptForInitializerFromJavaTypedDeclaration(parts[0])
            if (init != null) {
                parts[0] = init
            }
        }
        return parts.joinToString(";")
    }

    @JvmStatic
    fun pythonForLineFromJavaLikeLoopFields(lines: Array<String>?): String? {
        val raw = cleanedParts(lines)
        if (raw.size <= 1) return null
        val init = if (raw.size > 0) raw[0] else ""
        val cond = if (raw.size > 1) raw[1] else ""
        val step = if (raw.size > 2) raw[2] else ""
        val initMatcher = Pattern.compile(
            "^\\s*(?:int|long)?\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*(-?\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE,
        ).matcher(init)
        if (!initMatcher.matches()) return null
        val vn = initMatcher.group(1)
        val start = initMatcher.group(2).toInt()
        val less =
            Pattern.compile("^\\s*" + Pattern.quote(vn) + "\\s*<\\s*(\\d+)\\s*$").matcher(cond)
        val lessEq =
            Pattern.compile("^\\s*" + Pattern.quote(vn) + "\\s*<=\\s*(\\d+)\\s*$").matcher(cond)
        val length = Pattern.compile(
            "^\\s*" + Pattern.quote(vn)
                + "\\s*<\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\.length(?:\\(\\))?\\s*$",
        ).matcher(cond)
        var stop: String? = null
        when {
            less.matches() -> stop = less.group(1)
            lessEq.matches() ->
                stop = (Integer.parseInt(lessEq.group(1)) + 1).toString()
            length.matches() -> stop = "len(${length.group(1)})"
        }
        if (stop == null || !isSimpleIncrement(step, vn)) {
            return null
        }
        return if (start == 0) {
            "for $vn in range($stop):"
        } else {
            "for $vn in range($start, $stop):"
        }
    }

    @JvmStatic
    fun caseLabelToken(name: String?, target: Int): String {
        val t = name?.trim().orEmpty()
        if (t.isEmpty()) {
            return if (target == CodeErzeuger.typPython) "\"\"" else "0"
        }
        return if (target == CodeErzeuger.typPython || target == CodeErzeuger.typJavaScript) {
            if (t.chars().allMatch(Character::isDigit)) {
                t
            } else {
                "\"" + t.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
            }
        } else {
            t
        }
    }

    @JvmStatic
    fun forcedCaseComment(name: String?, target: Int): String {
        val t = name?.trim().orEmpty()
        if (t.isEmpty()) return ""
        if (target == CodeErzeuger.typPython) {
            return "  # $t"
        }
        if (target == CodeErzeuger.typJavaScript) {
            return " /* $t */"
        }
        return "/* $t */"
    }

    @JvmStatic
    fun postProcessGeneratedCode(code: String, target: Int): String {
        when (target) {
            CodeErzeuger.typJavaScript -> return code.replace(".length()", ".length")

            CodeErzeuger.typPython -> {
                val mPy = PYTHON_DOT_LENGTH_REPLACE.matcher(code)
                val sbPy = StringBuffer()
                while (mPy.find()) {
                    mPy.appendReplacement(
                        sbPy,
                        Matcher.quoteReplacement("len(${mPy.group(1)})"),
                    )
                }
                mPy.appendTail(sbPy)
                return replaceJavaMathRandomWithPythonRandom(sbPy.toString())
            }

            CodeErzeuger.typJava -> return replaceIdentifierDotLengthWithParensInJava(code)
            else -> {}
        }
        return code
    }

    @JvmStatic
    fun pythonNeedsRandomImport(code: String): Boolean = code.contains("random.random()")

    @JvmStatic
    fun javaUsesScanner(code: String): Boolean = code.contains("scanner.")

    @JvmStatic
    fun stripTrailingSemicolon(s: String?): String {
        var x = s?.trim().orEmpty()
        while (x.endsWith(";")) {
            x = x.substring(0, x.length - 1).trim()
        }
        return x
    }

    @JvmStatic
    fun formatCStyleStatementLine(rawLine: String?): String {
        val line = rawLine?.trim().orEmpty()
        if (line.isEmpty() || line.endsWith(";") || line.endsWith("{") || line.equals("}") || line.startsWith("}")) {
            return line
        }
        val lower = line.lowercase()
        if (lower.startsWith("if ") || lower.startsWith("else") || lower.startsWith("for ") ||
            lower.startsWith("while ") || lower.startsWith("do ") || lower.startsWith("switch ") ||
            lower.startsWith("case ") || lower.startsWith("default") || lower.startsWith("//") ||
            lower.startsWith("/*") || lower.startsWith("*") || lower.startsWith("#")
        ) {
            return line
        }
        return "$line;"
    }

    private fun indentMultiline(code: String, spaces: Int): String {
        val lines = LINE_SPLIT_PATTERN.split(code, -1)
        val sb = StringBuilder()
        for (i in lines.indices) {
            if (i > 0) sb.append('\n')
            sb.append(indent(lines[i], spaces))
        }
        return sb.toString()
    }

    private fun javaScriptForInitializerFromJavaTypedDeclaration(raw: String): String? {
        val m = Pattern.compile(
            "(?i)^(\\s*)(?:integer|int|long|float|double|string|boolean|char|bool)\\s+([A-Za-z_$][A-Za-z0-9_$]*)(?:\\s*=\\s*(.*))?\\s*$",
        ).matcher(raw)
        if (!m.matches()) return null
        val rhs = emptyToNull(m.group(3))
        return if (rhs == null) {
            m.group(1) + "let " + m.group(2)
        } else {
            m.group(1) + "let " + m.group(2) + " = $rhs"
        }
    }

    private fun cleanedParts(lines: Array<String>?): Array<String> {
        if (lines == null) return emptyArray()
        return lines
            .asSequence()
            .map { stripTrailingSemicolon(it) }
            .filter { it.isNotEmpty() }
            .toList()
            .toTypedArray()
    }

    private fun isSimpleIncrement(step: String, vn: String): Boolean =
        step.matches("^\\s*${Regex.escape(vn)}\\s*\\+\\+\\s*$".toRegex()) ||
            step.matches("^\\s*${Regex.escape(vn)}\\s*\\+=\\s*1\\s*$".toRegex())

    /** Wie Swift `inferIntegerVariablesForJavaScript` — für Math.floor-Division in JS. */
    @JvmStatic
    fun inferIntegerVariablesForJavaScript(root: StruktogrammElementListe?): Set<String> {
        val out = HashSet<String>()
        if (root != null) {
            walkStruktogramForJsIntInference(root, out)
        }
        return out
    }

    private fun walkStruktogramForJsIntInference(list: StruktogrammElementListe, out: MutableSet<String>) {
        for (e in list) {
            for (line in e.gibText()) {
                addFromStatementLineForJsInt(line, out)
            }
            when (e) {
                is Schleife -> walkStruktogramForJsIntInference(e.gibListe(), out)

                is Fallauswahl ->
                    for (i in 0 until e.gibAnzahlListen()) {
                        walkStruktogramForJsIntInference(e.gibListe(i), out)
                    }

                else -> {}
            }
        }
    }

    private fun addFromStatementLineForJsInt(raw: String?, set: MutableSet<String>) {
        val t = raw?.trim().orEmpty()
        if (t.isEmpty()) return
        val lower = t.lowercase()
        if (lower.startsWith("input:")) {
            val p = parseInputLine(t)
            if (p != null && isDiagramIntegerType(p.type) && !p.isIndexedTarget()) {
                set.add(p.name)
            }
            return
        }
        val constM = CONST_TYPED.matcher(stripTrailingSemicolon(t))
        if (constM.matches() && isDiagramIntegerType(constM.group(1))) {
            set.add(constM.group(2))
            return
        }
        val decl = JS_INT_DECL_ASSIGN.matcher(t)
        if (decl.find()) {
            set.add(decl.group(2).trim())
        }
    }

    private fun isDiagramIntegerType(rawType: String?): Boolean {
        val x = rawType?.trim()?.lowercase().orEmpty()
        return x == "int" || x == "integer" || x == "long" || x == "short" || x == "byte"
    }

    /**
     * Wie Swift `javaScriptIntegerDivisionIfNeeded` — einfache `x = x / y` bzw. `x /= y` für Ganzzahl-Variablen.
     */
    internal fun javaScriptIntegerDivisionIfNeeded(raw: String, intVariables: Set<String>?): String? {
        if (intVariables == null || intVariables.isEmpty()) return null
        val t = stripTrailingSemicolon(raw).trim()
        if (t.isEmpty()) return null
        JS_INT_ASSIGN_DIV.matcher(t).takeIf { it.matches() }?.let { assign ->
            val lhs = assign.group(1)
            val a = assign.group(2)
            val b = assign.group(3)
            return if (intVariables.contains(lhs) && lhs == a) {
                "$lhs = Math.floor($a / $b)"
            } else {
                null
            }
        }
        val divEq = JS_INT_DIV_EQ.matcher(t)
        if (divEq.matches()) {
            val lhs = divEq.group(1)
            val b = divEq.group(2)
            if (intVariables.contains(lhs)) {
                return "$lhs = Math.floor($lhs / $b)"
            }
        }
        return null
    }

    private fun javaScriptShorthandSelfAssignIfNeeded(
        raw: String,
        intVariables: Set<String>?,
    ): String? {
        val t = stripTrailingSemicolon(raw).trim()
        if (t.isEmpty()) return null
        SHORTHAND_SELF_DIV.matcher(t).takeIf { it.matches() }?.let { mDiv ->
            val lhs = mDiv.group(1)
            val rhs = mDiv.group(2)
            return if (intVariables != null && intVariables.contains(lhs)) {
                "$lhs = Math.floor($lhs / $rhs)"
            } else {
                "$lhs = $lhs / $rhs"
            }
        }
        SHORTHAND_VAR_MUL_ADD.matcher(t).takeIf { it.matches() }?.let { mMul ->
            val v = mMul.group(1)
            return "${v} = $v * ${mMul.group(2)} + ${mMul.group(3)}"
        }
        SHORTHAND_LIT_MUL_VAR_ADD.matcher(t).takeIf { it.matches() }?.let { m3 ->
            val v = m3.group(2)
            return "${v} = ${m3.group(1)} * $v + ${m3.group(3)}"
        }
        return null
    }

    private fun javaShorthandSelfAssignIfNeeded(raw: String): String? {
        val t = stripTrailingSemicolon(raw).trim()
        if (t.isEmpty()) return null
        SHORTHAND_SELF_DIV.matcher(t).takeIf { it.matches() }?.let { mDiv ->
            val lhs = mDiv.group(1)
            return "$lhs = $lhs / ${mDiv.group(2)}"
        }
        SHORTHAND_VAR_MUL_ADD.matcher(t).takeIf { it.matches() }?.let { mMul ->
            val v = mMul.group(1)
            return "${v} = $v * ${mMul.group(2)} + ${mMul.group(3)}"
        }
        SHORTHAND_LIT_MUL_VAR_ADD.matcher(t).takeIf { it.matches() }?.let { m3 ->
            val v = m3.group(2)
            return "${v} = ${m3.group(1)} * $v + ${m3.group(3)}"
        }
        return null
    }

    private fun pythonShorthandSelfAssignIfNeeded(raw: String): String? {
        val t = stripTrailingSemicolon(raw).trim()
        if (t.isEmpty()) return null
        SHORTHAND_SELF_DIV.matcher(t).takeIf { it.matches() }?.let { mDiv ->
            val lhs = mDiv.group(1)
            return "$lhs = $lhs // ${mDiv.group(2)}"
        }
        SHORTHAND_VAR_MUL_ADD.matcher(t).takeIf { it.matches() }?.let { mMul ->
            val v = mMul.group(1)
            return "${v} = $v * ${mMul.group(2)} + ${mMul.group(3)}"
        }
        SHORTHAND_LIT_MUL_VAR_ADD.matcher(t).takeIf { it.matches() }?.let { m3 ->
            val v = m3.group(2)
            return "${v} = ${m3.group(1)} * $v + ${m3.group(3)}"
        }
        return null
    }

    private fun normalizeJavaScriptMathRandomCall(s: String): String {
        val m = MATH_RANDOM_NO_PARENS.matcher(s)
        val out = StringBuffer()
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement("Math.random()"))
        }
        m.appendTail(out)
        return out.toString()
    }

    private fun replaceIdentifierDotLengthWithParensInJava(code: String): String {
        val arrayNames = javaArrayDeclarationNames(code)
        val m = JAVA_LENGTH.matcher(code)
        val out = StringBuffer()
        while (m.find()) {
            val id = m.group(1)
            val replacement =
                if (arrayNames.contains(id.lowercase(java.util.Locale.ENGLISH))) {
                    m.group(0)
                } else {
                    id + ".length()"
                }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement))
        }
        m.appendTail(out)
        return out.toString()
    }

    private fun javaArrayDeclarationNames(code: String): Set<String> {
        val names = HashSet<String>()
        collectArrayNames(JAVA_ARRAY_DECL_1, code, names)
        collectArrayNames(JAVA_ARRAY_DECL_2, code, names)
        return names
    }

    private fun collectArrayNames(pattern: Pattern, code: String, names: MutableSet<String>) {
        val m = pattern.matcher(code)
        while (m.find()) {
            names.add(m.group(1).lowercase(java.util.Locale.ENGLISH))
        }
    }

    private fun javaScriptRhsFromJavaRhs(rhs: String, array: Boolean): String {
        var t = rhs.trim()
        if (array) {
            val newArray = JAVA_NEW_ARRAY.matcher(t)
            if (newArray.matches()) {
                return "new Array(" + newArray.group(1).trim() + ")"
            }
            return arrayLiteralFromJavaInitializer(t)
        }
        val cast = JAVA_INT_CAST.matcher(t)
        return if (cast.matches()) {
            "Math.floor(" + javaScriptRhsFromJavaRhs(cast.group(1), false) + ")"
        } else {
            normalizeJavaScriptMathRandomCall(t)
        }
    }

    private fun pythonRhsFromJavaRhs(rhs: String, array: Boolean): String {
        var t = rhs.trim()
        if (array) {
            val newArray = JAVA_NEW_ARRAY.matcher(t)
            if (newArray.matches()) {
                return "[None] * " + newArray.group(1).trim()
            }
            return arrayLiteralFromJavaInitializer(t)
        }
        return pythonRhsFromJavaRhs(rhs)
    }

    private fun pythonRhsFromJavaRhs(rhs: String): String {
        var t = rhs.trim()
        if (t == "null") {
            return "None"
        }
        val cast = JAVA_INT_CAST.matcher(t)
        if (cast.matches()) {
            return "int(" + pythonRhsFromJavaRhs(cast.group(1)) + ")"
        }
        return replaceJavaMathRandomWithPythonRandom(t)
    }

    private fun replaceJavaMathRandomWithPythonRandom(s: String): String =
        s.replace(Regex("(?i)\\bMath\\.random\\s*\\(\\s*\\)")) { "random.random()" }

    private fun arrayLiteralFromJavaInitializer(rhs: String): String {
        val t = rhs.trim()
        if (t.startsWith("{") && t.endsWith("}")) {
            return "[" + t.substring(1, t.length - 1) + "]"
        }
        return t
    }

    private fun isCStyleTarget(target: Int): Boolean =
        target == CodeErzeuger.typJava || target == CodeErzeuger.typJavaScript

    private fun normalizeType(raw: String): String {
        val t = raw.trim().lowercase(java.util.Locale.ENGLISH)
        return when {
            t == "integer" || t == "int" -> "int"
            t == "string" -> "String"
            t == "bool" -> "boolean"
            else -> t
        }
    }

    private fun isTypeKeyword(raw: String): Boolean {
        val t = raw.lowercase()
        return t in setOf("int", "integer", "long", "float", "double", "string", "boolean", "char", "bool", "void")
    }

    private fun emptyToNull(raw: String?): String? =
        if (raw == null || raw.trim().isEmpty()) null else raw.trim()

    private fun escapeJava(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")

    private fun escapeJavaScript(s: String): String = escapeJava(s)

    private fun escapePython(s: String): String = escapeJava(s)

    private data class InputLineParse(val type: String, val name: String, val customPrompt: String?) {
        fun isIndexedTarget(): Boolean = name.contains("[") && name.contains("]")
    }
}
