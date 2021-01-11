package com.evolveum.axiom.lang.antlr;

public class AxiomAntlrLiterals {

    public static String convertSingleQuote(String text) {
        int stop = text.length();
        return text.substring(1, stop - 1);
    }

    public static String convertDoubleQuote(String text) {
        int stop = text.length();
        return text.substring(1, stop - 1);
    }

    public static String convertMultiline(String text) {
        return text.replace("\"\"\"", "");
    }
}
