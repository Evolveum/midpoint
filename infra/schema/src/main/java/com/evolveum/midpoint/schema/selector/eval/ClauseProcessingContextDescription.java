/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import org.jetbrains.annotations.NotNull;

/** Description of the processing context, mainly for tracing and error reporting. */
public interface ClauseProcessingContextDescription {

    /** Short identifier, mainly for trace records. */
    @NotNull String getId();

    /** Human-readable explanation of the context. */
    @NotNull String getText();

    /**
     * Creates a description for embedded (child) processing context.
     *
     * It is assumed that its ID and text will be somehow derived from the original one plus a "delta", provided here.
     */
    @NotNull ClauseProcessingContextDescription child(@NotNull String idDelta, @NotNull String textDelta);

    static ClauseProcessingContextDescription defaultOne() {
        return new Default();
    }

    static ClauseProcessingContextDescription defaultOne(@NotNull String id, @NotNull String text) {
        return new Default(id, text);
    }

    class Default implements ClauseProcessingContextDescription {

        @NotNull private final String id;
        @NotNull private final String text;

        public Default() {
            this("", "");
        }

        public Default(@NotNull String id, @NotNull String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        public @NotNull String getId() {
            return id;
        }

        @Override
        public @NotNull String getText() {
            return text;
        }

        @Override
        public @NotNull ClauseProcessingContextDescription child(@NotNull String idDelta, @NotNull String textDelta) {
            if ("".equals(id) && "".equals(text)) {
                return new Default(idDelta, textDelta);
            } else {
                return new Default(
                        id + "." + idDelta,
                        textDelta + " of " + text);
            }
        }
    }
}
