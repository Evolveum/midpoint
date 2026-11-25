/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * Format of a password dictionary file (one word per line, UTF-8, "#" starts a comment line)
 * and the normalization used when words are compared.
 */
final class PasswordDictionaryFormat {

    private PasswordDictionaryFormat() {
    }

    /**
     * Normalizes a value before it is compared with dictionary words. The same normalization is applied
     * to the checked value and to the words of the dictionaries. The value itself is never changed.
     */
    static @NotNull String normalize(@NotNull String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    /** Reads the words of a dictionary. The words are already normalized. */
    static @NotNull Set<String> read(@NotNull InputStream stream) throws IOException {
        Set<String> words = new HashSet<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            String stripped = line.strip();
            if (stripped.isEmpty() || stripped.startsWith("#")) {
                continue;
            }
            String word = normalize(stripped);
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }
}
