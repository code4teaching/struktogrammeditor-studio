package de.visustruct.struktogrammelemente;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.visustruct.view.CodeErzeuger;

/** Zielsprachen-Regeln aus der SwiftUI-Version des Codegenerators. */
public final class CodeGenRules {
	private static final Pattern OUTPUT_PREFIX = Pattern.compile("(?i)^\\s*(?:output|print):\\s*(.+?)\\s*;?\\s*$");
	private static final Pattern INPUT_TYPED = Pattern.compile(
			"(?i)^\\s*(integer|int|long|float|double|string|boolean|char|bool)\\s+([A-Za-z_$][A-Za-z0-9_$]*(?:\\s*\\[[^\\]]+\\])?)(?:\\s+\"([^\"]*)\")?\\s*$");
	private static final Pattern INPUT_NAME_QUOTED = Pattern.compile(
			"(?i)^\\s*([A-Za-z_$][A-Za-z0-9_$]*(?:\\s*\\[[^\\]]+\\])?)\\s+\"([^\"]*)\"\\s*$");
	private static final Pattern CONST_TYPED = Pattern.compile(
			"(?i)^\\s*const\\s+(integer|int|long|float|double|string|boolean|char|bool)\\s+([A-Za-z_$][A-Za-z0-9_$]*)(?:\\s*=\\s*(.*))?$");
	private static final Pattern JAVA_STYLE_ASSIGNMENT = Pattern.compile(
			"(?i)^\\s*(integer|int|long|float|double|string|boolean|char|bool)(?:\\s*\\[\\s*\\]\\s+([A-Za-z_$][A-Za-z0-9_$]*)|\\s+([A-Za-z_$][A-Za-z0-9_$]*)(\\s*\\[\\s*\\])?)\\s*=\\s*(.*)$");
	private static final Pattern JAVA_NEW_ARRAY = Pattern.compile(
			"(?i)^\\s*new\\s+(?:byte|short|int|long|float|double|boolean|char|string)\\s*\\[\\s*(.+?)\\s*\\]\\s*$");
	private static final Pattern JAVA_INT_CAST = Pattern.compile("(?i)^\\s*\\(\\s*int(?:eger)?\\s*\\)\\s*\\(?\\s*(.+?)\\s*\\)?\\s*$");
	private static final Pattern JAVA_LENGTH = Pattern.compile("\\b([A-Za-z_$][A-Za-z0-9_$]*)\\.length(?!\\()");
	private static final Pattern JAVA_ARRAY_DECL_1 = Pattern.compile(
			"\\b(?:final\\s+)?(?:byte|short|int|long|float|double|boolean|char|String)\\s*\\[\\s*\\]\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b");
	private static final Pattern JAVA_ARRAY_DECL_2 = Pattern.compile(
			"\\b(?:final\\s+)?(?:byte|short|int|long|float|double|boolean|char|String)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\[\\s*\\]");
	/** Während der Java-Quelltext-Erzeugung: bereits deklarierte einfache Bezeichner (z. B. kein zweites {@code int} bei {@code input:}). */
	private static final ThreadLocal<Integer> JAVA_SCOPE_DEPTH = new ThreadLocal<>();
	private static final ThreadLocal<Set<String>> JAVA_DECLARED_NAMES = new ThreadLocal<>();
	private static final Pattern JAVA_EMITTED_TYPED_ASSIGN = Pattern.compile(
			"(?i)^\\s*(?:final\\s+)?(int|long|float|double|boolean|byte|short|char|String)(?:\\s*\\[\\s*\\])?\\s+([A-Za-z_$][\\w]*)\\s*=");
	private static final Pattern JAVA_EMITTED_TYPED_ASSIGN_BRACKETS_AFTER = Pattern.compile(
			"(?i)^\\s*(?:final\\s+)?(int|long|float|double|boolean|byte|short|char|String)\\s+([A-Za-z_$][\\w]*)\\s*\\[\\s*\\]\\s*=");
	/** Während der JS-Quelltext-Erzeugung: für {@link #javaScriptIntegerDivisionIfNeeded} (Swift-Parität). */
	private static final ThreadLocal<Set<String>> JS_INTEGER_VARIABLES = new ThreadLocal<>();

