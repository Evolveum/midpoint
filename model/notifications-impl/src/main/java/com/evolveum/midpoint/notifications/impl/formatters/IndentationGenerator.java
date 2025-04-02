package com.evolveum.midpoint.notifications.impl.formatters;

import org.apache.commons.lang3.Validate;

public final class IndentationGenerator {
    private final String indentation;

    public IndentationGenerator(String indentationPrefix, String indentationCharacter) {
        this.indentation = indentationPrefix + indentationCharacter;
    }

    String indentation(int nestingLevel) {
        Validate.isTrue(nestingLevel >= 0, "Nesting level can not be negative: %d", nestingLevel);

        return this.indentation.repeat(nestingLevel);
    }

}
