package com.dupeclient.client.module.fuzzer.economy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pay-command amount strings for economy-plugin validation testing.
 */
public final class EconomyFuzzerValues {
    private static final List<String> VALUES = build();

    private EconomyFuzzerValues() {
    }

    public static List<String> all() {
        return VALUES;
    }

    public static int count() {
        return VALUES.size();
    }

    private static List<String> build() {
        List<String> v = new ArrayList<>();

        // Negatives / steal money
        Collections.addAll(v, "-1", "-64", "-1000000");

        // Negative zero – sign-bit tricks
        Collections.addAll(v, "-0", "-0.0", "-0.00", "0.0");

        // Decimals / precision attacks
        Collections.addAll(v, "0.0001", "1.5", "0.1", "0.999999999999");
        v.add("0." + "1".repeat(198));
        v.add("1.0000000000000002");

        // Scientific notation
        Collections.addAll(v,
                "1e6", "1E6", "1e06", "-1e6", "1e-6", "1.5e3",
                "1e308", "1e309",
                "1e+6", "1E+6", "1.0e6", "1.0E-6",
                "1e-308", "5e-324",
                "1E+999", "1E-999", "1E+2147483647");

        // Integer / long boundaries
        v.add(String.valueOf(Integer.MAX_VALUE));
        v.add(String.valueOf(Integer.MIN_VALUE));
        v.add("2147483648");
        v.add(String.valueOf(Long.MAX_VALUE));
        v.add(String.valueOf(Long.MIN_VALUE));
        v.add("9223372036854775808");
        v.add("-9223372036854775809");
        v.add("1000000000000000000000000000000000");

        // Special float values
        Collections.addAll(v,
                "NaN", "Infinity", "-Infinity", "+Infinity",
                "NAN", "INFINITY", "nan", "infinity", "-infinity");

        // Formatting / locale tricks
        Collections.addAll(v,
                "+100", " 100 ", "1,000", "1.000.000", "1_000", "0x10", "010", "1f", "1d", "1L",
                "1,5", "1.000,00",
                "1..0", "1.,0", ".100", "100.",
                "0100", "00064", "007");

        // Currency-symbol prefixes
        Collections.addAll(v,
                "$100", "€100", "£100", "¥100", "₽100",
                "$-100", "-$100");

        // Empty / whitespace
        Collections.addAll(v, "", " ", "\t", "  ", "\r", "\n");

        // Null byte / control-character truncation
        v.add("100\u0000");
        v.add("\u0000100");
        v.add("1\u00000");

        // Carriage-return / newline injection (log injection)
        v.add("100\r\n200");

        addUnicodeLookalikes(v);

        return Collections.unmodifiableList(v);
    }

    /**
     * Confusable Unicode dashes, commas, periods, and digits (homoglyphs) layered on common amount shapes.
     */
    private static void addUnicodeLookalikes(List<String> v) {
        // Minus / dash lookalikes (not ASCII U+002D)
        Collections.addAll(v,
                "\u2212100",      // − MINUS SIGN
                "\u221264",       // −64
                "\u22121000000",  // −1M
                "\uFF0D100",      // － FULLWIDTH HYPHEN-MINUS
                "\u2013100",      // – EN DASH
                "\u2014100",      // — EM DASH
                "\u2012100",      // ‒ FIGURE DASH
                "\u201110",       // ‑ NON-BREAKING HYPHEN
                "\uFE58100",      // ﹘ SMALL EM DASH
                "10\u22120",      // embedded minus
                "-\u2212100");     // double minus (ASCII + mathematical)

        // Comma lookalikes (thousands / decimal separators)
        Collections.addAll(v,
                "1\uFF0C000",     // ， FULLWIDTH COMMA
                "1\uFE50000",     // ﹐ SMALL COMMA
                "1\u060C000",     // ، ARABIC COMMA
                "10\uFF0C50",     // mixed grouping
                "1\uFF0C5",       // EU-style decimal comma with fullwidth comma
                "1\uFF0C000\uFF0C000", // 1，000，000
                "1,000\uFF0C50"); // mixed ASCII + fullwidth comma

        // Period / decimal-point lookalikes
        Collections.addAll(v,
                "1\uFF0E5",       // ． FULLWIDTH FULL STOP
                "1\u00B75",       // · MIDDLE DOT
                "1\u20245",       // ․ ONE DOT LEADER
                "1\uFE525",       // ﹒ SMALL FULL STOP
                "0\uFF0E0001",
                "1\uFF0E000\uFF0E000", // 1．000．000
                "100\uFF0E",      // trailing fullwidth period
                "\uFF0E100",      // leading fullwidth period
                "1\u00B7000\u00B750"); // middle-dot thousands + decimal

        // Fullwidth / mixed digit homoglyphs
        Collections.addAll(v,
                "\uFF11\uFF10\uFF10",           // １００
                "\uFF11\uFF0C\uFF10\uFF10\uFF10", // １，０００
                "\u2212\uFF11\uFF10\uFF10",     // −１００
                "\uFF11.\uFF15",                // １.５
                "\uFF11\uFF0E\uFF15",           // １．５
                "1\uFF10\uFF10\uFF10",          // 1０００
                "\uFF11\uFF10\uFF100",          // １００0
                "100\uFF10");                   // 100０

        // Combined confusables (realistic paste / mobile keyboard input)
        Collections.addAll(v,
                "\u22121\uFF0C000\uFF0E50",     // −1，000．50
                "\uFF0D1\u00B75",               // －1·5
                "1\uFF0C000\uFF0E99",
                "\u22120\uFF0E00",
                "1\uFF0E5e3",                   // unicode decimal + ascii exponent
                "1e\uFF0B6",                    // 1e＋6 fullwidth plus in exponent
                "+1\uFF0C000",
                "\uFF0B100");                   // ＋ FULLWIDTH PLUS SIGN
    }
}
