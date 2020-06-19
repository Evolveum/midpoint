/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.concepts;

public class SourceLocation {

    private final String sourceName;
    private final int line;
    private final int character;

    private SourceLocation(String sourceName, int line, int character) {
        this.sourceName = sourceName;
        this.line = line;
        this.character = character;
    }

    public static SourceLocation from(String source, int line, int pos) {
        return new SourceLocation(source, line, pos);
    }

    @Override
    public String toString() {
        return sourceName + (line >= 0 ? "["+ line + ":" + character + "]" : "");
    }

    public String getSource() {
        return sourceName;
    }

    public int getLine() {
        return line;
    }

    public int getChar() {
        return character;
    }

    public static SourceLocation runtime() {
        return SourceLocation.from("IN-MEMORY", 0, 0);
    }

}
