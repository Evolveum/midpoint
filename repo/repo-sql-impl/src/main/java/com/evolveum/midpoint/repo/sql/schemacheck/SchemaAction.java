/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.schemacheck;

import org.jetbrains.annotations.NotNull;

/**
 * An action to be executed against the database schema.
 */
abstract class SchemaAction {

    static class None extends SchemaAction {
        // nothing to do
        @Override
        public String toString() {
            return "None{}";
        }
    }

    static class Warn extends SchemaAction {
        @NotNull final String message;              // what to show

        Warn(@NotNull String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "Warn{message='" + message + '\'' + '}';
        }
    }

    static class Stop extends SchemaAction {
        @NotNull final String message;              // what to show
        final Exception cause;                      // what cause to put into the exception thrown

        Stop(@NotNull String message, Exception cause) {
            this.message = message;
            this.cause = cause;
        }

        @Override
        public String toString() {
            return "Stop{message='" + message + '\'' + ", cause=" + cause + '}';
        }
    }

    static class CreateSchema extends SchemaAction {
        @NotNull final String script;

        CreateSchema(@NotNull String script) {
            this.script = script;
        }

        @Override
        public String toString() {
            return "CreateSchema{script='" + script + '\'' + '}';
        }
    }

    static class UpgradeSchema extends SchemaAction {
        @NotNull final String script;
        @NotNull final String from;
        @NotNull final String to;

        UpgradeSchema(@NotNull String script, @NotNull String from, @NotNull String to) {
            this.script = script;
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return "UpgradeSchema{script='" + script + '\'' + '}';
        }
    }
}
