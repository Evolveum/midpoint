/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.concepts;

public class SourceLocation {

    private static final SourceLocation UNKNOWN = new SourceLocation("unknown", 0, 0) {

        @Override
        public SourceLocation offset(int offsetLine, int character) {
            // Does not make sense to calculate offset
            return this;
        }
    };

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

    public static SourceLocation from(String source) {
        return from(source, 0, 0);
    }

    public static SourceLocation runtime() {
        return SourceLocation.from("IN-MEMORY", 0, 0);
    }

    public static SourceLocation unknown() {
        return UNKNOWN;
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

    public SourceLocation offset(int offsetLine, int character) {
        if(offsetLine == 0) {
            return from(sourceName, this.line, this.character + character);
        }
        return from(sourceName, this.line + offsetLine, character);
    }

    @Override
    public String toString() {
        return sourceName + (line >= 0 ? "["+ line + ":" + character + "]" : "");
    }

    public interface Aware {
        SourceLocation sourceLocation();
    }

}
