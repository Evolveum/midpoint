package com.evolveum.axiom.lang.spi;

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
        return sourceName + "["+ line + ":" + character + "]";
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



}
