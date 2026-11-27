/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;

/** Dictionary with a fixed list of words that applies for any limitations. */
class FixedPasswordDictionary implements PasswordDictionary {

    private final Set<String> words;

    FixedPasswordDictionary(String... words) {
        this.words = Set.of(words);
    }

    @Override
    public boolean contains(@NotNull String word, @NotNull LimitationsType limitations) {
        return words.contains(word);
    }

    @Override
    public @NotNull Stream<String> words(@NotNull LimitationsType limitations) {
        return words.stream();
    }
}