	private static final Pattern MATH_RANDOM_NO_PARENS = Pattern.compile("(?i)\\bMath\\.random\\b(?!\\s*\\()");
	private static final Pattern JS_INT_ASSIGN_DIV = Pattern
			.compile("^\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*([A-Za-z_$][A-Za-z0-9_$]*|\\d+)\\s*/\\s*([A-Za-z_$][A-Za-z0-9_$]*|\\d+)\\s*$");
	private static final Pattern JS_INT_DIV_EQ = Pattern.compile("^\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*/=\\s*([A-Za-z_$][A-Za-z0-9_$]*|\\d+)\\s*$");
	private static final Pattern JS_INT_DECL_ASSIGN = Pattern
			.compile("(?i)^\\s*(integer|int|long|short|byte)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*=");
	/** Struktogramm-Kurzschreibweisen ohne „=“: {@code zahl / 2}, Collatz {@code zahl * 3 + 1} / {@code 3 * zahl + 1}. */
	private static final Pattern SHORTHAND_SELF_DIV = Pattern
			.compile("^\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*/\\s*([A-Za-z_$][A-Za-z0-9_$]*|\\d+)\\s*$");
	private static final Pattern SHORTHAND_VAR_MUL_ADD = Pattern
			.compile("^\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\*\\s*(\\d+)\\s*\\+\\s*(\\d+)\\s*$");
	private static final Pattern SHORTHAND_LIT_MUL_VAR_ADD = Pattern
			.compile("^\\s*(\\d+)\\s*\\*\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\+\\s*(\\d+)\\s*$");

	private CodeGenRules() {
	}

	/**
	 * Vor {@code quellcodeAllerUnterelementeGenerieren(…, typJavaScript, …)} aufrufen, danach
	 * {@link #endJavaScriptCodeGeneration()} in {@code finally}.
	 */
	public static void beginJavaScriptCodeGeneration(StruktogrammElementListe root) {
		JS_INTEGER_VARIABLES.set(inferIntegerVariablesForJavaScript(root));
	}

	public static void endJavaScriptCodeGeneration() {
		JS_INTEGER_VARIABLES.remove();
	}

	/**
	 * Pro Wurzel-{@code quellcodeAllerUnterelementeGenerieren}-Lauf (verschachtelte Aufrufe per Zähler).
	 * Ermittelt u. a. doppelte {@code int}-Deklarationen bei {@code input:} nach vorherigem {@code int zahl = …}.
	 */
	public static void enterJavaGenerationScope() {
		Integer d = JAVA_SCOPE_DEPTH.get();
		int n = d == null ? 0 : d;
		if (n == 0) {
			JAVA_DECLARED_NAMES.set(new HashSet<>());
		}
		JAVA_SCOPE_DEPTH.set(n + 1);
	}

	public static void leaveJavaGenerationScope() {
		Integer d = JAVA_SCOPE_DEPTH.get();
		if (d == null) {
			return;
		}
		int n = d - 1;
		if (n <= 0) {
			JAVA_SCOPE_DEPTH.remove();
			JAVA_DECLARED_NAMES.remove();
		} else {
			JAVA_SCOPE_DEPTH.set(n);
		}
	}

	private static Set<String> currentJavaDeclaredNames() {
		Set<String> s = JAVA_DECLARED_NAMES.get();
		return s != null ? s : Set.of();
	}

	private static void registerJavaNamesFromEmitted(String block) {
		Set<String> s = JAVA_DECLARED_NAMES.get();
		if (s == null || block == null || block.isEmpty()) {
			return;
		}
		for (String rawLine : block.split("\\R", -1)) {
			String ln = rawLine.trim();
			if (ln.isEmpty()) {
				continue;
			}
			Matcher m1 = JAVA_EMITTED_TYPED_ASSIGN.matcher(ln);
			if (m1.find()) {
				s.add(m1.group(2));
				continue;
			}
			Matcher m2 = JAVA_EMITTED_TYPED_ASSIGN_BRACKETS_AFTER.matcher(ln);
			if (m2.find()) {
				s.add(m2.group(2));
			}
		}
	}

	private static boolean javaVariableAlreadyDeclared(String simpleName) {
		if (simpleName == null || simpleName.isEmpty()) {
			return false;
		}
		return currentJavaDeclaredNames().contains(simpleName);
	}

	private static Set<String> currentJsIntegerVariables() {
		Set<String> s = JS_INTEGER_VARIABLES.get();
		return s != null ? s : Set.of();
	}

	public static String indent(String codeLine, int spaces) {
		if (spaces <= 0) {
			return codeLine;
		}
		return " ".repeat(spaces) + codeLine;
	}

