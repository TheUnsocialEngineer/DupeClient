package com.dupeclient.client.module.utility;

import net.minecraft.util.StringHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses ChatGames-style puzzles from plain chat text. */
final class ChatGamesSolver {
    private static final Pattern MATH_CANDIDATE = Pattern.compile(
            "\\d+(?:\\.\\d+)?(?:\\s*[+\\-*/]\\s*(?:\\d+(?:\\.\\d+)?|\\([\\d\\s+\\-*/.()]+\\))\\s*)*");
    private static final Pattern SAFE_EXPR = Pattern.compile("^[\\d\\s+\\-*/().]+$");
    private static final Pattern WRITE_OUT_WORD = Pattern.compile(
            "write\\s+out\\s+the\\s+word\\s*:?\\s*['\"]([^'\"]+)['\"]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EQUATION_PAIR = Pattern.compile("([^=]+?)\\s*=\\s*(-?\\d+(?:\\.\\d+)?)(?:\\s|$|\n)");

    private ChatGamesSolver() {
    }

    static String stripPlain(String raw) {
        return raw == null ? "" : StringHelper.stripTextFormat(raw);
    }

    static boolean isOwnFeedback(String plain) {
        return plain.contains("Answered:") || plain.contains("Sent word:");
    }

    static String extractWriteOutWord(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        Matcher m = WRITE_OUT_WORD.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    static String extractMathExpression(String text) {
        if (text == null || text.length() < 3) {
            return null;
        }
        Matcher m = MATH_CANDIDATE.matcher(text);
        while (m.find()) {
            String candidate = m.group();
            if (!SAFE_EXPR.matcher(candidate).matches()) {
                continue;
            }
            if (!candidate.matches(".*[+\\-*/].*")) {
                continue;
            }
            if (candidate.length() > 120) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    static Double evaluateMath(String expr) {
        if (expr == null || expr.isEmpty()) {
            return null;
        }
        try {
            return new ExprParser(expr.trim()).parse();
        } catch (Exception ignored) {
            return null;
        }
    }

    static Double trySolveForX(String text) {
        if (text == null || text.length() < 10) {
            return null;
        }
        String lower = text.toLowerCase();
        if (!lower.contains("solve for") || !text.contains("=")) {
            return null;
        }
        List<Equation> equations = new ArrayList<>();
        Matcher matcher = EQUATION_PAIR.matcher(text);
        while (matcher.find()) {
            String lhs = matcher.group(1).trim();
            double rhs = Double.parseDouble(matcher.group(2));
            if (lhs.isEmpty()) {
                continue;
            }
            String[] parts = lhs.split("\\s*\\+\\s*");
            List<String> symbols = new ArrayList<>();
            for (String p : parts) {
                String sym = p.trim();
                if (!sym.isEmpty()) {
                    symbols.add(sym);
                }
            }
            if (symbols.isEmpty()) {
                continue;
            }
            equations.add(new Equation(symbols, rhs));
        }
        if (equations.isEmpty()) {
            return null;
        }
        Map<String, Double> known = new HashMap<>();
        boolean hasX = equations.stream().anyMatch(eq -> eq.symbols.stream().anyMatch(ChatGamesSolver::isVariableX));
        if (!hasX) {
            return null;
        }
        for (int round = 0; round < equations.size() + 2; round++) {
            boolean progress = false;
            for (Equation eq : equations) {
                if (eq.symbols.size() == 1) {
                    String sym = eq.symbols.get(0);
                    if (!known.containsKey(sym)) {
                        known.put(sym, eq.rhs);
                        progress = true;
                    }
                    continue;
                }
                Set<String> unique = new HashSet<>(eq.symbols);
                if (unique.size() == 1) {
                    String sym = unique.iterator().next();
                    if (!known.containsKey(sym)) {
                        known.put(sym, eq.rhs / eq.symbols.size());
                        progress = true;
                    }
                    continue;
                }
                if (eq.symbols.stream().noneMatch(ChatGamesSolver::isVariableX)) {
                    continue;
                }
                double sumKnown = 0;
                int xCount = 0;
                boolean allOthersKnown = true;
                for (String s : eq.symbols) {
                    if (isVariableX(s)) {
                        xCount++;
                    } else {
                        Double v = known.get(s);
                        if (v == null) {
                            allOthersKnown = false;
                            break;
                        }
                        sumKnown += v;
                    }
                }
                if (xCount != 1 || !allOthersKnown) {
                    continue;
                }
                double x = eq.rhs - sumKnown;
                if (!Double.isFinite(x)) {
                    continue;
                }
                return x;
            }
            if (!progress) {
                break;
            }
        }
        return null;
    }

    static String formatAnswer(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        String s = String.valueOf(value);
        if (s.contains("E")) {
            return s;
        }
        int i = s.indexOf('.');
        if (i >= 0) {
            int j = s.length() - 1;
            while (j > i && s.charAt(j) == '0') {
                j--;
            }
            if (j == i) {
                return s.substring(0, i);
            }
            return s.substring(0, j + 1);
        }
        return s;
    }

    private static boolean isVariableX(String s) {
        return "x".equalsIgnoreCase(s.trim());
    }

    private static final class Equation {
        final List<String> symbols;
        final double rhs;

        Equation(List<String> symbols, double rhs) {
            this.symbols = symbols;
            this.rhs = rhs;
        }
    }

    private static final class ExprParser {
        private final String s;
        private int i;

        ExprParser(String s) {
            this.s = s;
        }

        double parse() {
            double v = parseExpr();
            if (i < s.length()) {
                throw new IllegalArgumentException("leftover: " + s.substring(i));
            }
            return v;
        }

        private double parseExpr() {
            double left = parseTerm();
            while (i < s.length()) {
                skipSpaces();
                if (i >= s.length()) {
                    break;
                }
                char c = s.charAt(i);
                if (c == '+') {
                    i++;
                    left += parseTerm();
                } else if (c == '-') {
                    i++;
                    left -= parseTerm();
                } else {
                    break;
                }
            }
            return left;
        }

        private double parseTerm() {
            double left = parseFactor();
            while (i < s.length()) {
                skipSpaces();
                if (i >= s.length()) {
                    break;
                }
                char c = s.charAt(i);
                if (c == '*') {
                    i++;
                    left *= parseFactor();
                } else if (c == '/') {
                    i++;
                    double right = parseFactor();
                    if (right == 0) {
                        throw new ArithmeticException("division by zero");
                    }
                    left /= right;
                } else {
                    break;
                }
            }
            return left;
        }

        private double parseFactor() {
            skipSpaces();
            if (i >= s.length()) {
                throw new IllegalArgumentException("unexpected end");
            }
            if (s.charAt(i) == '(') {
                i++;
                double v = parseExpr();
                skipSpaces();
                if (i >= s.length() || s.charAt(i) != ')') {
                    throw new IllegalArgumentException("missing )");
                }
                i++;
                return v;
            }
            return parseNumber();
        }

        private double parseNumber() {
            skipSpaces();
            int start = i;
            if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                i++;
            }
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                i++;
            }
            if (i == start) {
                throw new IllegalArgumentException("expected number at " + start);
            }
            return Double.parseDouble(s.substring(start, i).trim());
        }

        private void skipSpaces() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }
    }
}