	public static String generateInstructionLine(String raw, int target, int indent) {
		String line = raw != null ? raw.trim() : "";
		if (line.isEmpty()) {
			return "";
		}
		String translated = translateInstructionLine(line, target);
		if (target == CodeErzeuger.typJava) {
			registerJavaNamesFromEmitted(translated);
		}
		return indentMultiline(translated, indent);
	}

	private static String translateInstructionLine(String line, int target) {
		if (target == CodeErzeuger.typJava) {
			String input = emitJavaInput(line);
			if (input != null) {
				return input;
			}
			String output = emitOutput(line, target);
			if (output != null) {
				return output;
			}
		} else if (target == CodeErzeuger.typJavaScript) {
			String input = emitJavaScriptInput(line);
			if (input != null) {
				return input;
			}
			String output = emitOutput(line, target);
			if (output != null) {
				return output;
			}
		} else if (target == CodeErzeuger.typPython) {
			String input = emitPythonInput(line);
			if (input != null) {
				return input;
			}
			String output = emitOutput(line, target);
			if (output != null) {
				return output;
			}
		}

		String constant = emitConstTypedLine(line, target);
		if (constant != null) {
			return constant;
		}
		String assignment = emitTranslatedJavaLikeAssignment(line, target);
		if (assignment != null) {
			return assignment;
		}
		String bareOutput = emitBareLeadingStringAsPrint(line, target);
		if (bareOutput != null) {
			return bareOutput;
		}
		if (target == CodeErzeuger.typJavaScript) {
			String shortAssign = javaScriptShorthandSelfAssignIfNeeded(line, currentJsIntegerVariables());
			if (shortAssign != null) {
				return formatCStyleStatementLine(shortAssign);
			}
			String div = javaScriptIntegerDivisionIfNeeded(line, currentJsIntegerVariables());
			if (div != null) {
				return formatCStyleStatementLine(div);
			}
		} else if (target == CodeErzeuger.typJava) {
			String shortAssign = javaShorthandSelfAssignIfNeeded(line);
			if (shortAssign != null) {
				return formatCStyleStatementLine(shortAssign);
			}
		} else if (target == CodeErzeuger.typPython) {
			String shortAssign = pythonShorthandSelfAssignIfNeeded(line);
			if (shortAssign != null) {
				return shortAssign;
			}
		}
		if (isCStyleTarget(target)) {
			return formatCStyleStatementLine(line);
		}
		return pythonRhsFromJavaRhs(stripTrailingSemicolon(line));
	}

	private static String emitOutput(String raw, int target) {
		String expr = expressionAfterOutputPrefix(raw);
		if (expr == null) {
			return null;
		}
		if (target == CodeErzeuger.typJava) {
			return "System.out.println(" + expr + ");";
		}
		if (target == CodeErzeuger.typJavaScript) {
			return "console.log(" + expr + ");";
		}
		if (target == CodeErzeuger.typPython) {
			return "print(" + expr + ")";
		}
		return null;
	}

	private static String expressionAfterOutputPrefix(String raw) {
		Matcher m = OUTPUT_PREFIX.matcher(raw);
		if (!m.matches()) {
			return null;
		}
		String expr = stripTrailingSemicolon(m.group(1));
		return expr.isEmpty() ? null : expr;
	}

	private static String emitBareLeadingStringAsPrint(String raw, int target) {
		String text = stripTrailingSemicolon(raw).trim();
		if (!text.startsWith("\"")) {
			return null;
		}
		if (target == CodeErzeuger.typJava) {
			return "System.out.println(" + text + ");";
		}
		if (target == CodeErzeuger.typJavaScript) {
			return "console.log(" + text + ");";
		}
		if (target == CodeErzeuger.typPython) {
			return "print(" + text + ")";
		}
		return null;
	}

	private static String emitJavaInput(String raw) {
		InputLineParse p = parseInputLine(raw);
		if (p == null) {
			return null;
		}
		StringBuilder out = new StringBuilder();
		out.append("System.out.println(\"").append(escapeJava(promptText(p))).append("\");\n");
		out.append(javaInputTarget(p)).append(" = ").append(scannerExpression(p.type)).append(";");
		return out.toString();
	}

	private static String emitJavaScriptInput(String raw) {
		InputLineParse p = parseInputLine(raw);
		if (p == null) {
			return null;
		}
		String prompt = "prompt(\"" + escapeJavaScript(promptText(p)) + "\")";
		String fallback = switch (p.type) {
		case "int", "long" -> "0";
		case "float", "double" -> "0";
		default -> "";
		};
		String promptExpr = "(" + prompt + " ?? \"" + fallback + "\")";
		String value = switch (p.type) {
		case "int", "long" -> "parseInt(" + promptExpr + ", 10)";
		case "float", "double" -> "Number(" + promptExpr + ")";
		default -> promptExpr;
		};
		return jsInputTarget(p) + " = " + value + ";";
	}

	private static String emitPythonInput(String raw) {
		InputLineParse p = parseInputLine(raw);
		if (p == null) {
			return null;
		}
		String expr = "input(\"" + escapePython(promptText(p)) + "\")";
		String value = switch (p.type) {
		case "int", "long" -> "int(" + expr + ")";
		case "float", "double" -> "float(" + expr + ")";
		default -> expr;
		};
		return p.name + " = " + value;
	}

	private static InputLineParse parseInputLine(String raw) {
		String t = raw.trim();
		if (!t.regionMatches(true, 0, "input:", 0, "input:".length())) {
			return null;
		}
		String rest = stripTrailingSemicolon(t.substring("input:".length()).trim());
		if (rest.isEmpty()) {
			return null;
		}
		Matcher typed = INPUT_TYPED.matcher(rest);
		if (typed.matches()) {
			String name = typed.group(2).replace(" ", "");
			String customPrompt = typed.group(3);
			return new InputLineParse(normalizeType(typed.group(1)), name, emptyToNull(customPrompt));
		}
		Matcher nameQuoted = INPUT_NAME_QUOTED.matcher(rest);
		if (nameQuoted.matches()) {
			String name = nameQuoted.group(1).replace(" ", "");
			String inferredType = name.contains("[") ? "String" : "int";
			return new InputLineParse(inferredType, name, nameQuoted.group(2));
		}
		String[] parts = rest.split("\\s+");
		if (parts.length >= 2) {
			return new InputLineParse(normalizeType(parts[0]), parts[1], null);
		}
		if (isTypeKeyword(parts[0])) {
			return null;
		}
		return new InputLineParse("int", parts[0], null);
	}

	private static String promptText(InputLineParse p) {
		if (p.customPrompt != null && !p.customPrompt.isBlank()) {
			return p.customPrompt;
		}
		return p.name + ": ";
	}

	private static String javaInputTarget(InputLineParse p) {
		if (p.isIndexedTarget()) {
			return p.name;
		}
		String simple = p.name.replace(" ", "");
		int lb = simple.indexOf('[');
		if (lb >= 0) {
			simple = simple.substring(0, lb);
		}
		if (javaVariableAlreadyDeclared(simple)) {
			return p.name.replace(" ", "");
		}
		return p.type + " " + p.name.replace(" ", "");
	}

	private static String jsInputTarget(InputLineParse p) {
		return p.isIndexedTarget() ? p.name : "let " + p.name;
	}

	private static String scannerExpression(String type) {
		return switch (type) {
		case "int" -> "scanner.nextInt()";
		case "long" -> "scanner.nextLong()";
		case "float" -> "scanner.nextFloat()";
		case "double" -> "scanner.nextDouble()";
		case "boolean" -> "scanner.nextBoolean()";
		case "char" -> "scanner.next().charAt(0)";
		case "String" -> "scanner.next()";
		default -> "scanner.next()";
		};
	}

	private static String emitConstTypedLine(String raw, int target) {
		Matcher m = CONST_TYPED.matcher(stripTrailingSemicolon(raw));
		if (!m.matches()) {
			return null;
		}
		String javaType = normalizeType(m.group(1));
		String name = m.group(2);
		String rhs = emptyToNull(m.group(3));
		if (target == CodeErzeuger.typJava) {
			return formatCStyleStatementLine(rhs == null ? "final " + javaType + " " + name
					: "final " + javaType + " " + name + " = " + rhs);
		}
		if (target == CodeErzeuger.typJavaScript) {
			return rhs == null ? "let " + name + ";  // from diagram: const " + javaType + " " + name
					: formatCStyleStatementLine("const " + name + " = " + javaScriptRhsFromJavaRhs(rhs, false));
		}
		if (target == CodeErzeuger.typPython) {
			return rhs == null ? name + " = None  # const " + javaType
					: name + " = " + pythonRhsFromJavaRhs(rhs) + "  # const " + javaType;
		}
		return null;
	}

	private static String emitTranslatedJavaLikeAssignment(String raw, int target) {
		if (target != CodeErzeuger.typJavaScript && target != CodeErzeuger.typPython) {
			return null;
		}
		Matcher m = JAVA_STYLE_ASSIGNMENT.matcher(stripTrailingSemicolon(raw));
		if (!m.matches()) {
			return null;
		}
		String name = m.group(2) != null && !m.group(2).isBlank() ? m.group(2) : m.group(3);
		boolean array = (m.group(2) != null && !m.group(2).isBlank()) || (m.group(4) != null && !m.group(4).isBlank());
		String rhs = m.group(5).trim();
		if (target == CodeErzeuger.typJavaScript) {
			return formatCStyleStatementLine("let " + name + " = " + javaScriptRhsFromJavaRhs(rhs, array));
		}
		return name + " = " + pythonRhsFromJavaRhs(rhs, array);
	}

	public static String cStyleForLoopHeader(String[] lines) {
		String[] parts = cleanedParts(lines);
		if (parts.length == 0) {
			return ";;";
		}
		if (parts.length == 1) {
			return parts[0];
		}
		return String.join("; ", parts);
	}

	public static String javaScriptForLoopHeader(String[] lines) {
		String header = cStyleForLoopHeader(lines);
		String[] parts = header.split(";", -1);
		if (parts.length > 0) {
			String init = javaScriptForInitializerFromJavaTypedDeclaration(parts[0]);
			if (init != null) {
				parts[0] = init;
			}
		}
		return String.join(";", parts);
	}

	public static String pythonForLineFromJavaLikeLoopFields(String[] lines) {
		String[] raw = cleanedParts(lines);
		if (raw.length <= 1) {
			return null;
		}
		String init = raw.length > 0 ? raw[0] : "";
		String cond = raw.length > 1 ? raw[1] : "";
		String step = raw.length > 2 ? raw[2] : "";
		Matcher initMatcher = Pattern.compile("^\\s*(?:int|long)?\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*(-?\\d+)\\s*$",
				Pattern.CASE_INSENSITIVE).matcher(init);
		if (!initMatcher.matches()) {
			return null;
		}
		String var = initMatcher.group(1);
		int start = Integer.parseInt(initMatcher.group(2));
		String stop = null;
		Matcher less = Pattern.compile("^\\s*" + Pattern.quote(var) + "\\s*<\\s*(\\d+)\\s*$").matcher(cond);
		Matcher lessEq = Pattern.compile("^\\s*" + Pattern.quote(var) + "\\s*<=\\s*(\\d+)\\s*$").matcher(cond);
		Matcher length = Pattern.compile("^\\s*" + Pattern.quote(var)
				+ "\\s*<\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\.length(?:\\(\\))?\\s*$").matcher(cond);
		if (less.matches()) {
			stop = less.group(1);
		} else if (lessEq.matches()) {
			stop = Integer.toString(Integer.parseInt(lessEq.group(1)) + 1);
		} else if (length.matches()) {
			stop = "len(" + length.group(1) + ")";
		}
		if (stop == null || !isSimpleIncrement(step, var)) {
			return null;
		}
		if (start == 0) {
			return "for " + var + " in range(" + stop + "):";
		}
		return "for " + var + " in range(" + start + ", " + stop + "):";
	}

	public static String caseLabelToken(String name, int target) {
		String t = name != null ? name.trim() : "";
		if (t.isEmpty()) {
			return target == CodeErzeuger.typPython ? "\"\"" : "0";
		}
		if (target == CodeErzeuger.typPython || target == CodeErzeuger.typJavaScript) {
			if (t.chars().allMatch(Character::isDigit)) {
				return t;
			}
			return "\"" + t.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
		}
		return t;
	}

	public static String forcedCaseComment(String name, int target) {
		String t = name != null ? name.trim() : "";
		if (t.isEmpty()) {
			return "";
		}
		if (target == CodeErzeuger.typPython) {
			return "  # " + t;
		}
		if (target == CodeErzeuger.typJavaScript) {
			return " /* " + t + " */";
		}
		return "/* " + t + " */";
	}

	public static String postProcessGeneratedCode(String code, int target) {
		if (target == CodeErzeuger.typJavaScript) {
			return code.replace(".length()", ".length");
		}
		if (target == CodeErzeuger.typPython) {
			String python = code.replaceAll("\\b([A-Za-z_$][A-Za-z0-9_$]*)\\.length(?:\\(\\))?\\b", "len($1)");
			return replaceJavaMathRandomWithPythonRandom(python);
		}
		if (target == CodeErzeuger.typJava) {
			return replaceIdentifierDotLengthWithParensInJava(code);
		}
		return code;
	}

	public static boolean pythonNeedsRandomImport(String code) {
		return code.contains("random.random()");
	}

	public static boolean javaUsesScanner(String code) {
		return code.contains("scanner.");
	}

	public static String stripTrailingSemicolon(String s) {
		String x = s != null ? s.trim() : "";
		while (x.endsWith(";")) {
			x = x.substring(0, x.length() - 1).trim();
		}
		return x;
	}

	public static String formatCStyleStatementLine(String rawLine) {
		String line = rawLine != null ? rawLine.trim() : "";
		if (line.isEmpty() || line.endsWith(";") || line.endsWith("{") || line.equals("}") || line.startsWith("}")) {
			return line;
		}
		String lower = line.toLowerCase();
		if (lower.startsWith("if ") || lower.startsWith("else") || lower.startsWith("for ")
				|| lower.startsWith("while ") || lower.startsWith("do ") || lower.startsWith("switch ")
				|| lower.startsWith("case ") || lower.startsWith("default") || lower.startsWith("//")
				|| lower.startsWith("/*") || lower.startsWith("*") || lower.startsWith("#")) {
			return line;
		}
		return line + ";";
	}

	private static String indentMultiline(String code, int spaces) {
		String[] lines = code.split("\\R", -1);
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			if (i > 0) {
				out.append('\n');
			}
			out.append(indent(lines[i], spaces));
		}
		return out.toString();
	}

	private static String javaScriptForInitializerFromJavaTypedDeclaration(String raw) {
		Matcher m = Pattern.compile(
				"(?i)^(\\s*)(?:integer|int|long|float|double|string|boolean|char|bool)\\s+([A-Za-z_$][A-Za-z0-9_$]*)(?:\\s*=\\s*(.*))?\\s*$")
				.matcher(raw);
		if (!m.matches()) {
			return null;
		}
		String rhs = emptyToNull(m.group(3));
		return rhs == null ? m.group(1) + "let " + m.group(2) : m.group(1) + "let " + m.group(2) + " = " + rhs;
	}

	private static String[] cleanedParts(String[] lines) {
		if (lines == null) {
			return new String[0];
		}
		return java.util.Arrays.stream(lines)
				.map(CodeGenRules::stripTrailingSemicolon)
				.filter(s -> !s.isEmpty())
				.toArray(String[]::new);
	}

	private static boolean isSimpleIncrement(String step, String var) {
		return step.matches("\\s*" + Pattern.quote(var) + "\\s*\\+\\+\\s*")
				|| step.matches("\\s*" + Pattern.quote(var) + "\\s*\\+=\\s*1\\s*");
	}

	/** Wie Swift {@code inferIntegerVariablesForJavaScript}: für {@code Math.floor}-Division in JS. */
	public static Set<String> inferIntegerVariablesForJavaScript(StruktogrammElementListe root) {
		Set<String> out = new HashSet<>();
		if (root != null) {
			walkStruktogramForJsIntInference(root, out);
		}
		return out;
	}

	private static void walkStruktogramForJsIntInference(StruktogrammElementListe list, Set<String> out) {
		for (StruktogrammElement e : list) {
			for (String line : e.text) {
				addFromStatementLineForJsInt(line, out);
			}
			if (e instanceof Schleife schleife) {
				walkStruktogramForJsIntInference(schleife.gibListe(), out);
			} else if (e instanceof Fallauswahl fall) {
				for (StruktogrammElementListe sub : fall.listen) {
					walkStruktogramForJsIntInference(sub, out);
				}
			}
		}
	}

	private static void addFromStatementLineForJsInt(String raw, Set<String> set) {
		String t = raw != null ? raw.trim() : "";
		if (t.isEmpty()) {
			return;
		}
		String lower = t.toLowerCase();
		if (lower.startsWith("input:")) {
			InputLineParse p = parseInputLine(t);
			if (p != null && isDiagramIntegerType(p.type) && !p.isIndexedTarget()) {
				set.add(p.name);
			}
			return;
		}
		Matcher constM = CONST_TYPED.matcher(stripTrailingSemicolon(t));
		if (constM.matches() && isDiagramIntegerType(constM.group(1))) {
			set.add(constM.group(2));
			return;
		}
		Matcher decl = JS_INT_DECL_ASSIGN.matcher(t);
		if (decl.find()) {
			set.add(decl.group(2).trim());
		}
	}

	private static boolean isDiagramIntegerType(String rawType) {
		String x = rawType != null ? rawType.trim().toLowerCase() : "";
		return x.equals("int") || x.equals("integer") || x.equals("long") || x.equals("short") || x.equals("byte");
	}

	/**
	 * Wie Swift {@code javaScriptIntegerDivisionIfNeeded}: einfache {@code x = x / y} bzw. {@code x /= y} für
	 * als Ganzzahl erkannte Variablen → {@code Math.floor}.
	 */
	static String javaScriptIntegerDivisionIfNeeded(String raw, Set<String> intVariables) {
		if (intVariables == null || intVariables.isEmpty()) {
			return null;
		}
		String t = stripTrailingSemicolon(raw).trim();
		if (t.isEmpty()) {
			return null;
		}
		Matcher assign = JS_INT_ASSIGN_DIV.matcher(t);
		if (assign.matches()) {
			String lhs = assign.group(1);
			String a = assign.group(2);
			String b = assign.group(3);
			if (intVariables.contains(lhs) && lhs.equals(a)) {
				return lhs + " = Math.floor(" + a + " / " + b + ")";
			}
			return null;
		}
		Matcher divEq = JS_INT_DIV_EQ.matcher(t);
		if (divEq.matches()) {
			String lhs = divEq.group(1);
			String b = divEq.group(2);
			if (intVariables.contains(lhs)) {
				return lhs + " = Math.floor(" + lhs + " / " + b + ")";
			}
		}
		return null;
	}

	/**
	 * Diagrammzeilen ohne {@code =}: z. B. {@code zahl / 2} oder Collatz {@code zahl * 3 + 1} — werden zu gültigen
	 * Zuweisungen (sonst kein Effekt im Browser/Interpreter).
	 */
	private static String javaScriptShorthandSelfAssignIfNeeded(String raw, Set<String> intVariables) {
		String t = stripTrailingSemicolon(raw).trim();
		if (t.isEmpty()) {
			return null;
		}
		Matcher mDiv = SHORTHAND_SELF_DIV.matcher(t);
		if (mDiv.matches()) {
			String lhs = mDiv.group(1);
			String rhs = mDiv.group(2);
			if (intVariables != null && intVariables.contains(lhs)) {
				return lhs + " = Math.floor(" + lhs + " / " + rhs + ")";
			}
			return lhs + " = " + lhs + " / " + rhs;
		}
		Matcher mMul = SHORTHAND_VAR_MUL_ADD.matcher(t);
		if (mMul.matches()) {
			String v = mMul.group(1);
			return v + " = " + v + " * " + mMul.group(2) + " + " + mMul.group(3);
		}
		Matcher m3 = SHORTHAND_LIT_MUL_VAR_ADD.matcher(t);
		if (m3.matches()) {
			String v = m3.group(2);
			return v + " = " + m3.group(1) + " * " + v + " + " + m3.group(3);
		}
		return null;
	}

	private static String javaShorthandSelfAssignIfNeeded(String raw) {
		String t = stripTrailingSemicolon(raw).trim();
		if (t.isEmpty()) {
			return null;
		}
		Matcher mDiv = SHORTHAND_SELF_DIV.matcher(t);
		if (mDiv.matches()) {
			String lhs = mDiv.group(1);
			return lhs + " = " + lhs + " / " + mDiv.group(2);
		}
		Matcher mMul = SHORTHAND_VAR_MUL_ADD.matcher(t);
		if (mMul.matches()) {
			String v = mMul.group(1);
			return v + " = " + v + " * " + mMul.group(2) + " + " + mMul.group(3);
		}
		Matcher m3 = SHORTHAND_LIT_MUL_VAR_ADD.matcher(t);
		if (m3.matches()) {
			String v = m3.group(2);
			return v + " = " + m3.group(1) + " * " + v + " + " + m3.group(3);
		}
		return null;
	}

	private static String pythonShorthandSelfAssignIfNeeded(String raw) {
		String t = stripTrailingSemicolon(raw).trim();
		if (t.isEmpty()) {
			return null;
		}
		Matcher mDiv = SHORTHAND_SELF_DIV.matcher(t);
		if (mDiv.matches()) {
			String lhs = mDiv.group(1);
			return lhs + " = " + lhs + " // " + mDiv.group(2);
		}
		Matcher mMul = SHORTHAND_VAR_MUL_ADD.matcher(t);
		if (mMul.matches()) {
			String v = mMul.group(1);
			return v + " = " + v + " * " + mMul.group(2) + " + " + mMul.group(3);
		}
		Matcher m3 = SHORTHAND_LIT_MUL_VAR_ADD.matcher(t);
		if (m3.matches()) {
			String v = m3.group(2);
			return v + " = " + m3.group(1) + " * " + v + " + " + m3.group(3);
		}
		return null;
	}

	private static String normalizeJavaScriptMathRandomCall(String s) {
		Matcher m = MATH_RANDOM_NO_PARENS.matcher(s);
		StringBuffer out = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(out, "Math.random()");
		}
		m.appendTail(out);
		return out.toString();
	}

	private static String replaceIdentifierDotLengthWithParensInJava(String code) {
		Set<String> arrayNames = javaArrayDeclarationNames(code);
		Matcher m = JAVA_LENGTH.matcher(code);
		StringBuffer out = new StringBuffer();
		while (m.find()) {
			String id = m.group(1);
			String lower = id.toLowerCase();
			String replacement = arrayNames.contains(lower) ? m.group(0) : id + ".length()";
			m.appendReplacement(out, Matcher.quoteReplacement(replacement));
		}
		m.appendTail(out);
		return out.toString();
	}

	private static Set<String> javaArrayDeclarationNames(String code) {
		Set<String> names = new HashSet<>();
		collectArrayNames(JAVA_ARRAY_DECL_1, code, names);
		collectArrayNames(JAVA_ARRAY_DECL_2, code, names);
		return names;
	}

	private static void collectArrayNames(Pattern pattern, String code, Set<String> names) {
		Matcher m = pattern.matcher(code);
		while (m.find()) {
			names.add(m.group(1).toLowerCase());
		}
	}

	private static String javaScriptRhsFromJavaRhs(String rhs, boolean array) {
		String t = rhs.trim();
		if (array) {
			Matcher newArray = JAVA_NEW_ARRAY.matcher(t);
			if (newArray.matches()) {
				return "new Array(" + newArray.group(1).trim() + ")";
			}
			return arrayLiteralFromJavaInitializer(t);
		}
		Matcher cast = JAVA_INT_CAST.matcher(t);
		if (cast.matches()) {
			return "Math.floor(" + javaScriptRhsFromJavaRhs(cast.group(1), false) + ")";
		}
		return normalizeJavaScriptMathRandomCall(t);
	}

	private static String pythonRhsFromJavaRhs(String rhs, boolean array) {
		String t = rhs.trim();
		if (array) {
			Matcher newArray = JAVA_NEW_ARRAY.matcher(t);
			if (newArray.matches()) {
				return "[None] * " + newArray.group(1).trim();
			}
			return arrayLiteralFromJavaInitializer(t);
		}
		return pythonRhsFromJavaRhs(t);
	}

	private static String pythonRhsFromJavaRhs(String rhs) {
		String t = rhs.trim();
		if (t.equals("null")) {
			return "None";
		}
		Matcher cast = JAVA_INT_CAST.matcher(t);
		if (cast.matches()) {
			return "int(" + pythonRhsFromJavaRhs(cast.group(1)) + ")";
		}
		return replaceJavaMathRandomWithPythonRandom(t);
	}

	private static String replaceJavaMathRandomWithPythonRandom(String s) {
		return s.replaceAll("(?i)\\bMath\\.random\\s*\\(\\s*\\)", "random.random()");
	}

	private static String arrayLiteralFromJavaInitializer(String rhs) {
		String t = rhs.trim();
		if (t.startsWith("{") && t.endsWith("}")) {
			return "[" + t.substring(1, t.length() - 1) + "]";
		}
		return t;
	}

	private static boolean isCStyleTarget(int target) {
		return target == CodeErzeuger.typJava || target == CodeErzeuger.typJavaScript;
	}

	private static String normalizeType(String raw) {
		String t = raw.trim().toLowerCase();
		return switch (t) {
		case "integer", "int" -> "int";
		case "string" -> "String";
		case "bool" -> "boolean";
		default -> t;
		};
	}

	private static boolean isTypeKeyword(String raw) {
		String t = raw.toLowerCase();
		return Set.of("int", "integer", "long", "float", "double", "string", "boolean", "char", "bool", "void")
				.contains(t);
	}

	private static String emptyToNull(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return null;
		}
		return raw.trim();
	}

	private static String escapeJava(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	private static String escapeJavaScript(String s) {
		return escapeJava(s);
	}

	private static String escapePython(String s) {
		return escapeJava(s);
	}

	private record InputLineParse(String type, String name, String customPrompt) {
		boolean isIndexedTarget() {
			return name.contains("[") && name.contains("]");
		}
	}
}
